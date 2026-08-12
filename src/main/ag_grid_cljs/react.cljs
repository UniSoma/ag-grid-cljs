(ns ag-grid-cljs.react
  "React cell-renderer helpers — walking-skeleton first cut (ticket
  agd-01ky0ed8adbf). Optional namespace: only consumers who require it need
  react/react-dom on their npm classpath; the core stays framework-agnostic.

  Two tiers live here (ADR 0024). `react-renderer` mounts a local React root
  per cell — self-contained, but detached from the consumer's tree, so
  providers and error boundaries do not reach cell content. `portal-renderer`
  + `portal-host` is the provider-transparent tier: the CONSUMER mounts the
  host once under their providers, and cells createPortal into AG Grid's cell
  DOM from that host, so context and error boundaries flow natively.

  react-renderer mechanics: createRoot in init, unmount in destroy. Cell
  renders are queued and flushed in ONE flushSync per microtask: content
  lands before paint, not on the call stack — createRoot renders are async by
  default and would flash empty cells, while a flushSync per cell on the
  caller's stack is a React DEV error whenever that stack is inside a React
  commit (e.g. refreshCells from a useEffect) and defeats batching N times
  over (spike agd-01kzr7wehb0d).

  unmount is deferred one microtask: a synchronous unmount inside a React
  commit (the typical React host destroys grids from a useEffect cleanup)
  trips React's DEV synchronous-unmount error once per live cell, and React
  defers the actual deletion there anyway. Consequently a cell component's
  effect cleanups run after the grid is destroyed, for every caller — do not
  touch the grid api from a cleanup. (The portal tier has none of this
  per-cell root lifecycle: cells are children of the host's tree, and the
  host re-commits on the same one-flushSync-per-microtask queue.)"
  (:require [ag-grid-cljs.impl.convert :as convert]
            [ag-grid-cljs.impl.warn :as warn]
            ;; required for its :renderer construct method, the class builder
            [ag-grid-cljs.render]
            ["react" :as react]
            ["react-dom" :refer [createPortal flushSync]]
            ["react-dom/client" :refer [createRoot]]))

;; Pending cell-render thunks for the current microtask, nil when none is
;; scheduled. Module-level on purpose: one flush covers every cell of every
;; grid on the page, which is what collapses N flushSync calls into one.
(defonce ^:private render-queue (volatile! nil))

(defn- schedule-render!
  "Queue a cell-render thunk; the first thunk of a tick schedules ONE
  flushSync-wrapped drain at end of microtask — after any enclosing React
  commit's stack unwinds, before paint."
  [thunk]
  (if-some [q @render-queue]
    (.push q thunk)
    (do (vreset! render-queue #js [thunk])
        (js/queueMicrotask
         (fn []
           (let [q @render-queue]
             (vreset! render-queue nil)
             (flushSync (fn [] (.forEach q (fn [t] (t)))))))))))

;; An uncaught render error in a cell root unmounts the root, so the observable
;; is always an empty cell — the warning has no false-positive case. Once per
;; process per distinct error message (ADR 0022): the same missing provider
;; throws in every visible cell of the column, and one diagnosis covers them.
;; The provider-shaped hint fires on the "Provider was not found"-class messages
;; design systems throw from a detached per-cell root.
(defn- on-uncaught-render-error [error _error-info]
  (let [msg (or (some-> error .-message) (str error))]
    (warn/warn-once!
     ::cell-render-error msg
     "react-renderer cell threw during render; React unmounted the cell's "
     "root, so the cell paints empty. Error: " msg
     (when (re-find #"(?i)provider.*not.*found" msg)
       (str "\nThis looks like a missing context provider: per-cell React "
            "roots are detached from your app tree, so its providers do not "
            "reach cell content. Wrap the render fn's output in the provider "
            "it needs — see framework-composition.md, \"Design-system "
            "components in cells\".")))))

(defn- react-lifecycle [render-fn]
  {:init    (fn [state params]
              (let [el   (js/document.createElement "span")
                    ;; dev builds route uncaught cell render errors to the
                    ;; warning above (replacing React's default logging for
                    ;; these roots); production passes no options, keeping
                    ;; React's default onUncaughtError (reportError)
                    root (if ^boolean goog.DEBUG
                           (createRoot el #js {:onUncaughtError on-uncaught-render-error})
                           (createRoot el))]
                (reset! state {:el el :root root})
                ;; the destroyed guard covers a destroy landing between queue
                ;; and drain — skip rendering into a root about to unmount
                (schedule-render! #(when-not (:destroyed @state)
                                     (.render root (render-fn params))))))
   :get-gui (fn [state] (:el @state))
   :refresh (fn [state params]
              (let [^js root (:root @state)]
                (schedule-render! #(when-not (:destroyed @state)
                                     (.render root (render-fn params)))))
              true)
   ;; deferred so a destroy inside a React commit doesn't warn (ns docstring);
   ;; double-unmount is a no-op, so a late microtask racing a re-destroy is safe
   :destroy (fn [state]
              (when-let [^js root (:root @state)]
                (swap! state assoc :destroyed true)
                (js/queueMicrotask #(.unmount root))))})

;; This namespace owns its own construction tag, so `convert` never requires
;; `react` — that is what keeps react-dom off the classpath of consumers who
;; never require this namespace (ADR 0021 §4). The render fn is the payload for
;; the same reason it is in `dom-renderer`: the lifecycle map is four fresh
;; closures per call. Construction then goes through the `:renderer` method
;; rather than a fn exported from `render`, keeping one owner for the class.
(defmethod convert/construct :react-renderer [_ render-fn]
  (convert/construct :renderer (react-lifecycle render-fn)))

(defn react-renderer
  "(fn [params-bean] react-element) -> cellRenderer class, deferred to the
  conversion boundary (ADR 0021 §4).
  refresh re-renders into the same root (returns true), so React component
  local state survives value refreshes."
  [render-fn]
  (convert/deferred :react-renderer render-fn))

;; --- portal tier: consumer-mounted host, createPortal per cell (ADR 0024) ----

;; Host discovery is a module-level registry, the render queue's precedent:
;; the host registers itself on mount and portal cells look it up, so no live
;; object ref ever enters the options map and portal-renderer stays a deferred,
;; rebuild-stable value (ADR 0021). :hosts is a vector — the OLDEST mounted
;; host is active, a second concurrent mount is a consumer error (dev warning
;; below), and a surviving host takes over if the active one unmounts.
;; :cells maps cell id -> {:el :render-fn :params}; registrations made while
;; no host is mounted simply wait here — there is deliberately no per-cell-root
;; fallback, which would be a silent architecture change per cell.
(defonce ^:private portal-state (atom {:hosts [] :cells {}}))
(defonce ^:private next-cell-id (volatile! 0))
(defonce ^:private portal-flush-pending? (volatile! false))
(defonce ^:private host-check-pending? (volatile! false))
;; hostless-episode latch (ADR 0022: cells-waiting-with-no-host is a
;; relationship between live things, so the caller owns the period state):
;; set when the episode warns, cleared when a host mounts, so churn during one
;; hostless episode warns once but a NEW episode after a host unmounts re-warns
(defonce ^:private warned-hostless? (volatile! false))

(defn- active-host [] (first (:hosts @portal-state)))

(defn- flush-portals!
  "Re-commit the active host against the current cell registry, on the same
  one-flushSync-per-microtask queue as the per-cell tier: host commits land
  before paint, after any enclosing React commit's stack unwinds."
  []
  (when-not @portal-flush-pending?
    (vreset! portal-flush-pending? true)
    (schedule-render!
     (fn []
       (vreset! portal-flush-pending? false)
       (when-some [{:keys [rerender!]} (active-host)]
         (rerender!))))))

;; One macrotask is enough for any mount order within a tick (a consumer whose
;; effects create the grid before the host commits is fine — both land before
;; the check runs); cells still waiting after that means no host is coming.
(defn- warn-if-still-hostless! []
  (when ^boolean goog.DEBUG
    (when-not @host-check-pending?
      (vreset! host-check-pending? true)
      (js/setTimeout
       (fn []
         (vreset! host-check-pending? false)
         (let [{:keys [hosts cells]} @portal-state]
           (when (and (empty? hosts) (seq cells) (not @warned-hostless?))
             (vreset! warned-hostless? true)
             (warn/warn!
              "portal cells waiting — mount ag-grid-cljs.react/portal-host once "
              "under your app's providers. " (count cells) " portal-renderer "
              "cell(s) are registered with no host on the page; they paint empty "
              "until a host mounts and never fall back to a detached per-cell root."))))
       0))))

(defn- register-cell! [id cell]
  (swap! portal-state assoc-in [:cells id] cell)
  (if (active-host)
    (flush-portals!)
    (warn-if-still-hostless!)))

(defn- refresh-cell! [id params]
  (swap! portal-state update :cells
         (fn [cells]
           (if (contains? cells id)
             (assoc-in cells [id :params] params)
             cells)))
  (flush-portals!))

(defn- unregister-cell! [id]
  (swap! portal-state update :cells dissoc id)
  (flush-portals!))

(defn- register-host! [entry]
  (let [{:keys [hosts cells]} (swap! portal-state update :hosts conj entry)]
    (vreset! warned-hostless? false)
    (when (> (count hosts) 1)
      (warn/warn-once!
       ::second-portal-host nil
       "a second portal-host mounted; one host serves every portal cell on the "
       "page, so cells keep rendering through the first and the extra host "
       "renders nothing (it takes over only if the first unmounts)."))
    (when (seq cells)
      (flush-portals!))))

(defn- unregister-host! [k]
  (let [{:keys [hosts cells]} (swap! portal-state update :hosts
                                     (fn [hs]
                                       (into [] (remove #(identical? k (:host-key %))) hs)))]
    ;; a surviving host takes over the live cells
    (when (and (seq hosts) (seq cells))
      (flush-portals!))))

(defn portal-host
  "React function component; render it ONCE, anywhere under your app's
  providers. Every portal-renderer cell on the page portals its content out
  of this host into AG Grid's cell DOM, so providers (theme, locale, stores)
  AND error boundaries reach cell content natively — the two things a
  detached per-cell root can never inherit. Renders no DOM of its own where
  it sits; a second concurrently mounted host is inert (dev warning)."
  [_props]
  (let [[_ set-version] (react/useState 0)
        key-ref         (react/useRef nil)]
    ;; lazy ref init: a stable per-instance identity, minted on first render
    (when (nil? (.-current key-ref))
      (set! (.-current key-ref) #js {}))
    (react/useEffect
     (fn []
       (let [k (.-current key-ref)]
         (register-host! {:host-key k :rerender! #(set-version (fn [v] (inc v)))})
         (fn [] (unregister-host! k))))
     #js [])
    (let [{:keys [hosts cells]} @portal-state]
      ;; only the active host renders the portals — a duplicate host rendering
      ;; the same registry would double every cell's content
      (when (identical? (.-current key-ref) (:host-key (first hosts)))
        (into-array
         (map (fn [[id {:keys [el render-fn params]}]]
                (createPortal (render-fn params) el (str id)))
              cells))))))

;; No createRoot, no unmount choreography, no destroyed guard: cells are plain
;; children of the host's tree, so a registry change between queue and drain is
;; just the state the host renders next.
(defn- portal-lifecycle [render-fn]
  {:init    (fn [state params]
              (let [el (js/document.createElement "span")
                    id (vswap! next-cell-id inc)]
                (reset! state {:el el :id id})
                (register-cell! id {:el el :render-fn render-fn :params params})))
   :get-gui (fn [state] (:el @state))
   :refresh (fn [state params]
              (refresh-cell! (:id @state) params)
              true)
   :destroy (fn [state]
              (unregister-cell! (:id @state)))})

;; Same ownership shape as :react-renderer above: this namespace owns the tag,
;; the render fn is the payload (fresh lifecycle closures per call), and
;; construction reaches the :renderer class builder (ADR 0021 §4).
(defmethod convert/construct :portal-renderer [_ render-fn]
  (convert/construct :renderer (portal-lifecycle render-fn)))

(defn portal-renderer
  "(fn [params-bean] react-element) -> cellRenderer class, deferred to the
  conversion boundary (ADR 0021 §4). Cell content renders through the
  consumer-mounted portal-host (createPortal into AG Grid's cell DOM), inside
  the consumer's own React tree: providers and error boundaries flow with
  zero bridging. Requires a mounted portal-host — cells wait (and paint
  empty, with a dev warning after a macrotask) until one mounts; there is no
  per-cell-root fallback. refresh updates the same portal in place (returns
  true), so React component local state survives value refreshes."
  [render-fn]
  (convert/deferred :portal-renderer render-fn))

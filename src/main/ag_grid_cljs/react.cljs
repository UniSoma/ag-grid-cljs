(ns ag-grid-cljs.react
  "React cell-renderer helper — walking-skeleton first cut (ticket
  agd-01ky0ed8adbf). Optional namespace: only consumers who require it need
  react/react-dom on their npm classpath; the core stays framework-agnostic.

  Mounts a local React root per cell (createRoot in init, unmount in
  destroy). Cell renders are queued and flushed in ONE flushSync per
  microtask: content lands before paint, not on the call stack — createRoot
  renders are async by default and would flash empty cells, while a flushSync
  per cell on the caller's stack is a React DEV error whenever that stack is
  inside a React commit (e.g. refreshCells from a useEffect) and defeats
  batching N times over (spike agd-01kzr7wehb0d).

  unmount is deferred one microtask: a synchronous unmount inside a React
  commit (the typical React host destroys grids from a useEffect cleanup)
  trips React's DEV synchronous-unmount error once per live cell, and React
  defers the actual deletion there anyway. Consequently a cell component's
  effect cleanups run after the grid is destroyed, for every caller — do not
  touch the grid api from a cleanup."
  (:require [ag-grid-cljs.impl.convert :as convert]
            [ag-grid-cljs.impl.warn :as warn]
            ;; required for its :renderer construct method, the class builder
            [ag-grid-cljs.render]
            ["react-dom" :refer [flushSync]]
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
              (let [root (:root @state)]
                (schedule-render! #(when-not (:destroyed @state)
                                     (.render root (render-fn params)))))
              true)
   ;; deferred so a destroy inside a React commit doesn't warn (ns docstring);
   ;; double-unmount is a no-op, so a late microtask racing a re-destroy is safe
   :destroy (fn [state]
              (when-let [root (:root @state)]
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

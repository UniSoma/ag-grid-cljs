(ns ag-grid-cljs.browser.react-portal-test
  "Browser suite (ADR 0015): the portal-host spike harness (ticket
  agd-01kzva6hgxq5, ADR 0024). The kill rule's three correctness gates:

  1. create-grid!, refreshCells {:force true}, and set-rows! driven from
     useEffect bodies with portal cells: zero DEV console errors, and no
     empty-cell-paint frame beyond AG Grid's own async cell build (ADR 0023
     measured that baseline at {create 1, set-rows 1} for every tier — the
     cell div exists a frame before the renderer's gui attaches; the refresh
     leg must be exactly zero).

  2. Provider-based cell content (raw createContext — no design-system dep)
     renders under the consumer's own provider AND error boundary with zero
     consumer-side bridging.

  3. Missing host: cells queue (paint empty, dev-warn after a macrotask) and
     drain when a host mounts — never a per-cell-root fallback. A second
     concurrently mounted host dev-warns and renders nothing, taking over only
     when the first unmounts."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.react :as agr]
            [ag-grid-cljs.impl.warn :as warn]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [clojure.string :as str]
            [cljs.test :refer [deftest is testing async]]
            ["react" :as react]
            ["react-dom/client" :refer [createRoot]]))

;; --- shared fixtures ---------------------------------------------------------

(def ^:private ctx (react/createContext "no-provider"))

(defn- ctx-probe
  "Function component: renders useContext's value into a span classed by
  props.probe, so a test reads which value actually reached the cell."
  [props]
  (react/createElement "span" #js {:className (unchecked-get props "probe")}
                       (react/useContext ctx)))

(def ^:private error-boundary
  ;; React error boundaries are class components only; this is the minimal one:
  ;; a caught descendant throw renders a .eb-fallback div carrying the message.
  (let [ctor (fn [props]
               (this-as ^js t
                 (.call react/Component t props)
                 (set! (.-state t) #js {:msg nil})
                 nil))]
    (set! (.-prototype ctor) (js/Object.create (.-prototype react/Component)))
    (set! (.. ctor -prototype -constructor) ctor)
    (set! (.-getDerivedStateFromError ctor)
          (fn [e] #js {:msg (or (some-> e .-message) (str e))}))
    (set! (.. ctor -prototype -render)
          (fn []
            (this-as ^js t
              (if-some [msg (.. t -state -msg)]
                (react/createElement "div" #js {:className "eb-fallback"} msg)
                (.. t -props -children)))))
    ctor))

(defn- poll-count
  "Promise resolved once `n` elements match `sel` under `root` (nil payload on
  timeout; the caller's assertion reports the shortfall)."
  [root sel n]
  (u/poll-until #(= n (.-length (.querySelectorAll root sel)))))

(defn- simple-grid-host
  "React FC factory: a sized container whose useEffect creates a one-column,
  one-row grid rendering `cell` (cleanup destroys it)."
  [col-id cell]
  (fn [_props]
    (let [ref (react/useRef nil)]
      (react/useEffect
       (fn []
         (let [h (grid/create-grid!
                  (.-current ref)
                  (-> (grid/options)
                      (grid/with-columns
                        [{:header-name col-id :col-id col-id :cell-renderer cell}])
                      (grid/with-row-id :id)
                      (grid/with-row-data #js [#js {:id "1"}])))]
           (fn [] (grid/destroy! h))))
       #js [])
      (react/createElement "div" #js {:ref   ref
                                      :style #js {:width "600px" :height "300px"}}))))

;; --- 1. commit-driven legs: zero DEV errors, no added empty-cell paint -------

(def ^:private n-rows 8)

(defonce ^:private host-state (atom {}))

(defn- badge [props]
  (react/createElement "span" #js {:className "pt-badge"}
                       (str "S:" (unchecked-get props "name"))))

(def ^:private badge-cell
  (agr/portal-renderer (fn [p] (badge #js {:name (-> p :data :name)}))))

(defn- grid-host
  "React FC: useEffect #1 creates the grid (cleanup destroys it); useEffect #2,
  keyed on a tick state, force-refreshes all cells or pushes new rows — a
  synchronous cell-refresh API and a data push, each from inside a commit."
  [_props]
  (let [ref             (react/useRef nil)
        [tick set-tick] (react/useState 0)]
    (swap! host-state assoc :set-tick set-tick)
    (react/useEffect
     (fn []
       (let [h (grid/create-grid!
                (.-current ref)
                (-> (grid/options)
                    (grid/with-columns
                      [{:field :name}
                       {:header-name "R" :col-id "R" :cell-renderer badge-cell}])
                    (grid/with-row-data
                      (into-array (for [i (range n-rows)]
                                    #js {:name (str "row" i)})))))]
         (swap! host-state assoc :handle h)
         (fn [] (grid/destroy! h))))
     #js [])
    (react/useEffect
     (fn []
       (case tick
         1 (.refreshCells (grid/grid-api (:handle @host-state)) #js {:force true})
         2 (grid/set-rows! (:handle @host-state)
                           (into-array (for [i (range n-rows)]
                                         #js {:name (str "row" i "b")})))
         nil)
       js/undefined)
     #js [tick])
    (react/createElement "div" #js {:ref   ref
                                    :style #js {:width "600px" :height "300px"}})))

(defn- start-empty-monitor!
  "Per-animation-frame monitor: counts frames on which a portal-column cell
  (.ag-cell[col-id=R]) exists with no text — ADR 0023's empty-cell-paint
  measurement. Returns {:count atom :stop! fn}."
  [root]
  (let [counter  (atom 0)
        stopped? (atom false)]
    ((fn tick []
       (when-not @stopped?
         (let [cells (array-seq (.querySelectorAll root ".ag-cell[col-id=R]"))]
           (when (and (seq cells)
                      (some #(str/blank? (.-textContent %)) cells))
             (swap! counter inc)))
         (js/requestAnimationFrame tick))))
    {:count counter :stop! #(reset! stopped? true)}))

(deftest commit-driven-legs-zero-errors-zero-added-empty-paint
  (testing "create, refreshCells force, and set-rows! from useEffect: no DEV
            console error, and the empty-paint monitor stays at AG Grid's own
            baseline (create ≤1, refresh 0, set-rows ≤1)"
    (let [el         (u/mount-el)
          errors     (atom [])
          orig-error (.-error js/console)
          _          (set! (.-error js/console)
                           (fn [& args] (swap! errors conj (apply str args))))
          monitor    (start-empty-monitor! el)
          host-root  (createRoot el)
          settle     (fn [] (-> (u/next-frame) (.then u/next-frame)))
          mark       (atom 0)
          deltas     (atom {})
          ;; telemetry, not a gate (ADR 0024): the observed per-leg counts ride
          ;; along in the suite output
          leg-delta  (fn [leg]
                       (let [c @(:count monitor), d (- c @mark)]
                         (reset! mark c)
                         (swap! deltas assoc leg d)
                         d))]
      ;; the portal host mounts FIRST (tree order), the grid host beside it —
      ;; the documented consumer shape
      (.render host-root
               (react/createElement "div" nil
                                    (react/createElement agr/portal-host)
                                    (react/createElement grid-host)))
      (async done
             (-> (poll-count el ".pt-badge" n-rows)
                 (.then (fn [_]
                          (is (= n-rows (.-length (.querySelectorAll el ".pt-badge")))
                              "all portal cells rendered under the React host")
                          (settle)))
                 (.then (fn [_]
                          (is (<= (leg-delta :create) 1)
                              "create leg: no empty-cell frame beyond AG Grid's own async cell build")
                          ;; state update -> commit -> useEffect -> refreshCells
                          ((:set-tick @host-state) 1)
                          (settle)))
                 (.then (fn [_]
                          (is (zero? (leg-delta :refresh))
                              "refresh leg: zero empty-cell frames")
                          ;; state update -> commit -> useEffect -> set-rows!
                          ((:set-tick @host-state) 2)
                          (poll-count el ".pt-badge" n-rows)))
                 (.then (fn [_] (settle)))
                 (.then (fn [_]
                          (is (<= (leg-delta :set-rows) 1)
                              "set-rows leg: no empty-cell frame beyond AG Grid's own")
                          (js/console.log "portal empty-cell frames per leg:"
                                          (pr-str @deltas))
                          (is (= n-rows (->> (.querySelectorAll el ".pt-badge")
                                             array-seq
                                             (filter #(str/ends-with? (.-textContent %) "b"))
                                             count))
                              "set-rows! content landed in every portal cell")
                          ((:stop! monitor))
                          (.unmount host-root)
                          (u/next-frame)))
                 (.then (fn [_]
                          (set! (.-error js/console) orig-error)
                          (is (= [] @errors)
                              (str "zero DEV console errors across all legs, got: "
                                   (pr-str @errors)))
                          (u/detach! el)
                          (done))))))))

;; --- 2. providers and error boundaries flow with zero bridging ---------------

(def ^:private probe-cell
  (agr/portal-renderer
   (fn [_p] (react/createElement ctx-probe #js {:probe "pt-probe"}))))

(def ^:private probe-grid-host (simple-grid-host "P" probe-cell))

(deftest app-provider-reaches-portal-cells-with-zero-bridging
  (testing "a bare portal cell sees the app-level provider value — no wrap, no
            bridge, nothing consumer-side"
    (let [el        (u/mount-el)
          host-root (createRoot el)]
      (.render host-root
               (react/createElement (.-Provider ctx) #js {:value "from-app"}
                                    (react/createElement "div" nil
                                                         (react/createElement agr/portal-host)
                                                         (react/createElement probe-grid-host))))
      (async done
             (-> (u/poll-until #(.querySelector el ".pt-probe"))
                 (.then (fn [_]
                          (is (= "from-app"
                                 (some-> (.querySelector el ".pt-probe") .-textContent))
                              "portal cell content reads the consumer's own provider")
                          (.unmount host-root)
                          (u/next-frame)))
                 (.then (fn [_]
                          (u/detach! el)
                          (done))))))))

(def ^:private boom-msg
  "portal cell boom (browser-suite probe)")

(defn- boom [_props] (throw (js/Error. boom-msg)))

(def ^:private throwing-cell
  (agr/portal-renderer (fn [_p] (react/createElement boom))))

(def ^:private boom-grid-host (simple-grid-host "B" throwing-cell))

(deftest consumer-error-boundary-catches-a-throwing-portal-cell
  (testing "a throwing cell is caught by the consumer's own boundary — the
            thing no per-cell root or value-bridge can deliver"
    (let [el         (u/mount-el)
          ;; React 19 DEV logs even a boundary-CAUGHT error via its default
          ;; onCaughtError; capture it so the driver's unexpected-console-error
          ;; tripwire (test/browser/run.mjs) does not trip on expected behavior
          errors     (atom [])
          orig-error (.-error js/console)
          _          (set! (.-error js/console)
                           (fn [& args] (swap! errors conj (apply str args))))
          host-root  (createRoot el)]
      (.render host-root
               (react/createElement error-boundary nil
                                    (react/createElement "div" nil
                                                         (react/createElement agr/portal-host)
                                                         (react/createElement boom-grid-host))))
      (async done
             (-> (u/poll-until #(.querySelector el ".eb-fallback"))
                 (.then (fn [_]
                          (set! (.-error js/console) orig-error)
                          (is (= boom-msg
                                 (some-> (.querySelector el ".eb-fallback") .-textContent))
                              "the consumer boundary's fallback rendered with the cell's error")
                          (is (every? #(str/includes? % boom-msg) @errors)
                              "the only console errors are React's DEV log of the CAUGHT error")
                          (.unmount host-root)
                          (u/next-frame)))
                 (.then (fn [_]
                          (u/detach! el)
                          (done))))))))

;; --- 3. missing host: queue + dev warning + drain, never a fallback ----------

(def ^:private late-probe-cell
  (agr/portal-renderer
   (fn [_p] (react/createElement ctx-probe #js {:probe "late-probe"}))))

(deftest hostless-cells-warn-then-drain-into-a-late-host
  (testing "cells created with no host paint empty and dev-warn after a
            macrotask; a host mounting later drains them under ITS providers —
            never a detached per-cell fallback"
    (let [el        (u/mount-el)
          warns     (atom [])
          orig-warn (.-warn js/console)
          relevant  #(filter (fn [w] (str/includes? w "portal cells waiting")) %)
          _         (set! (.-warn js/console)
                          (fn [& args] (swap! warns conj (apply str args))))
          h         (grid/create-grid!
                     el
                     (-> (grid/options)
                         (grid/with-columns
                           [{:header-name "L" :col-id "L" :cell-renderer late-probe-cell}])
                         (grid/with-row-id :id)
                         (grid/with-row-data #js [#js {:id "1"}])))
          host-el   (u/mount-el 50)
          host-root (createRoot host-el)]
      (async done
             (-> (u/poll-until #(seq (relevant @warns)))
                 (.then (fn [_]
                          (set! (.-warn js/console) orig-warn)
                          (let [hits (relevant @warns)]
                            (is (= 1 (count hits))
                                "the missing-host warning fired once for the episode")
                            (is (some-> (first hits) (str/includes? "portal-host"))
                                "the warning points at mounting portal-host"))
                          (is (= "" (some-> (u/by-testid el (.cell u/testid "1" "L"))
                                            .-textContent))
                              "the hostless cell painted empty — no per-cell-root fallback")
                          ;; the host arrives late, under its own provider
                          (.render host-root
                                   (react/createElement (.-Provider ctx) #js {:value "late-host"}
                                                        (react/createElement agr/portal-host)))
                          (u/poll-until #(.querySelector el ".late-probe"))))
                 (.then (fn [_]
                          (is (= "late-host"
                                 (some-> (.querySelector el ".late-probe") .-textContent))
                              "queued cells drained through the late host, under its provider")
                          (grid/destroy! h)
                          (.unmount host-root)
                          (u/next-frame)))
                 (.then (fn [_]
                          (u/detach! el)
                          (u/detach! host-el)
                          (done))))))))

;; --- 4. second host: dev warning, inert until the first unmounts -------------

(def ^:private takeover-cell
  (agr/portal-renderer
   (fn [_p] (react/createElement ctx-probe #js {:probe "tk-probe"}))))

(defn- provided-host
  "A portal-host under a provider carrying `value` — which host serves the
  cells is then readable off the probe's text."
  [value]
  (react/createElement (.-Provider ctx) #js {:value value}
                       (react/createElement agr/portal-host)))

(deftest second-host-warns-is-inert-then-takes-over
  (testing "two mounted hosts: one dev warning, content renders exactly once
            through the FIRST host, and the survivor takes over when the first
            unmounts"
    (warn/reset-warnings!)
    (let [el      (u/mount-el)
          el-a    (u/mount-el 50)
          el-b    (u/mount-el 50)
          root-a  (createRoot el-a)
          root-b  (createRoot el-b)
          h       (grid/create-grid!
                   el
                   (-> (grid/options)
                       (grid/with-columns
                         [{:header-name "T" :col-id "T" :cell-renderer takeover-cell}])
                       (grid/with-row-id :id)
                       (grid/with-row-data #js [#js {:id "1"}])))]
      (.render root-a (provided-host "first-host"))
      (async done
             (-> (u/poll-until #(.querySelector el ".tk-probe"))
                 (.then (fn [_]
                          (is (= "first-host"
                                 (some-> (.querySelector el ".tk-probe") .-textContent))
                              "cells render through the first mounted host")
                          ;; the second host mounts while the first is live
                          (.render root-b (provided-host "second-host"))
                          (u/poll-until #(contains? (warn/fired)
                                                    [:ag-grid-cljs.react/second-portal-host nil]))))
                 (.then (fn [_]
                          (is (contains? (warn/fired)
                                         [:ag-grid-cljs.react/second-portal-host nil])
                              "the second-host dev warning fired")
                          (is (= 1 (.-length (.querySelectorAll el ".tk-probe")))
                              "content rendered once — the duplicate host is inert")
                          (is (= "first-host"
                                 (some-> (.querySelector el ".tk-probe") .-textContent))
                              "cells still render through the first host")
                          ;; the first host unmounts; the survivor takes over
                          (.unmount root-a)
                          (u/poll-until #(= "second-host"
                                            (some-> (.querySelector el ".tk-probe")
                                                    .-textContent)))))
                 (.then (fn [_]
                          (is (= "second-host"
                                 (some-> (.querySelector el ".tk-probe") .-textContent))
                              "the surviving host took over the live cells")
                          (is (= 1 (.-length (.querySelectorAll el ".tk-probe")))
                              "still exactly one copy of the content")
                          (grid/destroy! h)
                          (.unmount root-b)
                          (u/next-frame)))
                 (.then (fn [_]
                          (u/detach! el)
                          (u/detach! el-a)
                          (u/detach! el-b)
                          (done))))))))

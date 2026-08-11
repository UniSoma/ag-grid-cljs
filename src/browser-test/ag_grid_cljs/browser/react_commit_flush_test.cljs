(ns ag-grid-cljs.browser.react-commit-flush-test
  "Browser suite (ADR 0015): guards react-renderer's batched microtask flush
  (spike agd-01kzr7wehb0d, ADR 0023). Create, refreshCells, and set-rows!
  are each driven from inside a real React commit (useEffect bodies run under
  CommitContext) and must print NO React DEV \"flushSync was called from
  inside a lifecycle method\" error — one per live cell on the refresh leg
  before the batching. Also pins the flush contract itself: a refreshed
  cell's content is stale on the caller's stack and fresh after one
  microtask (before paint), and autoHeight rows still settle to content
  height under the deferred first render."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.react :as agr]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]
            ["react" :as react]
            ["react-dom/client" :refer [createRoot]]))

(def ^:private n-rows 8)

(defonce ^:private host-state (atom {}))

(defn- badge [props]
  (react/createElement "span" #js {:className "cf-badge"}
                       (str "S:" (unchecked-get props "name"))))

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
                       {:header-name   "R"
                        :cell-renderer (agr/react-renderer
                                        (fn [p] (badge #js {:name (-> p :data :name)})))}])
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

(defn- poll-badges
  "Promise of the live NodeList once all n-rows badges are present (nil on
  timeout)."
  [root]
  (js/Promise.
   (fn [resolve _]
     (let [deadline (+ (js/Date.now) 3000)]
       ((fn tick []
          (let [ns' (.querySelectorAll root ".cf-badge")]
            (if (= n-rows (.-length ns'))
              (resolve ns')
              (if (< (js/Date.now) deadline)
                (js/setTimeout tick 25)
                (resolve nil))))))))))

(deftest commit-driven-legs-are-silent
  (testing "create, refreshCells, and set-rows! from useEffect print no flushSync DEV error"
    (let [el          (u/mount-el)
          errors      (atom [])
          orig-error  (.-error js/console)
          _           (set! (.-error js/console)
                            (fn [& args] (swap! errors conj (apply str args))))
          host-root   (createRoot el)
          flush-count (fn [] (count (filter #(re-find #"flushSync was called" %) @errors)))]
      (.render host-root (react/createElement grid-host))
      (async done
             (-> (poll-badges el)
                 (.then (fn [badges]
                          (is (some? badges) "all renderer cells rendered under the React host")
                          (u/next-frame)))
                 (.then (fn [_] (u/next-frame)))
                 (.then (fn [_]
                          (is (zero? (flush-count)) "create leg: no flushSync-in-commit error")
                          ;; state update -> commit -> useEffect -> refreshCells
                          ((:set-tick @host-state) 1)
                          (u/next-frame)))
                 (.then (fn [_] (u/next-frame)))
                 (.then (fn [_]
                          (is (zero? (flush-count)) "refresh leg: no flushSync-in-commit error")
                          ;; state update -> commit -> useEffect -> set-rows!
                          ((:set-tick @host-state) 2)
                          (u/next-frame)))
                 (.then (fn [_] (u/next-frame)))
                 (.then (fn [_]
                          (is (zero? (flush-count)) "set-rows leg: no flushSync-in-commit error")
                          (is (= n-rows (.-length (.querySelectorAll el ".cf-badge")))
                              "badges survive refresh and set-rows!")
                          (.unmount host-root)
                          (u/next-frame)))
                 (.then (fn [_]
                          (set! (.-error js/console) orig-error)
                          (u/detach! el)
                          (done))))))))

;; --- the flush contract itself ---------------------------------------------

(def ^:private long-text? (atom false))

(defn- measured-badge [props]
  (react/createElement "span"
                       #js {:className "cf-measured"
                            :style     #js {:display "inline-block" :height "60px"}}
                       (str (if @long-text? "LONG-LONG-LONG-LONG-LONG-LONG-" "s")
                            (unchecked-get props "name"))))

(deftest flush-lands-at-end-of-microtask
  (testing "refresh content is stale on the caller's stack, fresh one microtask later; autoHeight settles"
    (reset! long-text? false)
    (let [el  (u/mount-el)
          h   (grid/create-grid!
               el
               (-> (grid/options)
                   (grid/with-columns
                     [{:field :name}
                      {:header-name   "M"
                       :col-id        "M"
                       :auto-height   true
                       :cell-renderer (agr/react-renderer
                                       (fn [p] (measured-badge #js {:name (-> p :data :name)})))}])
                   (grid/with-row-data #js [#js {:name "a"} #js {:name "b"}])))
          api    (grid/grid-api h)
          cell   (fn [] (.querySelector el ".cf-measured"))
          settle (fn [n] (reduce (fn [p _] (.then p (fn [_] (u/next-frame))))
                                 (js/Promise.resolve nil)
                                 (range n)))]
      (async done
             (-> (settle 6)
                 (.then (fn [_]
                          (is (>= (.-offsetHeight (.querySelector el ".ag-row")) 60)
                              "autoHeight row settled to the 60px cell content")
                          (reset! long-text? true)
                          (.refreshCells api #js {:force true})
                          ;; still the old content on the caller's stack —
                          ;; the flush is queued, not run
                          (is (= "sa" (.-textContent (cell)))
                              "refresh has not landed on the caller's stack")
                          (u/await-microtask)))
                 (.then (fn [_]
                          (is (re-find #"LONG-" (.-textContent (cell)))
                              "refresh landed by end of microtask (before paint)")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done)))
                 (.catch (fn [e]
                           (is false (str "flush-contract test threw: " e))
                           (grid/destroy! h)
                           (u/detach! el)
                           (done))))))))

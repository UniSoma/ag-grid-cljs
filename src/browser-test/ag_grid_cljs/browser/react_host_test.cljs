(ns ag-grid-cljs.browser.react-host-test
  "Browser suite (ADR 0015): a grid hosted inside a React component — useEffect
  setup creates it, the cleanup destroys it. Guards the react-renderer
  deferred-unmount contract (agd-01kzr5qwmfyx): destroying a grid with live
  react-renderer cells from inside a React commit must print no React DEV
  synchronous-unmount error. Also probes (log line, not an assertion) whether
  :init's flushSync warns when the grid is created from inside a commit."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.react :as agr]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]
            ["react" :as react]
            ["react-dom/client" :refer [createRoot]]))

(defn- badge [props]
  (react/createElement "span" #js {:className "host-badge"}
                       (str "H:" (unchecked-get props "name"))))

(defn- grid-host
  "React FC: a sized container div whose useEffect creates the grid and whose
  cleanup destroys it — the typical React-host embedding."
  [_props]
  (let [ref (react/useRef nil)]
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
                    (grid/with-row-data #js [#js {:name "Ada"} #js {:name "Alan"}])))]
         (fn [] (grid/destroy! h))))
     #js [])
    (react/createElement "div" #js {:ref   ref
                                    :style #js {:width "600px" :height "300px"}})))

(defn- poll-selector
  "Promise of the first element matching `sel` under `root`, or nil after the
  deadline — React commits and AG Grid's cell rendering are both async."
  [root sel]
  (js/Promise.
   (fn [resolve _]
     (let [deadline (+ (js/Date.now) 3000)]
       ((fn tick []
          (if-some [n (.querySelector root sel)]
            (resolve n)
            (if (< (js/Date.now) deadline)
              (js/setTimeout tick 25)
              (resolve nil)))))))))

(deftest destroy-from-commit-is-silent
  (testing "destroy! from a useEffect cleanup prints no React DEV errors"
    (let [el         (u/mount-el)
          errors     (atom [])
          orig-error (.-error js/console)
          _          (set! (.-error js/console)
                           (fn [& args] (swap! errors conj (apply str args))))
          host-root  (createRoot el)]
      (.render host-root (react/createElement grid-host))
      (async done
             (-> (poll-selector el ".host-badge")
                 (.then (fn [badge]
                          (is (some? badge)
                              "the react-renderer cell rendered under the React host")
                          ;; runs the effect cleanup -> destroy! inside React's commit
                          (.unmount host-root)
                          (u/next-frame)))
                 ;; a second frame lets passive-effect flushes and the deferred
                 ;; cell-root unmount microtasks settle before asserting
                 (.then (fn [_] (u/next-frame)))
                 (.then (fn [_]
                          (set! (.-error js/console) orig-error)
                          (let [sync-unmount (filter #(re-find #"synchronously unmount" %) @errors)
                                flush-warns  (filter #(re-find #"flushSync was called" %) @errors)]
                            (is (empty? sync-unmount)
                                "no React synchronous-unmount DEV error on destroy-from-commit")
                            ;; probe only (agd-01kzr5qwmfyx): a hit means :init's
                            ;; flushSync warns from a commit — new ticket, not a failure
                            (when (seq flush-warns)
                              (js/console.log "PROBE flushSync-in-commit warnings:"
                                              (count flush-warns))))
                          (u/detach! el)
                          (done))))))))

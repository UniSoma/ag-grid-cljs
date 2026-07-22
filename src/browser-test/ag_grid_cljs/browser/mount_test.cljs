(ns ag-grid-cljs.browser.mount-test
  "Browser suite (ADR 0015): core mount, a React cell renderer, and an
  update-grid! smoke — the assertions that depend on AG Grid's real runtime
  rendering a grid into live DOM."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.react :as agr]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]
            ["react" :as react]))

(deftest core-mount
  (testing "create-grid! mounts a grid with the given rows into real DOM"
    (let [el  (u/mount-el)
          h   (grid/create-grid!
               el (-> (grid/options)
                      (grid/with-columns [{:field :name} {:field :price}])
                      (grid/with-row-id :id)
                      (grid/with-row-data #js [#js {:id 1 :name "Ada"  :price 42}
                                               #js {:id 2 :name "Alan" :price 37}])))
          api (grid/grid-api h)]
      (async done
             (-> (u/next-frame)
                 (.then (fn [_]
                          (is (= 2 (.getDisplayedRowCount api)))
                          ;; select via AG Grid's first-party data-testid hooks (ADR 0015 §2)
                          (is (some? (u/by-testid el (.headerCell u/testid "name")))
                              "a header cell rendered into the DOM")
                          (is (some? (u/by-testid el (.cell u/testid "1" "name")))
                              "a data cell rendered into the DOM")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done))))))))

(defn- badge-fc [props]
  (react/createElement "span" #js {:className "react-badge"}
                       (str "R:" (unchecked-get props "name"))))

(deftest react-cell-renderer
  (testing "a React cell renderer mounts a live component into a cell"
    (let [el (u/mount-el)
          h  (grid/create-grid!
              el (-> (grid/options)
                     (grid/with-columns
                       [{:field :name}
                        {:header-name   "R"
                         :cell-renderer (agr/react-renderer
                                         (fn [p] (badge-fc #js {:name (-> p :data :name)})))}])
                     (grid/with-row-data #js [#js {:name "Ada"}])))]
      (async done
             (-> (u/next-frame)
                 (.then (fn [_]
                          (let [badge (.querySelector el ".react-badge")]
                            (is (some? badge) "the React component rendered into a cell")
                            (is (= "R:Ada" (.-textContent badge))))
                          (grid/destroy! h)
                          (u/detach! el)
                          (done))))))))

(deftest update-grid!-applies-to-live-grid
  (testing "update-grid! pushes an updatable option onto the live grid"
    (let [el  (u/mount-el)
          h   (grid/create-grid!
               el (-> (grid/options)
                      (grid/with-columns [{:field :name}])
                      (grid/with-row-data #js [#js {:name "Ada"} #js {:name "Alan"}
                                               #js {:name "Grace"}])))
          api (grid/grid-api h)]
      (async done
             (-> (u/next-frame)
                 (.then (fn [_]
                          (is (= 3 (.getDisplayedRowCount api)))
                          (grid/update-grid! h {:quick-filter-text "ada"})
                          (u/next-frame)))
                 (.then (fn [_]
                          (is (= 1 (.getDisplayedRowCount api))
                              "quick-filter-text applied via update-grid! filtered the grid live")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done))))))))

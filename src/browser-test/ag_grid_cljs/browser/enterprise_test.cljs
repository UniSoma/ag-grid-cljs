(ns ag-grid-cljs.browser.enterprise-test
  "Browser suite (ADR 0015): the unlicensed Enterprise smoke. Mounts a grid with
  an Enterprise feature (cell selection + fill handle) under the no-license path;
  the license console errors this emits are allowlisted by the Playwright driver,
  which fails the run on any OTHER console error (the regression tripwire)."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]))

(deftest enterprise-cell-selection-smoke
  (testing "an Enterprise feature is active on a live grid under the no-license path"
    (let [el  (u/mount-el)
          h   (grid/create-grid!
               el (-> (grid/options)
                      (grid/with-columns [{:field :name} {:field :born}])
                      (grid/with-cell-selection {:handle {:mode "fill"}})
                      (grid/with-row-data #js [#js {:name "Ada"  :born 1815}
                                               #js {:name "Alan" :born 1912}])))
          api (grid/grid-api h)]
      (async done
             (-> (u/next-frame)
                 (.then (fn [_]
                          (is (= 2 (.getDisplayedRowCount api)))
                          (is (boolean (.getGridOption api "cellSelection"))
                              "the Enterprise cellSelection option is set on the live grid")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done))))))))

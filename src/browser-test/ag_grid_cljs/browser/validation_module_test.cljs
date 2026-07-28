(ns ag-grid-cljs.browser.validation-module-test
  "Browser suite (ADR 0015): the one assertion behind ADR 0020 — that
  `create-grid!` registering `ValidationModule` in dev builds actually puts AG
  Grid's own deprecation warning on the console, with no `register!` call from
  the consumer.

  This is the replacement for the wrapper's deleted deprecation branch, so it is
  the test that carries the deletion's justification. It is about AG Grid rather
  than about us (the warning text and its runtime table are upstream), which is
  why it is here and not in the node suite. It goes red on an AG Grid bump that
  moves `GRID_OPTION_DEPRECATIONS` — the signal we want."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing]]))

(deftest ag-grid-deprecation-warning-reaches-the-console
  (testing "a deprecated grid option warns without the consumer registering ValidationModule"
    ;; modules.cljs registers AllEnterpriseModule, which does NOT pull in
    ;; ValidationModule (it is in neither All bundle), so the only thing that can
    ;; make this warning appear is create-grid!'s own registration.
    (let [el       (u/mount-el)
          warnings (atom [])
          orig     js/console.warn]
      (set! js/console.warn (fn [& args]
                              (swap! warnings conj (apply str args))
                              (apply orig args)))
      ;; processOptions runs synchronously inside createGrid, and _warnOnce
      ;; dedupes per message for the whole page — this key is used nowhere else
      ;; in the suite.
      (let [h (try
                (grid/create-grid!
                 el (-> (grid/options)
                        (grid/with-columns [{:field :name}])
                        (grid/with-row-data #js [#js {:name "Ada"}])
                        (assoc :enable-range-selection true)))
                (finally (set! js/console.warn orig)))]
        (is (some #(re-find #"As of v32\.2, enableRangeSelection is deprecated\. Use `cellSelection = true`" %)
                  @warnings)
            "AG Grid's own deprecation warning, carrying its replacement")
        (grid/destroy! h)
        (u/detach! el)))))

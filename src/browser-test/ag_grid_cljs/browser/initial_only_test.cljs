(ns ag-grid-cljs.browser.initial-only-test
  "Browser suite (ADR 0015): the upstream half of the reachability argument
  behind `update-grid!`'s `:initial-only` warning (ADR 0017 appendix).

  AG Grid's equivalent message — `_warn(22)`, \"{key} is an initial property and
  cannot be updated\" — is emitted from
  `ValidationService.warnOnInitialPropertyUpdate`, whose sole caller is
  `GridOptionsService.updateGridOptions`, i.e. the body of `setGridOption`. Our
  `:initial-only` branch skips `setGridOption`, so upstream never sees the key.
  That claim is about AG Grid's runtime rather than about us, which is why it is
  pinned here against a real grid and not in the node suite: it goes red on an AG
  Grid bump that moves the warning to a caller our skip does not bypass — the
  signal that would make deleting our own message correct."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing]]))

;; `:tab-index` is `:initial? true` in our registry AND present in AG Grid's
;; INITIAL_GRID_OPTION_KEYS, so both halves below exercise the same key. It is
;; used nowhere else in the suite: _warn(22) goes through _warnOnce, which
;; dedupes per message for the whole page.
(def ^:private ag-initial-warning
  #"tabIndex is an initial property and cannot be updated")

(defn- warn-lines
  "Run `f`, returning every console.warn line it produced — AG Grid's included,
  unlike the field-check suite's wrapper-only capture."
  [f]
  (let [warnings (atom [])
        orig     js/console.warn]
    (set! js/console.warn (fn [& args]
                            (swap! warnings conj (apply str args))
                            (apply orig args)))
    (try (f) (finally (set! js/console.warn orig)))
    @warnings))

(deftest initial-only-skip-bypasses-ag-grids-own-warning
  (let [el (u/mount-el)
        h  (grid/create-grid!
            el (-> (grid/options)
                   (grid/with-columns [{:field :name}])
                   (grid/with-row-data #js [#js {:name "Ada"}])))]
    (testing "an initial-only key through update-grid! draws no upstream warning"
      (let [w (warn-lines #(grid/update-grid! h {:tab-index 3}))]
        (is (not-any? #(re-find ag-initial-warning %) w)
            "setGridOption is skipped, so warnOnInitialPropertyUpdate never runs")
        ;; Positive control for the not-any? above, not a contract assertion —
        ;; the wrapper's own message is the node suite's (ADR 0015 §1,
        ;; core-test/update-grid!-initial-only-warns-once-and-is-ignored).
        (is (some #(re-find #"grid option :tab-index is initial-only" %) w)
            "the capture is live: the wrapper's own warning did come through")))
    (testing "the same key pushed straight at the grid api does warn"
      (let [w (warn-lines #(.setGridOption ^js (grid/grid-api h) "tabIndex" 4))]
        (is (some #(re-find ag-initial-warning %) w)
            "AG Grid's _warn(22), reachable only via updateGridOptions")))
    (grid/destroy! h)
    (u/detach! el)))

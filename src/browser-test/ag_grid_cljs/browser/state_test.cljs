(ns ag-grid-cljs.browser.state-test
  "Browser suite (ADR 0015): a fine-grained transaction preserves scroll,
  selection and focus. The scroll is a REAL mouse-wheel gesture supplied by the
  Playwright driver (the wrapper's whole point — transactional updates that map
  onto AG Grid's own diffing and keep grid state, ADR 0008)."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is async]]))

(defn- selected-ids [api]
  (mapv #(.-id %) (.getSelectedNodes api)))

(deftest transaction-preserves-scroll-selection-focus
  (let [el     (u/mount-el 300)
        rows   (into-array (map (fn [i] #js {:id i :value (str "v" i)}) (range 50)))
        h      (grid/create-grid!
                el (-> (grid/options)
                       (grid/with-columns [{:field :id} {:field :value}])
                       (grid/with-row-id :id)
                       (grid/with-selection {:mode :multiple})
                       (grid/with-row-data rows)))
        api    (grid/grid-api h)
        ;; scroll position read via the API (version-stable) rather than a
        ;; private DOM class; getVerticalPixelRange -> {:top :bottom} in px
        scroll-top #(.-top (.getVerticalPixelRange api))
        before (atom nil)]
    (async done
           (-> (u/next-frame)
               (.then (fn [_]
                        (.setSelected ^js (.getRowNode api "5") true)
                        (.setFocusedCell api 6 "value")
                   ;; a real mouse wheel over a cell (ADR 0015: the driver supplies
                   ;; mouse primitives; a real scroll exercises AG Grid's row
                   ;; virtualization, which a synthetic scrollTop would not). The
                   ;; target is a first-party data-testid, not an internal class.
                        (u/request-gesture!
                         {:type "wheel" :selector (str "[data-testid=" (pr-str (.cell u/testid "5" "value")) "]") :dy 600})))
               (.then (fn [_] (u/next-frame)))
               (.then (fn [_]
                        (reset! before {:scroll (scroll-top)
                                        :sel    (selected-ids api)
                                        :focus  (.getFocusedCell api)})
                        (is (pos? (:scroll @before)) "the real wheel gesture scrolled the grid body")
                        (is (= ["5"] (:sel @before)) "a row is selected before the transaction")
                        (is (some? (:focus @before)) "a cell is focused before the transaction")
                   ;; a fine-grained update on an OFF-SCREEN row
                        (grid/transact! h {:update [#js {:id 40 :value "changed"}]})
                        (u/next-frame)))
               (.then (fn [_]
                        (is (= (:scroll @before) (scroll-top))
                            "scroll position preserved across the transaction")
                        (is (= (:sel @before) (selected-ids api))
                            "selection preserved across the transaction")
                        (let [f1 (.getFocusedCell api)]
                          (is (some? f1) "focus preserved across the transaction")
                          (is (= (.-rowIndex (:focus @before)) (.-rowIndex f1))))
                        (grid/destroy! h)
                        (u/detach! el)
                        (done)))))))

(ns ag-grid-cljs.browser.rollback-events-test
  "Browser leg of the rollback recipe (ticket agd-01kzs8e9rwhx), covering the
  premises docs/editable-grids.md rests on — all three are claims about AG Grid's
  runtime, not about our code:

  1. a restore across N nodes fires N `cellValueChanged` events, one per cell,
     so a batch rollback without the guard is N spurious writes;
  2. `.setDataValue`'s fourth argument arrives on the event as `:source`, which
     is what the guard reads;
  3. `(:node params)` is the real RowNode (ADR 0018 §2) — `.setDataValue` works
     on it directly and `(.-data node)` is the row, while `(:data node)` is nil."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]))

(defn- rows
  "Fresh row objects per test — the restores below mutate them in place."
  []
  #js [#js {:id "1" :salary 1} #js {:id "2" :salary 2} #js {:id "3" :salary 3}])

(deftest a-restore-across-n-nodes-fires-n-events-carrying-the-sentinel
  (testing "N .setDataValue restores => N cellValueChanged events, each :source
            the sentinel, each :node a live RowNode"
    (let [seen (atom [])
          el   (u/mount-el)
          h    (grid/create-grid!
                el (-> (grid/options)
                       (grid/with-columns [{:field :salary :editable true}])
                       (grid/with-row-id :id)
                       (grid/with-row-data (rows))
                       (assoc :on-cell-value-changed
                              (fn [{:keys [node col-def value old-value source]}]
                                (swap! seen conj
                                       {:field     (:field col-def)
                                        :value     value
                                        :old-value old-value
                                        :source    source
                                        :node-row  (.-data ^js node)
                                        :node-key  (:data node)})))))
          api  (grid/grid-api h)]
      (async done
             (-> (u/poll-testid el (.cell u/testid "1" "salary"))
                 (.then (fn [_]
                          ;; the batch rollback: one restore per node, as
                          ;; flush-batch! writes it in the article.
                          (doseq [id ["1" "2" "3"]]
                            (let [^js node (.getRowNode api id)]
                              (.setDataValue node "salary" 0 "restore")))
                          (u/next-frame)))
                 (.then (fn [_]
                          (let [events @seen]
                            (is (= 3 (count events))
                                "one cellValueChanged per restored cell, not one per batch")
                            (is (= #{"restore"} (set (map :source events)))
                                "the fourth .setDataValue argument arrives as :source")
                            (is (= [0 0 0] (map :value events)))
                            (is (= [1 2 3] (map :old-value events))
                                "each event carries that cell's own old value")
                            (is (= #{"salary"} (set (map :field events)))
                                "(:field col-def) is the emitted string, the spelling the row is keyed at")
                            (is (= ["1" "2" "3"] (map #(.-id ^js (:node-row %)) events))
                                "(:node params) is the real RowNode — (.-data node) is the row")
                            (is (= [nil nil nil] (map :node-key events))
                                "a RowNode is not beaned, so (:data node) is nil"))
                          (grid/destroy! h)
                          (u/detach! el)
                          (done))))))))

(deftest an-unsourced-write-does-not-look-like-the-sentinel
  (testing "without a fourth argument :source is anything but the sentinel, so
            the guard lets an ordinary edit through"
    (let [seen (atom [])
          el   (u/mount-el)
          h    (grid/create-grid!
                el (-> (grid/options)
                       (grid/with-columns [{:field :salary :editable true}])
                       (grid/with-row-id :id)
                       (grid/with-row-data (rows))
                       (assoc :on-cell-value-changed
                              (fn [p] (swap! seen conj (:source p))))))
          api  (grid/grid-api h)]
      (async done
             (-> (u/poll-testid el (.cell u/testid "1" "salary"))
                 (.then (fn [_]
                          (let [^js node (.getRowNode api "1")]
                            (.setDataValue node "salary" 99))
                          (u/next-frame)))
                 (.then (fn [_]
                          (is (= 1 (count @seen)))
                          (is (not= "restore" (first @seen))
                              "the sentinel is ours: AG Grid never supplies it")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done))))))))

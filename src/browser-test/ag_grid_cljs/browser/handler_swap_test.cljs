(ns ag-grid-cljs.browser.handler-swap-test
  "Browser suite (ADR 0015): the upstream half of the handler-key classification
  in ADR 0008's Classification amendment (ticket agd-01kzvjaeg3g7).

  The node suite pins what the wrapper does — a handler key reaches
  `setGridOption` and draws no warning. That is a claim about our differ. The
  claim the amendment actually rests on is about AG Grid's runtime: that
  `setGridOption(\"onCellClicked\", f)` REPLACES the live handler rather than
  stacking a second listener beside it. A stacking implementation would leave the
  node suite green and every consumer double-firing on every hot reload, so it is
  pinned here against a real grid."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]))

(deftest update-grid!-swaps-a-live-handler-rather-than-stacking-one
  (testing "a handler key pushed through update-grid! replaces the handler the
            grid was created with: the new one fires, the old one does not"
    (let [seen (atom [])
          el   (u/mount-el)
          h    (grid/create-grid!
                el (-> (grid/options)
                       (grid/with-columns [{:field :name}])
                       (grid/with-row-id :id)
                       (grid/with-row-data #js [#js {:id "1" :name "Ada"}])
                       (assoc :on-cell-clicked (fn [_] (swap! seen conj :a)))))]
      (async done
             (-> (u/poll-testid el (.cell u/testid "1" "name"))
                 (.then (fn [cell]
                          (grid/update-grid! h {:on-cell-clicked (fn [_] (swap! seen conj :b))})
                          (.click cell)
                          ;; cellClicked flushes asynchronously like every other
                          ;; AG Grid event (ticket agd-01kzscf7xjrg) — wait for
                          ;; the batch rather than guessing a frame.
                          (u/poll-until #(seq @seen))))
                 ;; One more frame before reading: asserting on the first
                 ;; non-empty read would let a stacking implementation pass
                 ;; whenever it happened to dispatch B ahead of A — the exact
                 ;; failure this test exists to catch.
                 (.then (fn [_] (u/next-frame)))
                 (.then (fn [_]
                          (is (= [:b] @seen)
                              "the pushed handler fired, and exactly once — the
                               handler the grid was created with is gone, not
                               stacked behind it")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done)))
                 (.catch (fn [e]
                           ;; poll-testid resolves nil on timeout, so a cell that
                           ;; never rendered throws in the .then above — report it
                           ;; rather than hanging the suite.
                           (is false (str "handler-swap test threw: " e))
                           (grid/destroy! h)
                           (u/detach! el)
                           (done))))))))

(ns ag-grid-cljs.core-test
  "Contract tests for the GridHandle and the retargeted runtime channels
  (ticket agd-01ky5hj2mbj5). DOM-free: create-grid! itself needs a live DOM
  element, so these tests construct a handle over a fake GridApi and verify the
  accessor and channels dispatch onto it."
  (:require [ag-grid-cljs.core :as grid]
            [cljs.test :refer [deftest is testing]]))

(defn- fake-api
  "A stand-in GridApi that records the (method, args) calls made against it."
  []
  (let [calls (atom [])]
    (js-obj
     "calls"          calls
     "setGridOption"  (fn [k v] (swap! calls conj [:set-grid-option k v]))
     "applyTransaction" (fn [tx] (swap! calls conj [:apply-transaction tx]) #js {:add [] :update [] :remove []})
     "destroy"        (fn [] (swap! calls conj [:destroy])))))

(defn- calls-of [api] @(unchecked-get api "calls"))

(deftest grid-api-accessor
  (testing "grid-api pulls the raw GridApi back out of the handle"
    (let [api    (fake-api)
          handle (grid/->GridHandle api {:column-defs []})]
      (is (identical? api (grid/grid-api handle))))))

(deftest set-rows!-targets-the-handle
  (let [api    (fake-api)
        handle (grid/->GridHandle api {})
        rows   #js [#js {:id 1}]]
    (grid/set-rows! handle rows)
    (is (= [[:set-grid-option "rowData" rows]] (calls-of api)))))

(deftest transact!-targets-the-handle
  (let [api    (fake-api)
        handle (grid/->GridHandle api {})]
    (testing "the tx map is forward-converted before it hits the api"
      (grid/transact! handle {:add [#js {:id 1}]})
      (let [[[method tx]] (calls-of api)]
        (is (= :apply-transaction method))
        (is (array? (unchecked-get tx "add")))))))

(deftest destroy!-targets-the-handle
  (let [api    (fake-api)
        handle (grid/->GridHandle api {})]
    (grid/destroy! handle)
    (is (= [[:destroy]] (calls-of api)))))

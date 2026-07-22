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

(defn- handle
  "A GridHandle over a fresh fake api with the given stashed EDN opts."
  ([] (handle {}))
  ([opts] (grid/->GridHandle (fake-api) opts (atom #{}))))

(defn- set-grid-option-calls [h]
  (filterv (fn [[m]] (= m :set-grid-option)) (calls-of (grid/grid-api h))))

(defn- capture
  "Run f with js/console.warn captured; return the vector of warning strings."
  [f]
  (let [warnings (atom [])
        orig js/console.warn]
    (set! js/console.warn (fn [& args] (swap! warnings conj (apply str args))))
    (try (f) (finally (set! js/console.warn orig)))
    @warnings))

(deftest grid-api-accessor
  (testing "grid-api pulls the raw GridApi back out of the handle"
    (let [api (fake-api)
          h   (grid/->GridHandle api {:column-defs []} (atom #{}))]
      (is (identical? api (grid/grid-api h))))))

(deftest set-rows!-targets-the-handle
  (let [h    (handle)
        rows #js [#js {:id 1}]]
    (grid/set-rows! h rows)
    (is (= [[:set-grid-option "rowData" rows]] (calls-of (grid/grid-api h))))))

(deftest transact!-targets-the-handle
  (let [h (handle)]
    (testing "the tx map is forward-converted before it hits the api"
      (grid/transact! h {:add [#js {:id 1}]})
      (let [[[method tx]] (calls-of (grid/grid-api h))]
        (is (= :apply-transaction method))
        (is (array? (unchecked-get tx "add")))))))

(deftest destroy!-targets-the-handle
  (let [h (handle)]
    (grid/destroy! h)
    (is (= [[:destroy]] (calls-of (grid/grid-api h))))))

;; --- update-grid! PATCH differ (ADR 0008) -----------------------------------

(deftest update-grid!-applies-only-changed-updatable-keys
  (testing "a changed updatable key produces exactly one setGridOption"
    (let [h (handle {:pagination false})]
      (grid/update-grid! h {:pagination true})
      (is (= [[:set-grid-option "pagination" true]] (set-grid-option-calls h)))))
  (testing "an unchanged key present in new-opts produces no call"
    (let [h (handle {:pagination true})]
      (grid/update-grid! h {:pagination true})
      (is (= [] (set-grid-option-calls h)))))
  (testing "keys absent from new-opts produce no call"
    (let [h (handle {:pagination false :quick-filter-text "x"})]
      (grid/update-grid! h {:pagination true})
      (is (= [[:set-grid-option "pagination" true]] (set-grid-option-calls h)))))
  (testing "several changed updatable keys each produce one call"
    (let [h (handle {:pagination false :quick-filter-text "x"})]
      (grid/update-grid! h {:pagination true :quick-filter-text "ada"})
      (is (= #{[:set-grid-option "pagination" true]
               [:set-grid-option "quickFilterText" "ada"]}
             (set (set-grid-option-calls h)))))))

(deftest update-grid!-initial-only-warns-once-and-is-ignored
  (let [h (handle {:context {:a 1}})
        w (capture #(do (grid/update-grid! h {:context {:a 2}})
                        (grid/update-grid! h {:context {:a 3}})))]
    (is (= [] (set-grid-option-calls h)) "initial-only key never reaches the api")
    (is (= 1 (count w)) "warns once per key across successive updates")
    (is (re-find #":context is initial-only" (first w)))))

(deftest update-grid!-row-data-warns-and-is-ignored
  (let [h (handle {})
        w (capture #(grid/update-grid! h {:row-data #js [#js {:id 1}]}))]
    (is (= [] (set-grid-option-calls h)) "the data channel owns :row-data")
    (is (= 1 (count w)))
    (is (re-find #":row-data is owned by the data channel" (first w)))))

(deftest update-grid!-unclassified-applies-and-warns
  (let [h (handle {})
        w (capture #(grid/update-grid! h {:totally-bogus-xyz 7}))]
    (is (= [[:set-grid-option "totallyBogusXyz" 7]] (set-grid-option-calls h)))
    (is (= 1 (count w)))
    (is (re-find #"not in the key registry" (first w)))))

(deftest update-grid!-column-defs-ships-whole-value
  (let [h (handle {:column-defs [{:field :a}]})]
    (grid/update-grid! h {:column-defs [{:field :a} {:field :b}]})
    (let [[[method prop val]] (set-grid-option-calls h)]
      (is (= :set-grid-option method))
      (is (= "columnDefs" prop))
      (is (array? val))
      (is (= 2 (.-length val)) "the whole new column-defs value is forward-converted and shipped")
      (is (= "b" (unchecked-get (aget val 1) "field"))))))

(deftest update-grid!-stash-reflects-applied-state
  (testing "the returned handle's stash merges present new keys so later diffs stay minimal"
    (let [h0 (handle {:pagination false})
          h1 (grid/update-grid! h0 {:pagination true})]
      (is (= true (get-in h1 [:opts :pagination])))
      ;; re-applying the same value against the merged stash is a no-op
      (grid/update-grid! h1 {:pagination true})
      (is (= [[:set-grid-option "pagination" true]] (set-grid-option-calls h1))
          "only the first, genuinely-changing update called the api"))))

(ns ag-grid-cljs.browser.field-check-test
  "Browser suite (ADR 0015): the two field-check assertions that are about AG
  Grid rather than about us — that `getColumns()` hands back a FLAT list with
  group children in it, and that the events we listen to actually fire where
  ADR 0017 claims. The pure core, the ColDef->target mapping and the row
  sampling are the node suite's (ag-grid-cljs.impl.field-check-test)."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]))

(defn- start-capture!
  "Begin collecting the wrapper's own console.warn lines. Returns
  `[warnings-atom restore!]`; the capture must outlive the call under test,
  since a warning can arrive from an event listener a frame later."
  []
  (let [warnings (atom [])
        orig js/console.warn]
    (set! js/console.warn
          (fn [& args]
            (let [s (apply str args)]
              (when (re-find #"^\[ag-grid-cljs\]" s) (swap! warnings conj s))
              (apply orig args))))
    [warnings (fn [] (set! js/console.warn orig))]))

(defn- field-warnings [warnings]
  (filterv #(re-find #"is not a key in the row data" %) @warnings))

(defn- abort-on-throw
  "Tail for an async chain: restore console.warn and finish the test rather than
  leave the capture installed for every later test and time the suite out."
  [chain restore! done]
  (.catch chain (fn [e]
                  (restore!)
                  (is false (str "chain rejected: " e))
                  (done))))

(deftest group-child-field-is-checked
  (testing "a mistyped field on a column nested in :children warns — getColumns() flattens the group"
    (let [el (u/mount-el)
          [warnings restore!] (start-capture!)
          h  (grid/create-grid!
              el (-> (grid/options)
                     (grid/with-columns
                       [{:header-name "Person"
                         :children    [{:field :name} {:field :frist-name}]}])
                     (grid/with-row-data #js [#js {:name "Ada" :firstName "Ada"}])))]
      (async done
             (-> (u/next-frame)
                 (.then (fn [_]
                          (restore!)
                          (let [w (field-warnings warnings)]
                            (is (= 1 (count w))
                                "only the mistyped child field warned")
                            (is (re-find #"column field \"fristName\"" (first w)))
                            (is (re-find #"did you mean \"firstName\"\?" (first w))))
                          (grid/destroy! h)
                          (u/detach! el)
                          (done)))
                 (abort-on-throw restore! done))))))

(deftest fires-at-creation-and-when-columns-change
  (let [el (u/mount-el)
        [warnings restore!] (start-capture!)
        h  (grid/create-grid!
            el (-> (grid/options)
                   (grid/with-columns [{:field :nmae}])
                   (grid/with-row-data #js [#js {:name "Ada" :salary 1}])))]
    (async done
           (-> (u/next-frame)
               (.then (fn [_]
                        (let [w (field-warnings warnings)]
                          (is (= 1 (count w)) "warned at creation")
                          (is (re-find #"\"nmae\"" (first w))))
                        ;; :column-defs replaced -> newColumnsLoaded
                        (grid/update-grid! h {:column-defs [{:field :sallary}]})
                        (u/next-frame)))
               (.then (fn [_]
                        (restore!)
                        (let [w (field-warnings warnings)]
                          (is (= 2 (count w)) "warned again for the new column")
                          (is (re-find #"column field \"sallary\"" (second w)))
                          (is (re-find #"did you mean \"salary\"\?" (second w))))
                        (grid/destroy! h)
                        (u/detach! el)
                        (done)))
               (abort-on-throw restore! done)))))

(deftest silent-until-a-row-lands-then-fires-on-transaction
  (let [el (u/mount-el)
        [warnings restore!] (start-capture!)
        h  (grid/create-grid!
            el (-> (grid/options)
                   (grid/with-columns [{:field :nmae}])
                   (grid/with-row-id :id)
                   (grid/with-row-data #js [])))]
    (async done
           (-> (u/next-frame)
               (.then (fn [_]
                        (is (empty? (field-warnings warnings))
                            "no row has loaded, so there is nothing to compare against")
                        ;; data arriving by the transaction route -> modelUpdated
                        (grid/transact! h {:add [#js {:id 1 :name "Ada"}]})
                        (u/next-frame)))
               (.then (fn [_]
                        (restore!)
                        (let [w (field-warnings warnings)]
                          (is (= 1 (count w)) "the transaction supplied the sample row")
                          (is (re-find #"column field \"nmae\"" (first w)))
                          (is (re-find #"did you mean \"name\"\?" (first w))))
                        (grid/destroy! h)
                        (u/detach! el)
                        (done)))
               (abort-on-throw restore! done)))))

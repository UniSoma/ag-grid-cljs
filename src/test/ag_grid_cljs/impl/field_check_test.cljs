(ns ag-grid-cljs.impl.field-check-test
  "Field-check contract (ticket agd-01kyd3sefe7e, ADR 0017): the always-on dev
  diagnostic comparing each column's emitted field string against the keys of one
  sampled row. Node owns our contract given AG Grid's answers, so the Column and
  GridApi objects come from test-support's fakes; the browser suite owns the two
  assertions that are about AG Grid itself. Deliberately never calls
  enable-dev-validations! —
  the field check is not behind that gate."
  (:require [cljs.test :refer [deftest is testing]]
            [ag-grid-cljs.impl.validate :as v]
            [ag-grid-cljs.test-support
             :refer [capture fake-api fake-col fake-node fire!]]))

;; --- check-fields! (pure core) ----------------------------------------------

(defn- field-target [s] {:kind :field :field s :row-key s})

(deftest absent-field-warns-with-suggestion
  (let [w (capture #(v/check-fields! (atom #{})
                                     [(field-target "fristName")]
                                     #js {:firstName "Ada" :salary 1}))]
    (is (= 1 (count w)))
    (is (= (str "[ag-grid-cljs] column field \"fristName\" is not a key in the "
                "row data — did you mean \"firstName\"?")
           (first w)))))

(deftest absent-field-with-no-near-match-warns-bare
  (let [w (capture #(v/check-fields! (atom #{})
                                     [(field-target "xyz")]
                                     #js {:firstName "Ada"}))]
    (is (= 1 (count w)))
    (is (= "[ag-grid-cljs] column field \"xyz\" is not a key in the row data"
           (first w)))
    (is (not (re-find #"did you mean" (first w))))))

(deftest present-field-is-silent
  (is (empty? (capture #(v/check-fields! (atom #{})
                                         [(field-target "firstName")]
                                         #js {:firstName "Ada"})))))

(deftest nil-value-counts-as-present
  (testing "presence is key membership, not truthiness"
    (is (empty? (capture #(v/check-fields! (atom #{})
                                           [(field-target "middleName")]
                                           #js {:middleName nil}))))))

(deftest prototype-key-counts-as-present
  (testing "presence walks the prototype chain, so class instances stay quiet"
    (let [row (js/Object.create #js {:computed 1})]
      (set! (.-firstName row) "Ada")
      (is (empty? (capture #(v/check-fields! (atom #{})
                                             [(field-target "computed")]
                                             row))))))
  (testing "suggestions come from own enumerable keys only"
    (let [w (capture #(v/check-fields! (atom #{})
                                       [(field-target "toStrng")]
                                       #js {:firstName "Ada"}))]
      (is (= 1 (count w)))
      (is (not (re-find #"toString" (first w)))))))

(deftest tooltip-field-wording
  (let [w (capture #(v/check-fields! (atom #{})
                                     [{:kind :tooltip-field
                                       :field "sallary" :row-key "sallary"}]
                                     #js {:salary 1}))]
    (is (= 1 (count w)))
    (is (= (str "[ag-grid-cljs] column tooltip field \"sallary\" is not a key "
                "in the row data — did you mean \"salary\"?")
           (first w)))))

(deftest non-object-row-bails-silently
  (testing "nil, a string, and a number never reach the `in` operator"
    (doseq [row [nil js/undefined "a string" 42 true]]
      (is (empty? (capture #(v/check-fields! (atom #{})
                                             [(field-target "xyz")]
                                             row)))
          (str "row " (pr-str row) " bails silently"))))
  (testing "a non-object row resolves nothing, so a later real row still warns"
    (let [state (atom #{})]
      (capture #(v/check-fields! state [(field-target "xyz")] nil))
      (is (= 1 (count (capture #(v/check-fields! state
                                                 [(field-target "xyz")]
                                                 #js {:a 1}))))))))

(deftest warns-once-per-field
  (let [state (atom #{})
        row #js {:firstName "Ada"}
        targets [(field-target "fristName")]
        w1 (capture #(v/check-fields! state targets row))
        w2 (capture #(v/check-fields! state targets row))]
    (is (= 1 (count w1)))
    (is (empty? w2) "a second run over the same field is silent"))
  (testing "a field found present is also resolved, so it is never re-checked"
    (let [state (atom #{})]
      (capture #(v/check-fields! state [(field-target "a")] #js {:a 1}))
      (is (empty? (capture #(v/check-fields! state
                                             [(field-target "a")]
                                             #js {:b 2})))))))

;; --- field-targets (ColDef -> what gets checked) ----------------------------

(deftest value-getter-drops-field-but-keeps-tooltip-field
  (testing "no valueGetter: both field and tooltipField are checked"
    (is (= [{:kind :field :field "a" :row-key "a"}
            {:kind :tooltip-field :field "t" :row-key "t"}]
           (v/field-targets (fake-col #js {:field "a" :tooltipField "t"})))))
  (testing "a valueGetter supersedes field, but never tooltipField"
    (is (= [{:kind :tooltip-field :field "t" :row-key "t"}]
           (v/field-targets
            (fake-col #js {:field "a" :tooltipField "t"
                           :valueGetter (fn [_])})))))
  (testing "a ColDef with neither yields nothing"
    (is (= [] (v/field-targets (fake-col #js {:headerName "Grp"}))))))

(deftest dot-notation-checks-only-the-first-segment
  (testing "isFieldContainsDots true: the first segment is the looked-up key"
    (is (= [{:kind :field :field "address.city" :row-key "address"}]
           (v/field-targets (fake-col #js {:field "address.city"} true false)))))
  (testing "false (suppressFieldDotNotation): the whole string is the key"
    (is (= [{:kind :field :field "address.city" :row-key "address.city"}]
           (v/field-targets (fake-col #js {:field "address.city"} false false)))))
  (testing "the tooltip field asks its own predicate"
    (is (= [{:kind :tooltip-field :field "a.b" :row-key "a"}]
           (v/field-targets (fake-col #js {:tooltipField "a.b"} false true))))))

(deftest dot-notation-warning-names-the-field-but-suggests-for-the-key
  (testing "the message names what AG Grid looks up; the did-you-mean compares the first segment"
    (let [w (capture #(v/check-fields!
                       (atom #{})
                       [{:kind :field :field "adress.city" :row-key "adress"}]
                       #js {:address #js {:city "London"}}))]
      (is (= 1 (count w)))
      (is (= (str "[ag-grid-cljs] column field \"adress.city\" is not a key in "
                  "the row data — did you mean \"address\"?")
             (first w))))))

;; --- first-row (row sampling) -----------------------------------------------

(deftest first-row-skips-group-and-dataless-nodes
  (testing "the first leaf node carrying data wins"
    (let [[api] (fake-api {:nodes [(fake-node #js {:a 1} false)
                                   (fake-node #js {:b 2} false)]})]
      (is (= 1 (.-a ^js (v/first-row api))))))
  (testing "group nodes are skipped, data or not"
    (let [[api] (fake-api {:nodes [(fake-node nil true)
                                   (fake-node #js {:country "UK"} true)
                                   (fake-node #js {:a 1} false)]})]
      (is (= 1 (.-a ^js (v/first-row api))))))
  (testing "nodes with nil data are skipped"
    (let [[api] (fake-api {:nodes [(fake-node nil false)
                                   (fake-node #js {:a 1} false)]})]
      (is (= 1 (.-a ^js (v/first-row api))))))
  (testing "nil when nothing yields a leaf data row"
    (let [[api] (fake-api {:nodes []})]
      (is (nil? (v/first-row api))))
    (let [[api] (fake-api {:nodes [(fake-node nil true)]})]
      (is (nil? (v/first-row api))))))

;; --- install-field-check! ---------------------------------------------------

(deftest install-registers-two-listeners-and-runs-once
  (let [[api calls] (fake-api {:columns [(fake-col #js {:field "fristName"})]
                               :nodes [(fake-node #js {:firstName "Ada"} false)]})
        w (capture #(v/install-field-check! api))]
    (is (= ["modelUpdated" "newColumnsLoaded"] (mapv first (:listeners @calls)))
        "exactly two listeners, on data arrival and on columns replaced")
    (is (= 1 (:get-columns @calls)) "the immediate run happened, exactly once")
    (is (= 1 (count w)) "and it warned, with no enable-dev-validations! call")
    (is (re-find #"\"fristName\"" (first w)))))

(deftest install-is-silent-until-a-leaf-row-lands
  (testing "a datasource that has not delivered yet: installed, checked, silent"
    (let [[api calls nodes] (fake-api
                             {:columns [(fake-col #js {:field "fristName"})]
                              :nodes []})]
      (is (empty? (capture #(v/install-field-check! api)))
          "nothing to compare against, so no verdict and no warning")
      (is (empty? (capture #(fire! calls "modelUpdated")))
          "an empty model update is still silent")
      (testing "a block lands, and the very next modelUpdated warns"
        (reset! nodes [(fake-node #js {:firstName "Ada"} false)])
        (let [w (capture #(fire! calls "modelUpdated"))]
          (is (= 1 (count w)))
          (is (re-find #"\"fristName\"" (first w))))))))

(deftest nil-columns-short-circuits-before-the-row-model
  (testing "getColumns() returns null until colModel.ready"
    (let [[api calls] (fake-api {:columns nil
                                 :nodes [(fake-node #js {:a 1} false)]})]
      (is (empty? (capture #(v/install-field-check! api))))
      (is (zero? (:for-each-node @calls)) "the row model was never touched"))))

(deftest destroyed-grid-short-circuits-before-any-api-call
  (testing "a listener firing after destroy touches nothing but isDestroyed —
            modelUpdated is async-dispatched, so a queued closure can outlive
            the grid (agd-01kzq9x25fez)"
    (let [[api calls nodes destroyed?] (fake-api
                                        {:columns [(fake-col #js {:field "fristName"})]
                                         :nodes []})]
      (is (empty? (capture #(v/install-field-check! api))))
      (reset! nodes [(fake-node #js {:firstName "Ada"} false)])
      (reset! destroyed? true)
      (let [before (:get-columns @calls)]
        (is (empty? (capture #(fire! calls "modelUpdated")))
            "no warning from a dead grid")
        (is (= before (:get-columns @calls))
            "getColumns was never called — the guard returned first")))))

(deftest resolved-fields-short-circuit-the-row-model
  (testing "every field resolved -> return before traversing the rows"
    (let [[api calls] (fake-api
                       {:columns [(fake-col #js {:field "fristName"})]
                        :nodes [(fake-node #js {:firstName "Ada"} false)]})
          _ (capture #(v/install-field-check! api))
          before (:for-each-node @calls)]
      (is (= 1 before))
      (is (empty? (capture #(fire! calls "newColumnsLoaded")))
          "a re-run is silent — newColumnsLoaded also fires on sort and resize")
      (is (= before (:for-each-node @calls))
          "and cheap: the steady state is a set-membership test"))))

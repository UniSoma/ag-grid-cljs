(ns ag-grid-cljs.impl.ref-data-check-test
  "Ref-data-check contract (ticket agd-01kyqmb9ssq7, ADR 0019 §9): the always-on
  dev diagnostic comparing a :ref-data column's sampled row value against that
  column's own emitted refData keys. Node owns the condition, every silence case,
  the per-grid period and the suggestion; the browser suite owns the one
  assertion that is about AG Grid — that an unmatched key renders the cell blank.
  Deliberately never calls enable-dev-validations!: this check is not behind that
  gate."
  (:require [cljs.test :refer [deftest is testing]]
            [ag-grid-cljs.impl.validate :as v]
            [ag-grid-cljs.test-support
             :refer [capture fake-api fake-col fake-node fire!]]))

(defn- target
  "A ref-data target for `field` over `ref-keys` (the EMITTED refData keys)."
  ([ref-keys] (target "status" ref-keys))
  ([field ref-keys]
   {:col-id field :field field :dots? false :ref-keys ref-keys}))

(defn- row [status] #js {:status status})

;; --- the condition ----------------------------------------------------------

(deftest kebab-row-against-camelized-key-warns
  (testing "the whole bug: :ref-data {:in-progress ...} emits inProgress, the row holds in-progress"
    (let [w (capture #(v/check-ref-data! (atom #{})
                                         [(target ["inProgress" "done"])]
                                         (row "in-progress")))]
      (is (= 1 (count w)))
      (is (= (str "[ag-grid-cljs] column field \"status\" :ref-data has no key "
                  "\"in-progress\" — the row value AG Grid looks up, so the cell "
                  "renders blank. Nearest key: \"inProgress\". That is what "
                  "conversion emits for the keyword key :in-progress — :ref-data "
                  "keys are your rows' values, not AG Grid vocabulary. Write "
                  "\"in-progress\".")
             (first w))
          "names the property AG Grid looked up, the near-matching key, and the fix"))))

(deftest a-plain-typo-reports-the-mismatch-without-prescribing-a-side
  (testing "the nearest key is not the value's camelization, so which side is
            misspelled is unknowable — do not tell a consumer to break a correct
            :ref-data by rewriting it to match typo'd data"
    (let [w (capture #(v/check-ref-data! (atom #{})
                                         [(target ["in-progress" "done"])]
                                         (row "in-progres")))]
      (is (= 1 (count w)))
      (is (= (str "[ag-grid-cljs] column field \"status\" :ref-data has no key "
                  "\"in-progres\" — the row value AG Grid looks up, so the cell "
                  "renders blank. Nearest key: \"in-progress\". A :ref-data key "
                  "must be spelled exactly like the row values it maps.")
             (first w)))
      (is (not (re-find #"Write" (first w)))
          "no prescription: the typo could be on either side")))
  (testing "a typo in the KEY under the camel recipe is the same shape"
    (let [w (capture #(v/check-ref-data! (atom #{})
                                         [(target ["inProgres"])]
                                         (row "inProgress")))]
      (is (= 1 (count w)))
      (is (re-find #"Nearest key: \"inProgres\"" (first w)))
      (is (not (re-find #"Write" (first w)))))))

(deftest camel-row-recipe-is-silent
  (testing "the false positive this check was designed to refute: under the camel
            row recipe the keyword key camelizes INTO the row's spelling"
    (is (empty? (capture #(v/check-ref-data! (atom #{})
                                             [(target ["inProgress" "done"])]
                                             (row "inProgress")))))))

(deftest string-key-under-the-kebab-recipe-is-silent
  (testing "the prescribed fix: a string :ref-data key arrives verbatim"
    (is (empty? (capture #(v/check-ref-data! (atom #{})
                                             [(target ["in-progress" "done"])]
                                             (row "in-progress")))))))

;; --- silence cases ----------------------------------------------------------

(deftest no-near-match-is-silent
  (testing ":ref-data is sparse by intent, so an unmapped value is not a misspelling"
    (is (empty? (capture #(v/check-ref-data! (atom #{})
                                             [(target ["alpha" "beta"])]
                                             (row "in-progress")))))))

(deftest non-string-and-empty-values-are-silent
  (testing "no lookup happens, so there is nothing to be wrong about"
    (doseq [v [nil js/undefined "" 42 true]]
      (is (empty? (capture #(v/check-ref-data! (atom #{})
                                               [(target ["inProgress"])]
                                               (row v))))
          (str "row value " (pr-str v) " is silent")))))

(deftest a-field-the-field-check-reported-absent-is-silent
  (testing "an absent field yields no value, so this check never doubles the warning"
    (is (empty? (capture #(v/check-ref-data! (atom #{})
                                             [(target "staus" ["inProgress"])]
                                             (row "in-progress")))))))

(deftest no-rows-loaded-resolves-nothing
  (testing "a non-object row (including nil) warns nothing and reaches no verdict"
    (let [state (atom #{})]
      (doseq [r [nil js/undefined "a string" 42]]
        (is (empty? (capture #(v/check-ref-data! state
                                                 [(target ["inProgress"])]
                                                 r)))))
      (is (= 1 (count (capture #(v/check-ref-data! state
                                                   [(target ["inProgress"])]
                                                   (row "in-progress")))))
          "so the first real row still warns"))))

(deftest value-formatter-and-value-getter-supersede-the-check
  (let [rd #js {"inProgress" "In Progress"}]
    (testing "no supersession: the column is checked"
      (is (= [{:col-id "status" :field "status" :dots? false
               :ref-keys ["inProgress"]}]
             (v/ref-data-targets (fake-col #js {:field "status" :refData rd})))))
    (testing "a valueFormatter means AG Grid never consults refData at all"
      (is (= [] (v/ref-data-targets
                 (fake-col #js {:field "status" :refData rd
                                :valueFormatter (fn [_])})))))
    (testing "a valueGetter means the emitted field is not where the value comes from"
      (is (= [] (v/ref-data-targets
                 (fake-col #js {:field "status" :refData rd
                                :valueGetter (fn [_])})))))
    (testing "no refData, or no field, is nothing to check"
      (is (= [] (v/ref-data-targets (fake-col #js {:field "status"}))))
      (is (= [] (v/ref-data-targets (fake-col #js {:refData rd})))))))

;; --- dotted fields ----------------------------------------------------------

(deftest dotted-field-walks-the-whole-path
  (testing "unlike the field check's presence test, the VALUE needs every segment"
    (let [w (capture #(v/check-ref-data!
                       (atom #{})
                       [{:col-id "job" :field "job.status" :dots? true
                         :ref-keys ["inProgress"]}]
                       #js {:job #js {:status "in-progress"}}))]
      (is (= 1 (count w)))
      (is (re-find #"column field \"job\.status\"" (first w)))))
  (testing "a missing hop yields no value, and silence"
    (is (empty? (capture #(v/check-ref-data!
                           (atom #{})
                           [{:col-id "job" :field "job.status" :dots? true
                             :ref-keys ["inProgress"]}]
                           #js {:other 1})))))
  (testing "dots? false (suppressFieldDotNotation): the whole string is the key"
    (let [r (js-obj "job.status" "in-progress")
          w (capture #(v/check-ref-data!
                       (atom #{})
                       [{:col-id "job" :field "job.status" :dots? false
                         :ref-keys ["inProgress"]}]
                       r))]
      (is (= 1 (count w))))))

;; --- period: once per grid, not once per process -----------------------------

(defn- grid-with-the-bug []
  (fake-api {:columns [(fake-col #js {:field "status"
                                      :refData #js {"inProgress" "In Progress"}})]
             :nodes [(fake-node (row "in-progress") false)]}))

(deftest warns-once-per-column-within-one-grid
  (let [state (atom #{})
        targets [(target ["inProgress"])]
        r (row "in-progress")]
    (is (= 1 (count (capture #(v/check-ref-data! state targets r)))))
    (is (empty? (capture #(v/check-ref-data! state targets r)))
        "a second pass over the same field is silent"))
  (testing "a value found present is also resolved, so it is never re-checked"
    (let [state (atom #{})]
      (capture #(v/check-ref-data! state [(target ["in-progress"])]
                                   (row "in-progress")))
      (is (empty? (capture #(v/check-ref-data! state [(target ["inProgress"])]
                                               (row "in-progress"))))))))

(deftest two-columns-over-one-field-are-two-questions
  (testing "state keys on the col id, not the field: this check asks about the
            column's own map, so a raw column and a labelled one both get checked"
    (let [rd #js {"inProgress" "In Progress"}
          [api] (fake-api
                 {:columns [(fake-col #js {:colId "raw" :field "status"})
                            (fake-col #js {:colId "kw" :field "status" :refData rd})
                            (fake-col #js {:colId "str" :field "status"
                                           :refData (js-obj "in-progress" "In Progress")})]
                  :nodes [(fake-node (row "in-progress") false)]})
          w (capture #(v/install-ref-data-check! api))]
      (is (= 1 (count w)) "the camelized column warns; the string-keyed one matches")
      (is (re-find #"\"inProgress\"" (first w))))))

(deftest period-is-per-grid-not-per-process
  (testing "one capture spans both grids, so the process-wide set is not reset between them"
    (let [w (capture #(do (v/install-ref-data-check! (first (grid-with-the-bug)))
                          (v/install-ref-data-check! (first (grid-with-the-bug)))))]
      (is (= 2 (count w))
          "a second grid with differently-spelled rows warns again (ADR 0022 §1)"))))

;; --- install-ref-data-check! -------------------------------------------------

(deftest install-registers-two-listeners-and-runs-once
  (let [[api calls] (grid-with-the-bug)
        w (capture #(v/install-ref-data-check! api))]
    (is (= ["modelUpdated" "newColumnsLoaded"] (mapv first (:listeners @calls)))
        "exactly two listeners, on data arrival and on columns replaced")
    (is (= 1 (:get-columns @calls)) "the immediate run happened, exactly once")
    (is (= 1 (count w)) "and it warned, with no enable-dev-validations! call")
    (is (re-find #"\"in-progress\"" (first w)))))

(deftest install-is-silent-until-a-row-lands
  (let [[api calls nodes] (fake-api
                           {:columns [(fake-col
                                       #js {:field "status"
                                            :refData #js {"inProgress" "In Progress"}})]
                            :nodes []})]
    (is (empty? (capture #(v/install-ref-data-check! api)))
        "no row, so no value to look up and no verdict")
    (reset! nodes [(fake-node (row "in-progress") false)])
    (let [w (capture #(fire! calls "modelUpdated"))]
      (is (= 1 (count w)) "the very next modelUpdated warns"))))

(deftest a-grid-with-no-ref-data-column-never-touches-the-row-model
  (testing "no targets -> the short-circuit fires before forEachNode"
    (let [[api calls] (fake-api {:columns [(fake-col #js {:field "status"})]
                                 :nodes [(fake-node (row "in-progress") false)]})]
      (is (empty? (capture #(v/install-ref-data-check! api))))
      (is (zero? (:for-each-node @calls))))))

(deftest resolved-columns-short-circuit-the-row-model
  (testing "newColumnsLoaded also fires on sort and resize, so the steady state must be cheap"
    (let [[api calls] (grid-with-the-bug)
          _ (capture #(v/install-ref-data-check! api))
          before (:for-each-node @calls)]
      (is (= 1 before))
      (is (empty? (capture #(fire! calls "newColumnsLoaded"))))
      (is (= before (:for-each-node @calls))))))

(deftest nil-columns-short-circuits-before-the-row-model
  (testing "getColumns() returns null until colModel.ready"
    (let [[api calls] (fake-api {:columns nil
                                 :nodes [(fake-node (row "in-progress") false)]})]
      (is (empty? (capture #(v/install-ref-data-check! api))))
      (is (zero? (:for-each-node @calls))))))

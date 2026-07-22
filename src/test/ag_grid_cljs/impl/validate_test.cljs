(ns ag-grid-cljs.impl.validate-test
  "Dev-mode validation contract (ticket agd-01ky5hj2zhjt, ADR 0007 §4-5).
  goog.DEBUG is true under the node-test compile, so the guarded code runs."
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [ag-grid-cljs.impl.convert :as c]
            [ag-grid-cljs.impl.validate :as v]))

;; --- warn capture -----------------------------------------------------------

(def ^:dynamic *warnings* nil)

(defn capture
  "Run f with js/console.warn captured; return the vector of warning strings."
  [f]
  (let [warnings (atom [])
        orig js/console.warn]
    (v/reset-warnings!)
    (set! js/console.warn (fn [& args] (swap! warnings conj (apply str args))))
    (try (f) (finally (set! js/console.warn orig)))
    @warnings))

(use-fixtures :each
  {:before (fn [] (v/enable!) (v/reset-warnings!))})

(defn warns-for [opts]
  (capture #(v/validate-options! opts)))

;; --- unknown top-level key --------------------------------------------------

(deftest top-level-typo-did-you-mean
  (let [w (warns-for {:row-dat (clj->js [])})]
    (is (= 1 (count w)))
    (is (re-find #"unknown grid option :row-dat" (first w)))
    (is (re-find #"did you mean :row-data\?" (first w)))))

(deftest top-level-unknown-no-suggestion
  (let [w (warns-for {:totally-bogus-nonsense-xyz 1})]
    (is (= 1 (count w)))
    (is (re-find #"unknown grid option :totally-bogus-nonsense-xyz" (first w)))
    (is (not (re-find #"did you mean" (first w))))))

(deftest valid-top-level-keys-silent
  (testing "real grid option, event handler, and already-camel form all pass"
    (is (empty? (warns-for {:row-data (clj->js [])
                            :on-cell-clicked (fn [_])
                            :rowHeight 30
                            :context (c/raw {:anything :goes})})))))

(deftest event-handler-typo-suggests-handler-form
  ;; Event handlers are top-level keys written :on-*; a typo must suggest the
  ;; on-prefixed handler, not the bare event name.
  (let [w (warns-for {:on-cell-clickd (fn [_])})]
    (is (= 1 (count w)))
    (is (re-find #"unknown grid option :on-cell-clickd" (first w)))
    (is (re-find #"did you mean :on-cell-clicked\?" (first w)))))

;; --- ColDef positions -------------------------------------------------------

(deftest coldef-typo-in-column-defs
  (let [w (warns-for {:column-defs [{:field :name} {:fild :age}]})]
    (is (= 1 (count w)))
    (is (re-find #"unknown column option :fild" (first w)))
    (is (re-find #"did you mean :field\?" (first w)))))

(deftest coldef-typo-in-default-col-def
  (let [w (warns-for {:default-col-def {:sortabl true}})]
    (is (= 1 (count w)))
    (is (re-find #"unknown column option :sortabl" (first w)))
    (is (re-find #"did you mean :sortable\?" (first w)))))

(deftest coldef-typo-in-auto-group-column-def
  (let [w (warns-for {:auto-group-column-def {:fild :x}})]
    (is (= 1 (count w)))
    (is (re-find #"unknown column option :fild" (first w)))))

(deftest valid-coldef-keys-silent
  (is (empty? (warns-for {:column-defs [{:field :name :sortable true :cell-renderer (fn [_])}]
                          :default-col-def {:resizable true}}))))

;; --- column groups ----------------------------------------------------------

(deftest column-group-validated-against-group-block
  (testing "group keys (:children, :marry-children) are valid; children recurse"
    (is (empty? (warns-for {:column-defs [{:header-name "Grp"
                                           :marry-children true
                                           :children [{:field :a}]}]}))))
  (testing "typo on a group key warns as a column group option"
    (let [w (warns-for {:column-defs [{:children [{:field :a}] :marry-childrn true}]})]
      (is (= 1 (count w)))
      (is (re-find #"unknown column group option :marry-childrn" (first w)))))
  (testing "typo inside a group child warns as a column option"
    (let [w (warns-for {:column-defs [{:children [{:fild :a}]}]})]
      (is (= 1 (count w)))
      (is (re-find #"unknown column option :fild" (first w))))))

;; --- opaque positions never validated ---------------------------------------

(deftest opaque-positions-never-warn
  (testing "cell-renderer-params, context, and arbitrary nested maps are opaque"
    (is (empty? (warns-for
                 {:column-defs [{:field :name
                                 :cell-renderer-params {:not-a-real-key 1 :another-bogus 2}}]
                  :context {:my-bogus-app-key 1}
                  :default-col-def {:cell-renderer-params {:garbage-key 9}}}))))
  (testing "a raw-wrapped column-defs subtree is opaque"
    (is (empty? (warns-for {:column-defs (c/raw [{:whatever-typo 1}])})))))

;; --- deprecation ------------------------------------------------------------

(deftest deprecation-carries-replacement
  (testing "grid option"
    (let [w (warns-for {:enable-range-selection true})]
      (is (= 1 (count w)))
      (is (re-find #":enable-range-selection is deprecated" (first w)))
      (is (re-find #"cellSelection" (first w)) "replacement note included")))
  (testing "col-def option"
    (let [w (warns-for {:column-defs [{:checkbox-selection true}]})]
      (is (= 1 (count w)))
      (is (re-find #":checkbox-selection is deprecated" (first w)))
      (is (re-find #"selection API" (first w))))))

;; --- dedup ------------------------------------------------------------------

(deftest dedup-per-object-name-and-key
  (testing "same typo key in two column defs warns once"
    (let [w (warns-for {:column-defs [{:fild 1} {:fild 2}]})]
      (is (= 1 (count w)))))
  (testing "same kebab key at grid vs column scope warns once each"
    ;; :childrn is unknown at both grid level and (were it there) column level;
    ;; use a key meaningful to both dedup buckets via a shared typo.
    (let [w (warns-for {:min-widt 1                              ;; grid typo
                        :default-col-def {:min-widt 1}})]        ;; column typo, same kebab
      (is (= 2 (count w)) "distinct [object-name key] buckets warn separately")
      (is (some #(re-find #"grid option :min-widt" %) w))
      (is (some #(re-find #"column option :min-widt" %) w)))))

;; --- never rejects or alters ------------------------------------------------

(deftest validation-never-alters-conversion
  (let [opts {:row-data (clj->js [{"id" 1}])
              :column-defs [{:field :name :fild :typo}]
              :default-col-def {:sortable true}
              :enable-range-selection true}
        expected (js/JSON.stringify (c/->js opts))]
    ;; run validation (which warns), then confirm ->js output is deep-identical
    (warns-for opts)
    (is (= expected (js/JSON.stringify (c/->js opts)))
        "converted output is byte-identical before and after validation runs"))
  (testing "validate-options! returns nil and mutates nothing"
    (let [opts {:column-defs [{:field :a}]}]
      (is (nil? (v/validate-options! opts))))))

;; --- string keys / namespaced keys skipped ----------------------------------

(deftest literal-keys-skipped
  (testing "string keys are user-literal, never validated"
    (is (empty? (warns-for {"someArbitraryString" 1}))))
  (testing "namespaced keywords are skipped"
    (is (empty? (warns-for {:my.ns/whatever 1})))))

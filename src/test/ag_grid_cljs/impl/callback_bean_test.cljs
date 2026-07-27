(ns ag-grid-cljs.impl.callback-bean-test
  "Contract tests for the callback-bean literal-key fallback (ADR 0018,
  ticket agd-01kygja77mxj): a keyword resolves to its camelized property when
  present, otherwise to its literal name — object-local, camel priority."
  (:require [cljs.test :refer [deftest is testing]]
            [ag-grid-cljs.impl.bean :as bean]
            [ag-grid-cljs.impl.convert :as c]))

(defn- wrapped
  "The auto-wrapped form of f, as the options converter produces it."
  [f]
  (unchecked-get (c/->js {:f f}) "f"))

(deftest literal-lookup
  (let [read (wrapped (fn [p] (:first-name (:data p))))]
    (is (= "Ada" (read #js {:data #js {"first-name" "Ada"}}))
        "(:first-name (:data p)) reads the literal \"first-name\" property")))

(deftest camel-priority
  (let [read (wrapped (fn [p] (:first-name (:data p))))]
    (is (= "Ada" (read #js {:data #js {:firstName "Ada"}}))
        "camel lookup remains compatible")
    (is (= "camel" (read #js {:data #js {:firstName "camel" "first-name" "literal"}}))
        "camel wins when both spellings are present")
    (is (false? (read #js {:data #js {:firstName false "first-name" "literal"}}))
        "a present false camel value still wins")
    (is (nil? (read #js {:data #js {:firstName nil "first-name" "literal"}}))
        "a present nil camel value still wins")
    (is (nil? (read #js {:data #js {:firstName js/undefined "first-name" "literal"}}))
        "a present undefined camel value still wins")
    (let [read-vo (wrapped (fn [p] (:value-of (:data p))))]
      (is (= "x" (read-vo #js {:data #js {"value-of" "x"}}))
          "presence is own-property: Object.prototype.valueOf does not shadow a literal key"))))

(deftest fallback-reaches-nested-objects-and-array-elements
  (let [seen (atom nil)
        w    (wrapped (fn [p]
                        (reset! seen [(-> p :data :address :street-name)
                                      (mapv :item-name (:items (:data p)))])
                        nil))]
    (w #js {:data #js {:address #js {"street-name" "Main"}
                       :items   #js [#js {"item-name" "kebab"}
                                     #js {:itemName "camel"}]}})
    (is (= ["Main" ["kebab" "camel"]] @seen)
        "dashed keys resolve below an undashed root and inside array elements")))

(deftest heterogeneous-rows-independent-of-order
  (let [read  (wrapped (fn [p] (:first-name (:data p))))
        camel #js {:data #js {:firstName "camel"}}
        kebab #js {:data #js {"first-name" "kebab"}}]
    (is (= ["camel" "kebab"] [(read camel) (read kebab)]))
    (is (= ["kebab" "camel"] [(read kebab) (read camel)])
        "a camel first row cannot strand a later kebab row")))

(deftest ag-grid-vocabulary-keeps-camel-priority
  (testing "params object"
    (let [read (wrapped (fn [p] (:row-index p)))]
      (is (= 3 (read #js {:rowIndex 3})))
      (is (= 3 (read #js {:rowIndex 3 "row-index" 99}))
          "camel stays authoritative when a literal spelling coexists")))
  (testing "node object reached through params"
    (let [seen (atom nil)
          w    (wrapped (fn [p]
                          (reset! seen [(-> p :node :row-index)
                                        (-> p :node :data :first-name)])
                          nil))]
      (w #js {:node #js {:rowIndex 7 :data #js {"first-name" "Ada"}}})
      (is (= [7 "Ada"] @seen)
          "node props read camel; node.data gets the same lookup law"))))

(deftest direct-data-argument-gets-the-same-law
  ;; getDataPath / isRowMaster / getServerSideGroupKey style: the row itself
  ;; is the argument.
  (let [w (wrapped (fn [row] [(:first-name row) (:last-name row)]))]
    (is (= ["Ada" "Lovelace"]
           (vec (w #js {"first-name" "Ada" :lastName "Lovelace"})))
        "a direct row argument mixes literal and camel freely")))

(deftest row-node-argument-data
  ;; isRowSelectable / doesExternalFilterPass style: the node is the argument.
  (let [seen (atom nil)
        w    (wrapped (fn [node] (reset! seen (-> node :data :first-name)) nil))]
    (w #js {:id "0" :data #js {"first-name" "Ada"}})
    (is (= "Ada" @seen) "RowNode.data follows the callback-bean lookup law")))

(deftest lookup-like-operations-follow-the-law
  (let [b (c/params-bean #js {:firstName "camel" "last-name" "kebab"})]
    (is (= "camel" (get b :first-name)))
    (is (= "kebab" (get b :last-name)))
    (is (= "missing" (get b :other-key "missing")) "not-found still honored")
    (is (contains? b :last-name) "contains? sees a literal-only key")
    (is (= [:last-name "kebab"] (find b :last-name)))
    (is (= "kebab" (b :last-name)) "bean invocation follows the law")))

(deftest writes-snapshot-and-returns-camelize
  ;; ADR 0018 §9: callback-bean writes are not AG Grid object mutation, and a
  ;; non-raw return crosses the normal EDN->JS converter (keywords camelize).
  (let [row #js {"first-name" "Ada"}
        p   #js {:data row}
        w   (wrapped (fn [p] (assoc (:data p) :age 36)))
        ret (w p)]
    (is (not (.call js/Object.prototype.hasOwnProperty row "age"))
        "assoc does not mutate the underlying row")
    (is (= 36 (unchecked-get ret "age")))
    (is (= "Ada" (unchecked-get ret "firstName"))
        "a bean-derived keyword key camelizes on return")
    (is (not (.call js/Object.prototype.hasOwnProperty ret "first-name"))
        "the literal spelling does not survive the return conversion"))
  (let [row #js {"first-name" "Ada"}
        p   #js {:data row}
        w   (wrapped (fn [p] (update-in p [:data :first-name] str "!")))]
    (w p)
    (is (= "Ada" (unchecked-get row "first-name"))
        "a nested update does not mutate the underlying row")))

(deftest raw-callbacks-stay-raw
  (let [f      (fn [p] (unchecked-get p "first-name"))
        passed (unchecked-get (c/->js {:f (c/raw f)}) "f")]
    (is (identical? f passed) "(raw f) is emitted untouched — no bean, ever")))

(deftest nested-beans-are-views-over-the-same-object
  ;; ADR 0018 §8: bean identity is not a contract, but the view must be lazy —
  ;; the underlying JS object stays reachable and unconverted.
  (let [row #js {"first-name" "Ada"}
        b   (c/params-bean #js {:data row})]
    (is (identical? row (bean/object (:data b))))))

(ns ag-grid-cljs.impl.convert-test
  "Contract tests for the forward converter (ticket agd-01ky0eck96vn rules)."
  (:require [cljs.test :refer [deftest is testing]]
            [ag-grid-cljs.impl.convert :as c]))

(deftest key-transform
  (is (= "rowData" (c/kebab->camel "row-data")))
  (is (= "sortable" (c/kebab->camel "sortable")))
  (is (= "rowData" (c/kebab->camel "rowData")) "already-camel passes unchanged")
  (is (= "row-index" (c/camel->kebab "rowIndex"))))

(deftest map-keys
  (let [o (c/->js {:row-height 42 "literalKey" 1 :rowData nil})]
    (is (= 42 (unchecked-get o "rowHeight")))
    (is (= 1 (unchecked-get o "literalKey")) "string keys verbatim")
    (testing "nil -> null with key kept"
      (is (.call js/Object.prototype.hasOwnProperty o "rowData"))
      (is (nil? (unchecked-get o "rowData"))))))

(deftest keyword-values
  (is (= "multiRow" (c/->js :multi-row)) "keywords translate in value position")
  (let [o (c/->js {:dom-layout :auto-height})]
    (is (= "autoHeight" (unchecked-get o "domLayout")))))

(deftest type-driven-recursion
  (let [js-obj #js {:untouched true}
        date   (js/Date. 0)
        o      (c/->js {:column-defs [{:field :first-name}]
                        :ctx  js-obj
                        :when date
                        :s    "verbatim-string"})]
    (is (array? (unchecked-get o "columnDefs")))
    (is (= "firstName" (-> (unchecked-get o "columnDefs")
                           (aget 0)
                           (unchecked-get "field"))))
    (is (identical? js-obj (unchecked-get o "ctx")) "JS objects pass untouched")
    (is (identical? date (unchecked-get o "when")) "js/Date passes untouched")
    (is (= "verbatim-string" (unchecked-get o "s")) "strings are verbatim")))

(deftest raw-escape-hatch
  (let [m {:keep-me :kebab-inside}
        o (c/->js {:context (c/raw m)})]
    (is (identical? m (unchecked-get o "context")) "raw emits untouched")))

(deftest sets-pass-through
  (let [s #{1 2 3}
        o (c/->js {:oops s})]
    (is (identical? s (unchecked-get o "oops")))))

(deftest fn-auto-wrapping
  (testing "params arrive as lazy kebab bean, nested access, converted return"
    (let [f (fn [p] {:font-weight (if (pos? (:value p)) :bold :normal)
                     :seen-field (-> p :col-def :field)})
          wrapped (unchecked-get (c/->js {:cell-style f}) "cellStyle")
          ret (wrapped #js {:value 5 :colDef #js {:field "salary"}})]
      (is (fn? wrapped))
      (is (= "bold" (unchecked-get ret "fontWeight")) "return forward-converted")
      (is (= "salary" (unchecked-get ret "seenField")))))
  (testing "(raw f) opts out: raw JS params in, return as-is"
    (let [f (fn [p] (unchecked-get p "value"))
          passed (unchecked-get (c/->js {:value-getter (c/raw f)}) "valueGetter")]
      (is (identical? f passed)))))

(deftest scalar-args-not-beaned
  (let [f (fn [x] x)
        wrapped (unchecked-get (c/->js {:f f}) "f")]
    (is (= 7 (wrapped 7)) "non-object args pass to the fn as-is")))

(deftest renderer-fn-html-string-warning
  ;; decision on agd-01ky0ed8adbf: the bare fn in a *CellRenderer position is
  ;; the vanilla escape hatch (innerHTML semantics) — dev-warn when its string
  ;; return looks like HTML; other fn positions (value-formatter) stay silent.
  (let [warnings   (atom [])
        orig-warn  js/console.warn]
    (set! js/console.warn (fn [& args] (swap! warnings conj (apply str args))))
    (try
      (let [o (c/->js {:cell-renderer   (fn [_] "<b>hi</b>")
                       :value-formatter (fn [_] "a < b")})]
        ((unchecked-get o "cellRenderer") #js {})
        (is (= 1 (count (filter #(re-find #"HTML-looking" %) @warnings)))
            "cellRenderer fn string return with < warns")
        ((unchecked-get o "valueFormatter") #js {})
        (is (= 1 (count (filter #(re-find #"HTML-looking" %) @warnings)))
            "value-formatter string return never warns"))
      (finally
        (set! js/console.warn orig-warn)))))

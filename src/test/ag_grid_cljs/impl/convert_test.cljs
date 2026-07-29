(ns ag-grid-cljs.impl.convert-test
  "Contract tests for the forward converter (ticket agd-01ky0eck96vn rules)."
  (:require [cljs.test :refer [deftest is testing]]
            [ag-grid-cljs.impl.convert :as c]
            [ag-grid-cljs.test-support :refer [capture]]))

(deftest key-transform
  (is (= "rowData" (c/kebab->camel "row-data")))
  (is (= "sortable" (c/kebab->camel "sortable")))
  (is (= "rowData" (c/kebab->camel "rowData")) "already-camel passes unchanged")
  (is (= "row-index" (c/camel->kebab "rowIndex"))))

(deftest key-transform-edges
  (testing "kebab->camel is unchanged by the no-dash fast path"
    (is (= "" (c/kebab->camel "")))
    (is (= "value" (c/kebab->camel "value")))
    (is (= "rowDATA" (c/kebab->camel "row-DATA")) "segment case beyond the first char is kept")
    (is (= "a1" (c/kebab->camel "a-1")) "a digit segment has no upper-case form")
    (is (= "aB" (c/kebab->camel "a--b")) "empty segments contribute nothing")
    (is (= "A" (c/kebab->camel "-a")) "a leading dash upper-cases the first segment")
    (is (= "a" (c/kebab->camel "a-")) "a trailing dash contributes nothing")
    (is (= "" (c/kebab->camel "-"))))
  (testing "camel->kebab is unchanged by the no-upper-case fast path"
    (is (= "" (c/camel->kebab "")))
    (is (= "value" (c/camel->kebab "value")))
    (is (= "first-name" (c/camel->kebab "first-name")) "already-kebab passes unchanged")
    (is (= "abc" (c/camel->kebab "ABC")) "all-caps still lower-cases")
    (is (= "row-index2" (c/camel->kebab "rowIndex2")))
    (is (= "col-id" (c/camel->kebab "colID")) "only the first boundary of a run splits")
    (is (= "value1" (c/camel->kebab "value1")) "no digit-letter boundary")
    (is (= "é" (c/camel->kebab "é")) "non-ASCII lower-case is untouched")
    (is (= "é" (c/camel->kebab "É")) "non-ASCII upper-case still lower-cases")))

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

(deftest raw-compares-by-value
  ;; The rebuild-stability contract (ADR 0021): a value the wrapper manufactures
  ;; must be = to itself given = inputs, so a consumer rebuilding the options map
  ;; per render gets a clean diff out of update-grid!.
  (testing "two Raws wrapping the same fn are ="
    (let [f (fn [_])]
      (is (= (c/raw f) (c/raw f)))))
  (testing "two Raws wrapping equal-but-distinct CLJS maps are ="
    (is (= (c/raw {:tenant "acme"}) (c/raw {:tenant (str "ac" "me")}))))
  (testing "Raws wrapping unequal values are not ="
    (is (not= (c/raw {:tenant "acme"}) (c/raw {:tenant "other"})))
    (is (not= (c/raw (fn [_])) (c/raw (fn [_])))
        "distinct fns are unequal — = degrades to identity for functions"))
  (testing "a Raw is not = to the bare value it wraps"
    (is (not= (c/raw {:a 1}) {:a 1}))
    (is (not= {:a 1} (c/raw {:a 1}))))
  (testing "a tagged Raw is never = to an untagged Raw wrapping the same value"
    (let [m {:a 1}]
      (is (not= (c/->Raw m :row-id) (c/raw m)))
      (is (not= (c/raw m) (c/->Raw m :row-id)))
      (is (not= (c/->Raw m :row-id) (c/->Raw m :renderer)) "the tag participates")
      (is (= (c/->Raw m :row-id) (c/->Raw {:a 1} :row-id)) "same tag, = value"))))

(deftest raw-hashes-without-touching-the-wrapped-value
  (testing "equal Raws hash equally"
    (let [f (fn [_])]
      (is (= (hash (c/raw f)) (hash (c/raw f))))
      (is (= (hash (c/raw {:a 1})) (hash (c/raw {:a 1}))))
      (is (= (hash (c/->Raw {:a 1} :row-id)) (hash (c/->Raw {:a 1} :row-id))))))
  (testing "equal maps carrying equal Raws hash equally"
    ;; hashing a map hashes its values, so this is the path an options map takes
    (is (= (hash {:context (c/raw {:a 1})}) (hash {:context (c/raw {:a 1})}))))
  (testing "hashing a Raw adds no property to the wrapped value"
    ;; ADR 0021 §3: the hash derives from the tag alone precisely so the wrapped
    ;; value never reaches goog/getUid, which would mutate it
    (let [o #js {:a 1}
          f (fn [_])]
      (hash (c/raw o))
      (hash (c/raw f))
      (hash {:context (c/raw o)})
      (is (= ["a"] (vec (js/Object.keys o))))
      (is (= [] (vec (js/Object.keys f)))))))

(deftest deferred-values-construct-at-the-boundary
  ;; ADR 0021 §4: a builder stashes the consumer's input under an internal tag
  ;; and the boundary constructs the real value, so equal inputs give = maps.
  (testing "an untagged Raw still unwraps by a plain field read"
    (let [m {:a 1}]
      (is (identical? m (c/->js (c/raw m))))))
  (testing "an unregistered tag falls back to the same plain unwrap"
    (let [m {:a 1}]
      (is (identical? m (c/->js (c/->Raw m :no-such-tag))))))
  (testing "a registered tag defers construction to conversion time"
    (let [v (c/deferred :row-id :id)]
      (is (not (fn? v)) "the builder stashes the input; it does not construct")
      (is (fn? (c/->js v)) "the boundary constructs"))))

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
  (testing "fixed and variadic callback arities bean every object argument"
    (let [f       (fn [& xs]
                    (mapv #(if (number? %) % (:value %)) xs))
          wrapped (unchecked-get (c/->js {:callback f}) "callback")
          arg     (fn [n] #js {:value n})]
      (is (= [] (js->clj (wrapped))))
      (is (= [1] (js->clj (wrapped (arg 1)))))
      (is (= [1 2] (js->clj (wrapped (arg 1) 2))))
      (is (= [1 2 3] (js->clj (wrapped (arg 1) 2 (arg 3)))))
      (is (= [1 2 3 4] (js->clj (wrapped (arg 1) 2 (arg 3) 4))))))
  (testing "(raw f) opts out: raw JS params in, return as-is"
    (let [f (fn [p] (unchecked-get p "value"))
          passed (unchecked-get (c/->js {:value-getter (c/raw f)}) "valueGetter")]
      (is (identical? f passed)))))

(deftest bean-lookup-past-the-prop-cache-bound
  ;; The bean's key->prop memo is explicitly bounded (agd-01kygjftnhwa): far more
  ;; distinct keys than the bound must still resolve, and must not disturb an
  ;; ordinary lookup that follows them.
  (let [n 1200
        o (reduce (fn [o i]
                    (unchecked-set o (str "key" i "Value") i)
                    o)
                  #js {"rowIndex" 7}
                  (range n))
        b (c/params-bean o)
        read-all #(map (fn [i] (get b (keyword (str "key" i "-value")))) (range n))]
    (is (= (range n) (read-all)))
    (is (= (range n) (read-all)) "a second pass resolves the same way")
    (is (= 7 (:row-index b)) "a literal keyword still resolves after them")))

(deftest scalar-args-not-beaned
  (let [f (fn [x] x)
        wrapped (unchecked-get (c/->js {:f f}) "f")]
    (is (= 7 (wrapped 7)) "non-object args pass to the fn as-is")))

(deftest cljs-collection-warnings-split-rows-from-context
  ;; agd-01kygjg6avt2: the row nudge must not name a bare conversion call as a
  ;; recipe (a recipe is a row/field pairing, so the code lives in the article);
  ;; :context is not row data and keeps raw as its answer.
  (let [w           (capture #(c/->js {:row-data [{:first-name "Ada"}]
                                       :context  {:tenant-id 42}}))
        matching    #(first (filter (fn [line] (re-find % line)) w))
        row-warning (matching #"rowData received")
        ctx-warning (matching #"context received")]
    (is (some? row-warning) "a CLJS row collection warns")
    (is (not (re-find #"clj->js" row-warning))
        "the row nudge points at the recipes instead of naming a call")
    (is (re-find #"Options and conversion" row-warning)
        "the row nudge names where the recipes live")
    (is (some? ctx-warning) "a CLJS context warns separately")
    (is (re-find #"raw" ctx-warning)
        "the context nudge points at raw, its actual answer")
    (is (not (re-find #"JS by contract" ctx-warning))
        "context is not row data")))

(deftest renderer-fn-html-string-warning
  ;; decision on agd-01ky0ed8adbf: the bare fn in a *CellRenderer position is
  ;; the vanilla escape hatch (innerHTML semantics) — dev-warn when its string
  ;; return looks like HTML; other fn positions (value-formatter) stay silent.
  ;; Conversion itself is silent for both keys, so each call gets its own capture.
  (let [o (c/->js {:cell-renderer   (fn [_] "<b>hi</b>")
                   :value-formatter (fn [_] "a < b")})]
    (is (= 1 (count (capture #((unchecked-get o "cellRenderer") #js {}))))
        "cellRenderer fn string return with < warns")
    (is (empty? (capture #((unchecked-get o "valueFormatter") #js {})))
        "value-formatter string return never warns")))

(deftest renderer-html-warning-is-bounded
  ;; agd-01kyqy2y8c7m / ADR 0022 §4. The nudge fires from inside the wrapped
  ;; renderer, so before it had a period it warned on EVERY cell render — the
  ;; live defect this ticket exists for. A grid scrolling 10k rows produced 10k
  ;; identical console lines.
  (let [w (capture
           (fn []
             (let [r (unchecked-get (c/->js {:cell-renderer (fn [_] "<b>hi</b>")}) "cellRenderer")]
               (dotimes [_ 500] (r #js {})))
             ;; The consumer's closure is fresh every render (ADR 0021 leaves those
             ;; to the consumer), so keying the dedup on the fn would have dedup'd
             ;; nothing for exactly the rebuild-per-render shape ADR 0021 supports.
             (dotimes [_ 20]
               (let [r (unchecked-get (c/->js {:cell-renderer (fn [_] "<b>hi</b>")}) "cellRenderer")]
                 (r #js {})))))]
    (is (= 1 (count (filter #(re-find #"HTML-looking" %) w)))
        "500 renders of one renderer plus 20 rebuilds with fresh closures, one warning")))

(deftest cljs-collection-warning-is-per-prop
  ;; ADR 0022 §1/§3: the discriminator is the prop, so rowData and context each
  ;; get their own line but neither repeats across a rebuilt map.
  (let [w     (capture #(dotimes [_ 50]
                          (c/->js {:row-data [{:first-name "Ada"}] :context {:tenant-id 42}})))
        lines #(count (filter (fn [line] (re-find % line)) w))]
    (is (= 1 (lines #"rowData received")) "50 conversions, one row warning")
    (is (= 1 (lines #"context received")) "50 conversions, one context warning")))

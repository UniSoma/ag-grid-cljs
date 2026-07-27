(ns ag-grid-cljs.bench.transforms
  "Warmed node microbenchmarks for key transforms, callback-bean lookup, and
  callback wrapping (tickets agd-01kygjftnhwa, agd-01kygja77mxj, and
  agd-01kyj2jwkdkq).

  Not library code and not on the test path: `bb bench` compiles this build in
  both dev and release and runs each under node. Methodology and recorded
  results live in docs/research/key-transform-benchmarks.md.

  Every measured expression's value is stored on a global sink so :advanced
  cannot eliminate the work being timed."
  (:require [ag-grid-cljs.impl.bean :as bean]
            [ag-grid-cljs.impl.convert :as convert]
            [clojure.string :as str]))

(def ^:private warmup-iterations 50000)
(def ^:private measured-iterations 500000)

(defn- sink! [x]
  (unchecked-set js/globalThis "__benchSink" x))

(defn- run
  "Time `f` over the measured iterations, after a warmup pass, and return
  nanoseconds per call. hrtime.bigint is monotonic and ns-resolution."
  [f]
  (dotimes [_ warmup-iterations] (sink! (f)))
  (let [t0 (js/process.hrtime.bigint)]
    (dotimes [_ measured-iterations] (sink! (f)))
    (/ (js/Number (- (js/process.hrtime.bigint) t0)) measured-iterations)))

(defn- report [label f]
  (println (str (.padEnd (str label) 44 ".")
                " " (.padStart (.toFixed (run f) 1) 8) " ns")))

;; --- pre-fallback callback bean ----------------------------------------------
;; The shipped params-bean as it stood before the ADR 0018 literal-key fallback
;; landed (ticket agd-01kygja77mxj): mechanical kebab->camel lookup, no
;; per-object resolver, no :transform. Kept bench-local so the recorded
;; before/after comes from one run on one machine.

(defn- mechanical-bean [o]
  (bean/bean o
             :prop->key (comp keyword convert/camel->kebab)
             :key->prop convert/lookup-prop
             :recursive true))

;; --- callback-wrapper construction candidates -------------------------------
;; Bench-only evidence for agd-01kyj2jwkdkq: fixed arities shipped; positional
;; construction was rejected because it couples to the vendored deftype's field
;; order. Keep both baselines so future runs can reproduce the decision.

(def ^:private prototype-prop->key (comp keyword convert/camel->kebab))

(defn- prototype-key->prop [o]
  (fn [k]
    (let [literal (name k)]
      (if (== -1 (.indexOf literal "-"))
        literal
        (let [camel (convert/lookup-prop k)]
          (if ^boolean (js/Object.hasOwn o camel) camel literal))))))

(def ^:private prototype-nested-cache (js/WeakMap.))

(declare options-bean)

(defn- prototype-transform [x]
  (when (object? x)
    (or (.get prototype-nested-cache x)
        (let [b (options-bean x)]
          (.set prototype-nested-cache x b)
          b))))

(defn- options-bean [o]
  (bean/bean o
             :prop->key prototype-prop->key
             :key->prop (prototype-key->prop o)
             :recursive true
             :transform prototype-transform))

(defn- positional-bean [o]
  ;; Deliberately couples this bench-only candidate to the vendored deftype's
  ;; generated constructor so we can measure option parsing in isolation.
  (bean/->Bean nil o prototype-prop->key (prototype-key->prop o)
               prototype-transform true nil nil nil))

(defn- bean-arg [bean-fn a]
  (if (object? a) (bean-fn a) a))

(defn- fixed-arity-wrap
  "Prototype of fixed 0–3 arities with the current variadic behavior as its
  fallback. Common AG Grid one-argument callbacks avoid rest/map/apply."
  [bean-fn f]
  (fn
    ([] (convert/->js (f)))
    ([a] (convert/->js (f (bean-arg bean-fn a))))
    ([a b] (convert/->js (f (bean-arg bean-fn a) (bean-arg bean-fn b))))
    ([a b c] (convert/->js (f (bean-arg bean-fn a)
                              (bean-arg bean-fn b)
                              (bean-arg bean-fn c))))
    ([a b c & more]
     (convert/->js
      (apply f (bean-arg bean-fn a)
             (bean-arg bean-fn b)
             (bean-arg bean-fn c)
             (map #(bean-arg bean-fn %) more))))))

(defn- fixed-arity-wrap-no-return-conversion [bean-fn f]
  (fn [a] (f (bean-arg bean-fn a))))

(defn- legacy-wrap [f]
  (fn [& args]
    (convert/->js
     (apply f (map #(bean-arg convert/params-bean %) args)))))

;; --- pre-fast-path baselines ------------------------------------------------
;; The transform bodies as they stood before this ticket, kept here so the
;; recorded before/after comes from one run on one machine.

(defn- baseline-kebab->camel [s]
  (let [[head & tail] (str/split s #"-")]
    (apply str head (map (fn [seg]
                           (if (seq seg)
                             (str (str/upper-case (subs seg 0 1)) (subs seg 1))
                             seg))
                         tail))))

(defn- baseline-camel->kebab [s]
  (str/lower-case (str/replace s #"([a-z0-9])([A-Z])" "$1-$2")))

(defn- baseline-bean [o]
  (bean/bean o
             :prop->key (comp keyword baseline-camel->kebab)
             :key->prop (comp baseline-kebab->camel name)
             :recursive true))

;; --- memoization candidates -------------------------------------------------
;; Measured to answer whether a cache still earns its keep over the fast paths
;; (ticket design step 3). Bench-local: nothing here ships.

(defn- unbounded-memo [f]
  (let [cache (js/Map.)]
    (fn [s]
      (let [hit (.get cache s)]
        (if (undefined? hit)
          (let [v (f s)] (.set cache s v) v)
          hit)))))

(def ^:private memo-kebab->camel (unbounded-memo convert/kebab->camel))
(def ^:private memo-camel->kebab (unbounded-memo convert/camel->kebab))

(defn- memo-bean
  "The rejected alternative: cache every key in both directions, by name and
  unbounded. Written without `comp` so the comparison against the shipped
  keyword-keyed memo measures caching, not closure-chain overhead."
  [o]
  (bean/bean o
             :prop->key (fn [p] (keyword (memo-camel->kebab p)))
             :key->prop (fn [k] (memo-kebab->camel (name k)))
             :recursive true))

;; --- fixtures ---------------------------------------------------------------

(def ^:private camel-row
  #js {:firstName "Ada" :lastName "Lovelace" :rowIndex 3 :id "r1"})

(def ^:private kebab-row
  #js {"first-name" "Ada" "last-name" "Lovelace" "row-index" 3 "id" "r1"})

(defn- params [row]
  #js {:value "Ada" :data row :node #js {:id "r1" :data row}
       :colDef #js {:field "firstName"} :api #js {}})

(def ^:private camel-params (params camel-row))
(def ^:private kebab-params (params kebab-row))

(def ^:private mechanical-live-bean (mechanical-bean camel-params))
(def ^:private shipped-bean (convert/params-bean camel-params))
(def ^:private kebab-shipped-bean (convert/params-bean kebab-params))

(def ^:private read-first-name (fn [p] (:first-name (:data p))))
(def ^:private raw-read-first-name (fn [^js p] (.. p -data -firstName)))
(def ^:private legacy-wrapped-read (legacy-wrap read-first-name))
(def ^:private shipped-wrapped-read
  (unchecked-get (convert/->js {:f read-first-name}) "f"))
(def ^:private fixed-options-wrapped-read
  (fixed-arity-wrap options-bean read-first-name))
(def ^:private fixed-shipped-wrapped-read
  (fixed-arity-wrap convert/params-bean read-first-name))
(def ^:private fixed-positional-wrapped-read
  (fixed-arity-wrap positional-bean read-first-name))
(def ^:private fixed-shipped-no-return-read
  (fixed-arity-wrap-no-return-conversion convert/params-bean read-first-name))
(def ^:private fixed-positional-no-return-read
  (fixed-arity-wrap-no-return-conversion positional-bean read-first-name))

;; --- suites -----------------------------------------------------------------

(defn- standalone-transforms []
  (println "\nstandalone transforms (baseline = pre-fast-path bodies)")
  (report "baseline kebab->camel \"first-name\"" #(baseline-kebab->camel "first-name"))
  (report "baseline kebab->camel \"value\"" #(baseline-kebab->camel "value"))
  (report "baseline camel->kebab \"firstName\"" #(baseline-camel->kebab "firstName"))
  (report "baseline camel->kebab \"value\"" #(baseline-camel->kebab "value"))
  (report "kebab->camel \"first-name\"" #(convert/kebab->camel "first-name"))
  (report "kebab->camel \"value\"" #(convert/kebab->camel "value"))
  (report "kebab->camel \"row-data\" (memoized)" #(memo-kebab->camel "row-data"))
  (report "camel->kebab \"firstName\"" #(convert/camel->kebab "firstName"))
  (report "camel->kebab \"first-name\"" #(convert/camel->kebab "first-name"))
  (report "camel->kebab \"value\"" #(convert/camel->kebab "value"))
  (report "camel->kebab \"firstName\" (memoized)" #(memo-camel->kebab "firstName"))
  (report "lookup-prop :value (memo bypassed)" #(convert/lookup-prop :value))
  (report "lookup-prop :row-index (memo hit)" #(convert/lookup-prop :row-index)))

(defn- flat-lookup []
  (println "\nflat callback-bean lookup (pre-fallback mechanical bean)")
  (report "baseline construct + read :value" #(:value (baseline-bean camel-params)))
  (report "baseline construct + read :row-index" #(:row-index (baseline-bean camel-params)))
  (report "construct + read :value" #(:value (mechanical-bean camel-params)))
  (report "construct + read :row-index"
          #(:row-index (mechanical-bean camel-params)))
  (report "read :value on a live bean" #(:value mechanical-live-bean))
  (report "read :col-def on a live bean" #(:col-def mechanical-live-bean))
  (report "construct + read :value (unbounded memo, both directions)"
          #(:value (memo-bean camel-params)))
  (report "construct + read :row-index (unbounded memo, both directions)"
          #(:row-index (memo-bean camel-params)))
  (report "construct + nested read :first-name of :data"
          #(:first-name (:data (mechanical-bean camel-params)))))

(defn- adr-0018-fallback []
  (println "\nADR 0018 literal-key fallback (shipped params-bean)")
  (report "construct + read :value" #(:value (convert/params-bean camel-params)))
  (report "read :value on a live bean" #(:value shipped-bean))
  (report "read :col-def on a live bean" #(:col-def shipped-bean))
  (report "camel row: nested read :first-name of :data"
          #(:first-name (:data (convert/params-bean camel-params))))
  (report "kebab row: nested read :first-name of :data"
          #(:first-name (:data (convert/params-bean kebab-params))))
  (report "kebab row: nested read via a live bean"
          #(:first-name (:data kebab-shipped-bean))))

(defn- callback-wrapper-candidates []
  (println "\ncallback wrapper candidates (one object arg, scalar return)")
  (report "raw JS property read" #(raw-read-first-name camel-params))
  (report "baseline variadic map/apply + params-bean"
          #(legacy-wrapped-read camel-params))
  (report "shipped fixed arities + params-bean"
          #(shipped-wrapped-read camel-params))
  (report "fixed arity + equivalent options bean"
          #(fixed-options-wrapped-read camel-params))
  (report "bench clone, fixed arity + params-bean"
          #(fixed-shipped-wrapped-read camel-params))
  (report "fixed arity + positional Bean constructor"
          #(fixed-positional-wrapped-read camel-params))
  (report "shipped params-bean, no return ->js"
          #(fixed-shipped-no-return-read camel-params))
  (report "positional constructor, no return ->js"
          #(fixed-positional-no-return-read camel-params)))

(defn ^:export main []
  (println (str "node " js/process.version
                ", " measured-iterations " measured iterations"
                " after " warmup-iterations " warmup"
                ", goog.DEBUG=" ^boolean goog.DEBUG))
  (standalone-transforms)
  (flat-lookup)
  (adr-0018-fallback)
  (callback-wrapper-candidates)
  (println "\nsink:" (pr-str (unchecked-get js/globalThis "__benchSink"))))

(ns ag-grid-cljs.bench.transforms
  "Warmed node microbenchmarks for the key transforms and callback-bean lookup
  (ticket agd-01kygjftnhwa; ADR 0018 §7 asks for these numbers to judge the
  literal-key fallback, shipped by ticket agd-01kygja77mxj).

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

(defn ^:export main []
  (println (str "node " js/process.version
                ", " measured-iterations " measured iterations"
                " after " warmup-iterations " warmup"
                ", goog.DEBUG=" ^boolean goog.DEBUG))
  (standalone-transforms)
  (flat-lookup)
  (adr-0018-fallback)
  (println "\nsink:" (pr-str (unchecked-get js/globalThis "__benchSink"))))

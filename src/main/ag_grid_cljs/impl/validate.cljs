(ns ag-grid-cljs.impl.validate
  "Dev-only, position-aware validation of the EDN options map, run at the
  conversion boundary (ADR 0007 §4-5). It does the strictly-kebab-native layer
  AG Grid's own ValidationModule cannot: unknown-key warnings with a kebab
  did-you-mean, plus kebab-native deprecation warnings carrying the replacement.
  It NEVER reimplements type/dependency/row-model checks (delegated to
  ValidationModule) and it NEVER rejects or alters conversion output — warn-only
  (ADR 0002).

  Position-aware: top-level keys validate against :grid-options (+ event
  handlers); the known ColDef-bearing positions (:column-defs items,
  :default-col-def, :auto-group-column-def, and group :children) validate against
  :col-def / :col-group-def; everything else is opaque and never touched.

  Off by default; ag-grid-cljs.core/enable-dev-validations! flips it on. Every
  registry reference here sits inside ^boolean goog.DEBUG guards and the whole
  namespace is reached only from goog.DEBUG-guarded call sites, so :advanced
  compilation with {goog.DEBUG false} dead-code-eliminates it (ADR 0007 §1)."
  (:require [ag-grid-cljs.impl.convert :as convert]
            [ag-grid-cljs.impl.registry :as reg]))

;; --- dev-only state ---------------------------------------------------------

(defonce ^:private enabled? (atom false))

;; Deduped signatures: #{[object-name kebab-key] ...}. One warning per pair.
(defonce ^:private warned (atom #{}))

(defn enable!
  "Turn the wrapper's dev validations on. No-op in production builds."
  []
  (when ^boolean goog.DEBUG (reset! enabled? true)))

(defn reset-warnings!
  "Clear the dedup set (test helper)."
  []
  (reset! warned #{}))

(defn- warn-once! [object-name k msg]
  (let [sig [object-name k]]
    (when-not (contains? @warned sig)
      (swap! warned conj sig)
      (js/console.warn (str "[ag-grid-cljs] " msg)))))

;; --- did-you-mean -----------------------------------------------------------

(defn- levenshtein [s t]
  (let [n (count t)]
    (loop [i 1
           prev (vec (range (inc n)))
           sc (seq s)]
      (if (nil? sc)
        (peek prev)
        (let [a (first sc)
              cur (reduce
                   (fn [row j]
                     (let [cost (if (= a (nth t (dec j))) 0 1)]
                       (conj row (min (inc (peek row))
                                      (inc (nth prev j))
                                      (+ (nth prev (dec j)) cost)))))
                   [i]
                   (range 1 (inc n)))]
          (recur (inc i) cur (next sc)))))))

(defn- suggest
  "Closest kebab key to `input` (a string) within a length-scaled edit distance,
  or nil. `kebabs` is a seq of candidate keywords."
  [input kebabs]
  (let [thresh (max 2 (quot (count input) 3))]
    (->> kebabs
         (map (fn [kw] [kw (levenshtein input (name kw))]))
         (filter (fn [[_ d]] (<= d thresh)))
         (sort-by second)
         ffirst)))

;; --- per-position known-key indexes (dev-only literals; DCE in prod) ---------
;; Each position is a spec {:camels <set> :deprs <camel->note> :kebabs <keys>}:
;; :camels is the membership test (camel-normalized so kebab and already-camel
;; input match identically), :deprs the deprecation notes, :kebabs the
;; did-you-mean candidate pool.

(defn- block-camels [block]
  (into #{} (map (comp :camel val)) block))

(defn- block-deprecations [block]
  (into {} (keep (fn [[_ e]] (when (:deprecated e) [(:camel e) (:deprecated e)]))) block))

(defn- block-spec [block]
  {:camels (block-camels block)
   :deprs  (block-deprecations block)
   :kebabs (vec (keys block))})

(def ^:private grid-spec
  ;; Grid options also accept event-handler keys (:on-cell-clicked), whose valid
  ;; camel form is the registry's :handler ("onCellClicked") and whose
  ;; did-you-mean candidate is that handler's kebab form.
  (when ^boolean goog.DEBUG
    (let [go (:grid-options reg/registry)
          ev (:events reg/registry)
          handler-camels (map (comp :handler val) ev)
          handler-kebabs (map (comp keyword convert/camel->kebab :handler val) ev)]
      {:camels (into (block-camels go) handler-camels)
       :deprs  (block-deprecations go)
       :kebabs (into (vec (keys go)) handler-kebabs)})))

(def ^:private col-spec
  (when ^boolean goog.DEBUG (block-spec (:col-def reg/registry))))

(def ^:private col-group-spec
  (when ^boolean goog.DEBUG (block-spec (:col-group-def reg/registry))))

;; --- key checks -------------------------------------------------------------

(defn- check-key!
  "Warn on one unknown or deprecated key. `object-name` labels the position
  (and scopes dedup). String keys and namespaced keywords are user-literal and
  skipped (conversion rule: string = verbatim)."
  [object-name {:keys [camels deprs kebabs]} k]
  (when (and (keyword? k) (nil? (namespace k)))
    (let [prop (convert/kebab->camel (name k))]
      (if (contains? camels prop)
        (when-let [dep (get deprs prop)]
          (warn-once! object-name k
                      (str object-name " option " k " is deprecated: " dep)))
        (let [sug (suggest (name k) kebabs)]
          (warn-once! object-name k
                      (str "unknown " object-name " option " k
                           (when sug (str " — did you mean " sug "?")))))))))

(defn- check-map! [object-name spec m]
  (doseq [k (keys m)] (check-key! object-name spec k)))

(declare validate-col-defs)

(defn- validate-col-item [item]
  (when (map? item)
    (if (contains? item :children)
      (do (check-map! "column group" col-group-spec item)
          (validate-col-defs (:children item)))
      (check-map! "column" col-spec item))))

(defn- validate-col-defs [xs]
  (when (sequential? xs)
    (doseq [item xs] (validate-col-item item))))

(defn- validate-col-def [m]
  (when (map? m) (check-map! "column" col-spec m)))

(defn validate-options!
  "Position-aware dev validation of the top-level EDN options map. No-op unless
  dev validations are enabled and this is a goog.DEBUG build. Never rejects or
  alters — it only emits js/console.warn."
  [opts]
  (when (and ^boolean goog.DEBUG @enabled? (map? opts))
    (check-map! "grid" grid-spec opts)
    (validate-col-defs (:column-defs opts))
    (validate-col-def (:default-col-def opts))
    (validate-col-def (:auto-group-column-def opts))))

(ns ag-grid-cljs.impl.validate
  "Three dev-only diagnostics, only the first gated (ADR 0017, ADR 0019). All
  are warn-only: none ever rejects or alters what AG Grid receives (ADR 0002).

  DEV VALIDATIONS — position-aware validation of the EDN options map, run at the
  conversion boundary (ADR 0007 §4-5). It does the strictly-kebab-native layer
  AG Grid's own ValidationModule cannot: unknown-key warnings with a kebab
  did-you-mean, plus kebab-native deprecation warnings carrying the replacement.
  It NEVER reimplements type/dependency/row-model checks (delegated to
  ValidationModule). Position-aware: top-level keys validate against
  :grid-options (+ event handlers); the known ColDef-bearing positions
  (:column-defs items, :default-col-def, :auto-group-column-def, and group
  :children) validate against :col-def / :col-group-def; everything else is
  opaque and never touched. OFF by default — ag-grid-cljs.core/
  enable-dev-validations! flips it on, because these checks test consumer keys
  against a registry pinned to one AG Grid version and so drift.

  CLASS-RULE KEYS — check-class-rules!, which warns when a :row-class-rules or
  :cell-class-rules key is a keyword conversion would rename, since those keys
  are CSS class names the consumer coins and the stylesheet matches literally
  (ADR 0019). ALWAYS ON, for the same reason as the field check: registry-free.

  FIELD CHECK — install-field-check!, which compares each column's emitted field
  string against the keys of one sampled row and warns once per field with a
  did-you-mean. ALWAYS ON: it is registry-free (it compares two consumer-supplied
  things to each other), so the drift argument behind the gate does not apply and
  enable-dev-validations! does not cover it.

  Every registry reference here sits inside ^boolean goog.DEBUG guards and the
  whole namespace is reached only from goog.DEBUG-guarded call sites, so
  :advanced compilation with {goog.DEBUG false} dead-code-eliminates it
  (ADR 0007 §1)."
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

(defn disable!
  "Turn the dev validations back off (test helper). enable! has no other
  counterpart, and the always-on checks are only observable with the gate down."
  []
  (reset! enabled? false))

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

(defn- closest
  "Closest of `candidates` (strings) to `input` within a length-scaled edit
  distance, or nil."
  [input candidates]
  (let [thresh (max 2 (quot (count input) 3))]
    (->> candidates
         (map (fn [c] [c (levenshtein input c)]))
         (filter (fn [[_ d]] (<= d thresh)))
         (sort-by second)
         ffirst)))

(defn- suggest
  "Closest kebab key to `input` (a string) within a length-scaled edit distance,
  or nil. `kebabs` is a seq of candidate keywords."
  [input kebabs]
  (some-> (closest input (map name kebabs)) keyword))

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

;; --- class-rule keys (always-on, ADR 0019) ----------------------------------

(defn- renamed-key?
  "Would conversion change what AG Grid receives for this key? A dashed name
  camelizes; a namespace is dropped. Anything else — a string, a dashless
  keyword — arrives spelled exactly as written."
  [k]
  (and (keyword? k)
       (or (some? (namespace k))
           (not (== -1 (.indexOf ^string (name k) "-"))))))

(defn- check-class-rule-keys!
  "Warn once per renamed key in one class-rules map. `option` is the owning
  option keyword, which also scopes dedup."
  [option m]
  (when (map? m)
    (doseq [k (keys m) :when (renamed-key? k)]
      (warn-once! option k
                  (str option " key " k " emits the CSS class \""
                       (convert/kebab->camel (name k))
                       "\" — CSS class names are strings, not AG Grid"
                       " vocabulary. Write \"" (name k) "\".")))))

(declare check-col-defs!)

(defn- check-col-item! [item]
  (when (map? item)
    (check-class-rule-keys! :cell-class-rules (:cell-class-rules item))
    ;; One branch covers leaves and groups: a leaf's :children is nil, and a
    ;; group carries no :cell-class-rules so its own lookup just misses.
    (check-col-defs! (:children item))))

(defn- check-col-defs! [xs]
  (when (sequential? xs)
    (doseq [item xs] (check-col-item! item))))

(defn check-class-rules!
  "Always-on check over the consumer-keyed class-rule options: a keyword key
  that conversion would rename emits a CSS class the stylesheet cannot match,
  and fails silently (ADR 0019). Registry-free, so no enable-dev-validations!
  gate — unlike the four consumer-keyed options whose names are cited from
  inside the options map, where a keyword key is correct and a keyword-key
  warning would be a false positive (ADR 0019 §3).

  Called with the full options map at creation and with the PATCH on update:
  update-grid! is a merge differ, so anything already applied was checked when
  it arrived."
  [opts]
  (when (and ^boolean goog.DEBUG (map? opts))
    (check-class-rule-keys! :row-class-rules (:row-class-rules opts))
    (check-col-defs! (:column-defs opts))
    (check-col-item! (:default-col-def opts))
    (check-col-item! (:auto-group-column-def opts))))

;; --- field check (always-on, ADR 0017) --------------------------------------
;; Reads the emitted JS off the live grid rather than the EDN options map: rows
;; leave the options map at creation (ADR 0004), so warnings name camel strings —
;; which is what AG Grid is looking up and failing to find.

(defn- js-object?
  "True when `x` is safe on the right of the `in` operator — a plain object, a
  class instance or a null-prototype object, but not a primitive (`in` throws on
  those, and a diagnostic must never crash the grid)."
  [x]
  (identical? "object" (goog/typeOf x)))

(defn- lookup-key
  "The key AG Grid actually reads out of the row for `field`. Only the first dot
  segment: nested objects are legitimately sparse, so walking deeper would trade
  a rare catch for a common false positive."
  [field dots?]
  (if dots? (first (.split field ".")) field))

(defn field-targets
  "The checkable targets of one AG Grid `Column` — a vector of
  `{:kind :field|:tooltip-field :field <emitted string> :row-key <key read>}`
  (public for the node suite).

  Skipping is per field, not per ColDef: a `valueGetter` supersedes `field` and
  drops it, but nothing drops `tooltipField` — v36's cell tooltip resolver reads
  `data[tooltipField]` before consulting `tooltipValueGetter`. Dot notation is
  whatever the Column already resolved, so `:suppress-field-dot-notation` is
  honoured for free."
  [^js col]
  (let [d (.getColDef col)
        f (.-field d)
        t (.-tooltipField d)]
    (cond-> []
      (and (string? f) (seq f) (nil? (.-valueGetter d)))
      (conj {:kind    :field
             :field   f
             :row-key (lookup-key f (.isFieldContainsDots col))})

      (and (string? t) (seq t))
      (conj {:kind    :tooltip-field
             :field   t
             :row-key (lookup-key t (.isTooltipFieldContainsDots col))}))))

(defn- unresolved?
  "Has no verdict yet been reached for this target's field string?"
  [state {:keys [field]}]
  (not (contains? @state field)))

(defn first-row
  "The first loaded leaf data row, or nil (public for the node suite). Group rows
  are skipped: a CSRM group node carries no data at all, and an SSRM group row
  carries only its grouping field — sampling one would read as every other field
  being absent."
  [^js api]
  (let [v (volatile! nil)]
    (.forEachNode api (fn [^js node]
                        (when (and (nil? @v)
                                   (not (.-group node))
                                   (some? (.-data node)))
                          (vreset! v (.-data node)))))
    @v))

(defn check-fields!
  "Warn once for each of `targets` whose `:row-key` is absent from `row` (public
  for the node suite). `state` is an atom holding the set of resolved field
  strings, where resolved means a verdict was reached — warned or found present;
  the warning fires on the transition into the set. A non-object `row` (including
  nil: no rows loaded yet) resolves nothing and warns nothing.

  Presence is deliberately asymmetric with the suggestion pool: `in` walks the
  prototype chain, so class instances with prototype getters stay quiet, while
  `js-keys` does not, so \"toString\" is never suggested."
  [state targets row]
  (when (js-object? row)
    (doseq [{:keys [kind field row-key] :as target} targets
            :when (unresolved? state target)]
      (swap! state conj field)
      (when-not (js-in row-key row)
        (js/console.warn
         (str "[ag-grid-cljs] column "
              (if (= :tooltip-field kind) "tooltip field" "field") " "
              (pr-str field) " is not a key in the row data"
              (when-let [sug (closest row-key (js-keys row))]
                (str " — did you mean " (pr-str sug) "?"))))))))

(defn- run-field-check!
  "One pass over the live grid. `getColumns` returns null until colModel.ready;
  the short-circuit on already-resolved fields keeps the steady state a
  set-membership test rather than a full forEachNode traversal, which matters
  because newColumnsLoaded also fires on sort and resize."
  [^js api state]
  (when-let [cols (.getColumns api)]
    (let [targets (into [] (mapcat field-targets) cols)]
      (when (some #(unresolved? state %) targets)
        (check-fields! state targets (first-row api))))))

(defn install-field-check!
  "Install the field check on a live grid: register `modelUpdated` (data arriving
  by any route) and `newColumnsLoaded` (columnDefs replaced), then run the check
  once. The immediate run is load-bearing, not belt-and-braces — addEventListener
  is only reachable after createGrid returns, by which point both events have
  already fired for the initial columns and rows, so a grid that is never
  subsequently modified would otherwise never be checked.

  State is per-grid, held in the listener closure: the \"present in this grid's
  rows\" half is inherently per-grid, and the module-global dedup set would both
  silence a real bug on a second grid with differently-shaped rows and survive
  hot reload. Not gated by enable-dev-validations! (ADR 0017)."
  [^js api]
  (when ^boolean goog.DEBUG
    (let [state  (atom #{})
          check! (fn [_] (run-field-check! api state))]
      (.addEventListener api "modelUpdated" check!)
      (.addEventListener api "newColumnsLoaded" check!)
      (check! nil))))

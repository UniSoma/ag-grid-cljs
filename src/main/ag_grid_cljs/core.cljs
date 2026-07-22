(ns ag-grid-cljs.core
  "Public API — walking-skeleton first cut. Namespace layout and naming
  are provisional until the namespace-layout decision lands."
  (:require [ag-grid-cljs.impl.convert :as convert]
            [ag-grid-cljs.impl.validate :as validate]
            [ag-grid-cljs.impl.registry :as reg]
            ["ag-grid-community" :refer [createGrid ModuleRegistry]]))

(def raw
  "Escape hatch: (raw x) passes x to AG Grid untouched — no key renaming,
  no recursion, no function wrapping."
  convert/raw)

(defn register!
  "Thin optional sugar over ModuleRegistry.registerModules. The consumer
  owns module registration; must run before the first grid is created."
  [& modules]
  (.registerModules ModuleRegistry (into-array modules)))

;; --- builders (first cut) ---------------------------------------------------
;; Builders are plain-map sugar: the bottom layer is always an EDN options
;; map, fully reachable via ordinary assoc.

(defn options
  "Start an options map, optionally from an existing EDN map."
  ([] {})
  ([base] (or base {})))

(defn with-columns [opts col-defs]
  (assoc opts :column-defs (vec col-defs)))

(defn with-default-col-def [opts col-def]
  (assoc opts :default-col-def col-def))

(defn with-row-data
  "Rows are JS by contract: pass a JS array of JS objects.
  (A JS array passes the converter untouched; CLJS rows trigger the
  dev-mode JS-by-contract warning.)"
  [opts rows]
  (assoc opts :row-data rows))

(defn with-cell-selection
  "Enable cell (range) selection — Enterprise. Pass true for defaults, or the
  v32.2+ cellSelection object form for finer control, e.g.
  {:handle {:mode \"fill\"}} to turn on the fill handle. Requires an Enterprise
  module bundle registered (CellSelectionModule) and a license."
  [opts cell-selection]
  (assoc opts :cell-selection cell-selection))

;; --- mount ------------------------------------------------------------------

(defrecord GridHandle [api opts warned])

(defn create-grid!
  "Convert the EDN options map and mount AG Grid on `el`, returning a GridHandle
  — `{:api <GridApi> :opts <last-applied-EDN>}` — that every runtime channel
  takes (ADR 0008). `el` is a DOM element; `opts` is the EDN options map (build
  it with the `with-*` builders or plain `assoc`). The map crosses the
  conversion boundary (kebab->camel, ADR 0005) before it reaches AG Grid.

  The stashed `:opts` is the unconverted EDN, so later `update-grid!` diffs
  compare EDN to EDN; reach the raw GridApi with `grid-api` for anything the
  wrapper does not cover. (The handle also carries an internal per-handle set of
  keys already dev-warned by `update-grid!` — not part of the public shape.)

  Example:

      (create-grid! el (-> (options)
                           (with-columns [{:field :name}])
                           (with-row-data rows)))"
  [el opts]
  ;; goog.DEBUG guard here (not only inside validate-options!) is load-bearing:
  ;; it keeps validate the sole caller goog.DEBUG-gated so :advanced DCEs the
  ;; whole namespace + registry in production (ADR 0007 §1). Don't "simplify".
  (when ^boolean goog.DEBUG (validate/validate-options! opts))
  (->GridHandle (createGrid el (convert/->js opts)) opts (atom #{})))

(defn enable-dev-validations!
  "Turn on the wrapper's dev-mode option validation: unknown top-level and
  ColDef keys warn once with a kebab did-you-mean, and deprecated keys warn with
  their replacement (ADR 0007 §4-5). Warn-only — validation never rejects or
  alters what AG Grid receives. No-op in production builds (goog.DEBUG false),
  where both the validation code and the key registry are dead-code-eliminated.

  Call once at app startup in dev. This covers only the kebab-native layer; for
  type, option-dependency, and row-model checks register AG Grid's own
  ValidationModule (dev bundle) alongside it:

      (:require [\"ag-grid-community\" :refer [ValidationModule]])
      (ag/register! ValidationModule)
      (ag/enable-dev-validations!)"
  []
  (when ^boolean goog.DEBUG (validate/enable!)))

(defn grid-api
  "Return the raw AG Grid GridApi held by `handle` — the escape hatch
  (ADR 0004) for any imperative capability the wrapper does not wrap.

  Example: `(.getDisplayedRowCount (grid-api handle))`."
  [handle]
  (:api handle))

(defn destroy!
  "Tear down the grid behind `handle` (releases its DOM and listeners).
  Takes a GridHandle from `create-grid!`.

  Example: `(destroy! handle)`."
  [handle]
  (.destroy ^js (:api handle)))

;; --- explicit data channel (first cut, ticket agd-01ky0ed8766f) --------------

(defn set-rows!
  "Replace the full row set on the grid behind `handle`. Rows are JS by
  contract: a JS array of JS objects (ADR 0003). With `:get-row-id` set, AG Grid
  diffs by id and preserves grid state (scroll/selection/focus) across the swap.
  Writes AG Grid's `rowData` grid option.

  Example: `(set-rows! handle (into-array (map person->row people)))`."
  [handle rows]
  (.setGridOption ^js (:api handle) "rowData" rows))

(defn transact!
  "Apply a fine-grained row transaction to the grid behind `handle`:
  `{:add [...] :update [...] :remove [...] :add-index n}`. Rows are JS by
  contract (ADR 0003); `:update`/`:remove` match existing rows via
  `:get-row-id`. The tx map is forward-converted (ADR 0005) and handed to AG
  Grid's `applyTransaction`; its RowNodeTransaction result is returned untouched.

  Example: `(transact! handle {:update [(person->row p)]})`."
  [handle tx]
  (.applyTransaction ^js (:api handle) (convert/->js tx)))

;; --- declarative update channel (ADR 0008) ----------------------------------

(defn- warn-once!
  "Dev-only, deduped console warning. `warned` is the handle's atom holding the
  set of keys already warned; each key warns at most once per handle. No-op in
  production builds (goog.DEBUG false), so `warned` never grows there."
  [warned k msg]
  (when ^boolean goog.DEBUG
    (when-not (contains? @warned k)
      (swap! warned conj k)
      (js/console.warn (str "[ag-grid-cljs] " msg)))))

(defn- apply-opt!
  "Push one changed grid option onto `api` via setGridOption. The value is
  converted the SAME way create-grid! converts it — through map->js, not a bare
  ->js — so a renderer grid option keeps its innerHTML/XSS dev-warn and
  data-carrying keys keep their JS-by-contract nudge (ADR 0005). Convert the
  singleton map and read the one prop back out."
  [api k v]
  (let [prop (convert/kebab->camel (name k))]
    (.setGridOption ^js api prop (unchecked-get (convert/->js {k v}) prop))))

(defn- classify
  "Classify a top-level grid-option key as :updatable, :initial-only, or
  :unclassified, using the registry's :initial? flag as the sole classifier
  (ADR 0007/0008). The registry is dev-only and dead-code-eliminated in
  production, so this reference sits inside a goog.DEBUG branch and every key
  classifies :unclassified in production — where it applies optimistically and
  AG Grid ignores any option it treats as initial-only."
  [k]
  (if ^boolean goog.DEBUG
    (if-let [entry (get-in reg/registry [:grid-options k])]
      (if (:initial? entry) :initial-only :updatable)
      :unclassified)
    :unclassified))

(defn update-grid!
  "PATCH the grid behind `handle` with `new-opts`, an EDN options map holding
  only the keys to change (ADR 0008). A MERGE differ, not full-state: keys
  ABSENT from `new-opts` are left as-is; present keys are compared by `=` against
  the handle's stashed last-applied EDN and only CHANGED keys do anything.
  Returns the handle with its stash merged forward, so successive updates diff
  against the true applied state — thread the returned handle.

  Per changed key (registry :initial? is the sole updatable-vs-initial-only
  classifier):

  - updatable      -> one `setGridOption` (key camelized, value forward-converted
                      via the conversion boundary, ADR 0005). `:column-defs` is
                      an ordinary updatable key: the WHOLE new value ships and AG
                      Grid owns column-level diffing (pin `:col-id` to preserve
                      column state).
  - initial-only   -> ignored with a once-per-key dev warning (an initial-only
                      option cannot change after creation; recreating the grid
                      would destroy scroll/selection/focus state).
  - `:row-data`    -> ignored with a dev warning; the data channel owns it (use
                      `set-rows!` / `transact!`, ADR 0004).
  - unclassified   -> applied optimistically via `setGridOption` with a
                      once-per-key dev warning (newer than the registry pin, or a
                      typo already flagged by conversion-time validation).

  All dev warnings are no-ops in production builds (goog.DEBUG false).

  Example:

      (-> handle
          (update-grid! {:pagination true})
          (update-grid! {:quick-filter-text \"ada\"}))"
  [handle new-opts]
  (let [{:keys [api opts warned]} handle]
    (doseq [k (keys new-opts)]
      (let [new-val (get new-opts k)]
        (when (not= (get opts k) new-val)
          (if (= k :row-data)
            (warn-once! warned k
                        (str ":row-data is owned by the data channel and ignored "
                             "by update-grid!; use set-rows! or transact! (ADR 0004)"))
            (case (classify k)
              :updatable    (apply-opt! api k new-val)
              :initial-only (warn-once! warned k
                                        (str "grid option " k " is initial-only and cannot change "
                                             "after creation; update-grid! ignored it"))
              :unclassified (do (apply-opt! api k new-val)
                                (warn-once! warned k
                                            (str "grid option " k " is not in the key registry; "
                                                 "update-grid! applied it optimistically"))))))))
    (assoc handle :opts (merge opts new-opts))))

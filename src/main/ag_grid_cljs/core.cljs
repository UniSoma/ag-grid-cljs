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

(defn with-row-data
  "Rows are JS by contract: pass a JS array of JS objects.
  (A JS array passes the converter untouched; CLJS rows trigger the
  dev-mode JS-by-contract warning.)"
  [opts rows]
  (assoc opts :row-data rows))

(defn with-row-id
  "Coercion: assoc `:get-row-id`, the row-identity callback AG Grid uses to
  diff row data. Give it a keyword naming an id field, or a function computing
  an id from the row; either way the result is string-coerced, since AG Grid
  requires `getRowId` to return a string.

  Load-bearing: with a stable row id, `set-rows!` and `transact!` diff by id
  and preserve grid state (scroll/selection/focus) across updates (ADR 0008).

  EDN shape:

  - a keyword — `(with-row-id opts :id)` reads that field from each JS row
    (`:first-name` reads `firstName`, kebab->camel like every other key) and
    calls `str` on it. Compiles to a `raw` callback over the JS row, so the
    per-row hot path allocates no bean.
  - a function — `(with-row-id opts (fn [p] (:id (:data p))))` receives the
    kebab-bean params (ADR 0010; `(:data p)` is the row) and its return is
    string-coerced. Wrap your own fn with `raw` to receive raw JS params.

  Writes AG Grid's `getRowId` grid option (initial-only). Community; all
  supported versions.

  Example:

      (-> (options)
          (with-columns [{:field :id} {:field :name}])
          (with-row-id :id))"
  [opts id]
  (assoc opts :get-row-id
         (if (keyword? id)
           (let [prop (convert/kebab->camel (name id))]
             (raw (fn [^js params] (str (unchecked-get (.-data params) prop)))))
           (fn [params] (str (id params))))))

(defn- ->row-selection-mode
  "Coerce a friendly selection mode to the v32.2 rowSelection.mode string:
  :single/:multiple -> \"singleRow\"/\"multiRow\". The deprecated pre-v32.2
  strings \"single\"/\"multiple\" are upgraded the same way (churn-shielding);
  the v32.2 strings \"singleRow\"/\"multiRow\" and anything else pass through."
  [mode]
  (case mode
    (:single "single" "singleRow")    "singleRow"
    (:multiple "multiple" "multiRow") "multiRow"
    mode))

(defn with-selection
  "Option-bundle: assoc `:row-selection` as the v32.2+ `rowSelection` OBJECT,
  shielding callers from the pre-v32.2 string form (`\"single\"`/`\"multiple\"`)
  and its scattered deprecated flags (`suppressRowClickSelection`, etc.).

  Coerces the friendly `:mode` value — `:single`/`:multiple` (or the raw
  `\"singleRow\"`/`\"multiRow\"`) — and passes the remaining friendly keys
  through the conversion boundary unchanged (`:header-checkbox` ->
  `headerCheckbox`, etc.).

  EDN shape (all keys optional; `:mode` defaults to `:multiple`):

      {:mode                   :single | :multiple
       :checkboxes             true | false
       :header-checkbox        true | false     ; multiRow only
       :enable-click-selection true | false}

  Writes AG Grid's `rowSelection` grid option (an object). Community; requires
  AG Grid v32.2+ (the object form of `rowSelection`).

  Example:

      (-> (options)
          (with-columns [{:field :name}])
          (with-selection {:mode :multiple :header-checkbox true}))"
  [opts selection]
  (assoc opts :row-selection
         (assoc selection :mode (->row-selection-mode (:mode selection :multiple)))))

(defn with-pagination
  "Option-bundle: turn on pagination and configure page sizing, encoding the
  `paginationAutoPageSize` x `paginationPageSize` mutual exclusion (AG Grid
  ignores a fixed page size when auto-sizing is on).

  EDN shape (config optional; `(with-pagination opts)` just enables it):

      {:page-size          25            ; rows per page (ignored if :auto-page-size)
       :page-size-selector [25 50 100]   ; or true/false to show/hide the selector
       :auto-page-size     true}         ; size each page to fill the viewport

  When `:auto-page-size` is true, `:page-size` is dropped (dev-warns if both
  were given). Writes `pagination`, plus `paginationPageSize`,
  `paginationPageSizeSelector`, `paginationAutoPageSize` as supplied. Community;
  all supported versions.

  Example:

      (-> (options)
          (with-columns [{:field :name}])
          (with-pagination {:page-size 25 :page-size-selector [25 50 100]}))"
  ([opts] (with-pagination opts {}))
  ([opts {:keys [page-size page-size-selector auto-page-size]}]
   (when (and ^boolean goog.DEBUG auto-page-size (some? page-size))
     (js/console.warn
      (str "[ag-grid-cljs] with-pagination: :auto-page-size and :page-size are "
           "mutually exclusive; :page-size dropped (AG Grid auto-sizes each page).")))
   (cond-> (assoc opts :pagination true)
     auto-page-size                       (assoc :pagination-auto-page-size true)
     (and page-size (not auto-page-size)) (assoc :pagination-page-size page-size)
     (some? page-size-selector)           (assoc :pagination-page-size-selector page-size-selector))))

(defn with-cell-selection
  "Enable cell (range) selection — Enterprise. Pass true for defaults, or the
  v32.2+ cellSelection object form for finer control, e.g.
  {:handle {:mode \"fill\"}} to turn on the fill handle. Requires an Enterprise
  module bundle registered (CellSelectionModule) and a license."
  [opts cell-selection]
  (assoc opts :cell-selection cell-selection))

(defn with-infinite-datasource
  "Behavioral bundle (Community, Infinite Row Model): assoc
  `:row-model-type \"infinite\"` plus a `:datasource` wrapping `get-rows`, and
  optional cache sizing. Bundles the row-model wiring — it does NOT marshal the
  callbacks (ADR 0010 §5).

  `get-rows` is your fetch function. It receives one argument, the kebab-bean
  request params: `:start-row`, `:end-row`, `:sort-model`, `:filter-model`, and
  the raw AG Grid callbacks `:success`/`:fail`. Rows are JS by contract, so
  reply with a JS object:

      (fn [params]
        (let [page (fetch (:start-row params) (:end-row params))]
          ((:success params) #js {:rowData (into-array page) :rowCount total})))

  Second arg (optional) is cache sizing: `{:cache-block-size 100
  :max-blocks-in-cache 10}`. Writes `rowModelType`, `datasource`, and any of
  `cacheBlockSize`/`maxBlocksInCache` supplied. Community; requires the
  InfiniteRowModelModule registered (`register!`).

  Example:

      (-> (options)
          (with-columns [{:field :name}])
          (with-infinite-datasource get-rows {:cache-block-size 50}))"
  ([opts get-rows] (with-infinite-datasource opts get-rows {}))
  ([opts get-rows {:keys [cache-block-size max-blocks-in-cache]}]
   (cond-> (assoc opts
                  :row-model-type "infinite"
                  :datasource {:get-rows get-rows})
     cache-block-size    (assoc :cache-block-size cache-block-size)
     max-blocks-in-cache (assoc :max-blocks-in-cache max-blocks-in-cache))))

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

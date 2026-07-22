(ns ag-grid-cljs.core
  "Public API — walking-skeleton first cut. Namespace layout and naming
  are provisional until the namespace-layout decision lands."
  (:require [ag-grid-cljs.impl.convert :as convert]
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

(defrecord GridHandle [api opts])

(defn create-grid!
  "Convert the EDN options map and mount AG Grid on `el`, returning a GridHandle
  — `{:api <GridApi> :opts <last-applied-EDN>}` — that every runtime channel
  takes (ADR 0008). `el` is a DOM element; `opts` is the EDN options map (build
  it with the `with-*` builders or plain `assoc`). The map crosses the
  conversion boundary (kebab->camel, ADR 0005) before it reaches AG Grid.

  The stashed `:opts` is the unconverted EDN, so later `update-grid!` diffs
  compare EDN to EDN; reach the raw GridApi with `grid-api` for anything the
  wrapper does not cover.

  Example:

      (create-grid! el (-> (options)
                           (with-columns [{:field :name}])
                           (with-row-data rows)))"
  [el opts]
  (->GridHandle (createGrid el (convert/->js opts)) opts))

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

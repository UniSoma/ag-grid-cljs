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

;; --- mount ------------------------------------------------------------------

(defn create-grid
  "Convert the EDN options map and mount AG Grid on el. Returns the GridApi."
  [el opts]
  (createGrid el (convert/->js opts)))

(defn destroy!
  "Tear down a grid instance (releases its DOM and listeners)."
  [api]
  (.destroy ^js api))

;; --- explicit data channel (first cut, ticket agd-01ky0ed8766f) --------------

(defn set-rows!
  "Replace the full row set. Rows are JS by contract: a JS array of JS
  objects. With :get-row-id set, AG Grid diffs by id and preserves grid
  state across the swap."
  [api rows]
  (.setGridOption ^js api "rowData" rows))

(defn transact!
  "Apply a row transaction: {:add [...] :update [...] :remove [...]
  :add-index n}. Rows are JS by contract; :update/:remove match rows via
  :get-row-id. Returns AG Grid's RowNodeTransaction result untouched."
  [api tx]
  (.applyTransaction ^js api (convert/->js tx)))

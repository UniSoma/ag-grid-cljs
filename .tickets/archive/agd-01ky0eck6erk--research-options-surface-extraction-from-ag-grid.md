---
id: agd-01ky0eck6erk
title: 'Research: options-surface extraction from AG Grid TypeScript definitions'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-20T19:00:04.430180422Z'
updated: '2026-07-20T19:07:50.214172606Z'
closed: '2026-07-20T19:07:50.214172606Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:research
assignee: jonas
---

## Description

## Question

Can the option-key registry (all grid option, column-def, and event names) be mechanically extracted from AG Grid's published .d.ts files, and how stable is that extraction across releases? Identify which type definition files/entry points enumerate GridOptions, ColDef, and the event map; whether the names are extractable with a simple script (regex/ts-morph) or need the TS compiler API; how often the surface churns per release; and what the extraction output should look like to feed (a) dev-mode typo warnings with did-you-mean and (b) a kebab-case<->camelCase reference table. Primary sources: ag-grid-community package contents and the AG Grid repo.

## Notes

**2026-07-20T19:07:40.996890694Z**

# Research: options-surface extraction from AG Grid TypeScript definitions

**Verdict: feasible, and easier than expected.** All grid-option, column-def, and event names are mechanically extractable from the published `ag-grid-community` package. GridOptions keys and event names are even available as *runtime constants* — no TS parsing needed for those. ColDef keys need a small ts-morph/compiler-API pass over the shipped `.d.ts`. The surface is highly stable: across v33→v34→v35→v36 the change was purely additive (~8–20 new GridOptions keys per major, 0 removals); removals happen only on deprecation-sweep majors (v32→v33 removed 12 GridOptions + 3 ColDef keys). Verified empirically against npm packages 32.3.9, 33.3.2, 34.3.1, 35.3.1, 36.0.1.

**2026-07-20T19:07:50.214172606Z**

Feasible. GridOptions keys, event names, and the event->handler map ship as runtime constants in ag-grid-community (_GET_ALL_GRID_OPTIONS, _PUBLIC_EVENTS, _PUBLIC_EVENT_HANDLERS_MAP); ColDef keys need a ~25-line ts-morph pass over dist/types/src/entities/colDef.d.ts (regex fails: method-syntax handlers + interface inheritance). Churn measured across v33-v36: additive-only (~8-20 GridOptions keys per major, 0 removals; removals only on deprecation sweeps like v32->33). Recommended output: per-version generated EDN keyed by kebab keyword with {:camel :type :default :initial? :deprecated :doc}, plus an :events block pairing event name and onX handler - serves O(1) typo checks with did-you-mean over ~600 keys and a collision-free kebab<->camel table (all 726 keys round-trip cleanly).

## 1. Where the surface lives (files/entry points)

All paths are inside the published `ag-grid-community` npm package (v33+ layout; `"types": "./dist/types/src/main.d.ts"` per its `package.json`):

| Surface | File in package | What it contains |
|---|---|---|
| GridOptions interface | `dist/types/src/entities/gridOptions.d.ts` | `export interface GridOptions<TData>` — 462 props in v36.0.1 (incl. 111 `onX?(event)` handlers), each with JSDoc carrying `@default`, `@initial`, `@agModule`, `@deprecated` tags |
| ColDef / ColGroupDef | `dist/types/src/entities/colDef.d.ts` (+ `colDef-base.d.ts`) | `ColDef extends AbstractColDef, IFilterDef` — 153 props flattened in v36.0.1; `ColGroupDef` 27 |
| Event map | `dist/types/src/eventTypes.d.ts` | `_PUBLIC_EVENTS` — a `readonly [...]` tuple of every public event name (111 in v36.0.1), plus `_INTERNAL_EVENTS`, `AgPublicEventType`, `_GET_ALL_EVENTS` |
| Event→handler map | `dist/types/src/publicEventHandlersMap.d.ts` | `_PUBLIC_EVENT_HANDLERS_MAP: Record<eventName, 'onEventName'>` — the exact event-name → GridOptions-handler mapping |
| GridOptions keys, by type | `dist/types/src/propertyKeys.d.ts` | `_BOOLEAN_GRID_OPTIONS`, `_NUMBER_GRID_OPTIONS`, `_FUNCTION_GRID_OPTIONS`, `_GET_ALL_GRID_OPTIONS()` |

GitHub sources (repo mirrors of the same): https://github.com/ag-grid/ag-grid/blob/latest/packages/ag-grid-community/src/propertyKeys.ts (verified: defines `STRING/OBJECT/ARRAY/_NUMBER/_BOOLEAN/_FUNCTION_GRID_OPTIONS` and `_GET_ALL_GRID_OPTIONS()`), and sibling `src/eventTypes.ts`, `src/entities/gridOptions.ts`, `src/entities/colDef.ts`. The header of `gridOptions.d.ts` says: *"If you change the GridOptions interface, you must also update PropertyKeys to be consistent"* — AG Grid maintains this invariant themselves.

**Runtime shortcut (verified by executing v36.0.1):** `require('ag-grid-community')` exports `_GET_ALL_GRID_OPTIONS()` → 351 keys (option keys minus `on*` handlers), `_PUBLIC_EVENTS` → 111 names, `_PUBLIC_EVENT_HANDLERS_MAP` → 111 entries. Caveat: these carry `@internal AG_GRID_INTERNAL — Can change / be removed at any time` JSDoc, so treat them as a build-time extraction source pinned to a version, not a stable public API. There is **no** runtime key list for ColDef in v33+ (the old `ColDefUtil.ALL_PROPERTIES` was removed).

## 2. Regex vs ts-morph vs TS compiler API

Tested a naive regex (`^    (\w+)\??:` over `gridOptions.d.ts`): **fails**. It produced 385 unique names vs the true 462 — false positives from other interfaces in the same file (`RowClassParams`, `ChartRefParams`, …) and it misses all 111 event handlers because they use method syntax (`onCellClicked?(event: CellClickedEvent<TData>): void;`). For ColDef, regex on one interface also misses inherited props (`ColDef extends AbstractColDef, IFilterDef`).

**ts-morph (or raw TS compiler API) is the right tool and is cheap**: `checker.getPropertiesOfType()` on the interface flattens inheritance, includes method-syntax members, and hands you JSDoc + type text per property. My working proof is ~70 lines of raw compiler API (`scratchpad/rt/extract.js`); in ts-morph it is ~25 lines (`project.getSourceFile('entities/gridOptions.d.ts').getInterface('GridOptions').getType().getProperties()`). Runs in a couple of seconds per version. The full type-checking programmatic API stays in TS 5.x (TS 7/tsgo does not expose it the same way), so pin `typescript@5.x` as a devDependency of the extraction script.

Recommended hybrid: take GridOptions keys + events + handler map from the runtime constants (zero parsing), and use ts-morph only for ColDef/ColGroupDef and for JSDoc metadata (`@deprecated`, `@initial`, `@agModule`, first doc line for warning messages).

## 3. Churn between releases (measured)

Extraction run on four published versions; diffs of key sets:

| Boundary | GridOptions | ColDef | Public events |
|---|---|---|---|
| 32.3.9 → 33.3.2 | +19 / −12 | +5 / −3 | (v32 tuple shape differs; v33 restructured events) |
| 33.3.2 → 34.3.1 | +19 / −0 | +4 / −0 | +7 / −0 |
| 34.3.1 → 35.3.1 | +20 / −0 | +4 / −0 | +0 / −0 |
| 35.3.1 → 36.0.1 | +8 / −0 | +7 / −0 | +4 / −0 |

Pattern: minors and most majors are additive-only; removals cluster on deprecation-sweep majors (v33 removed long-deprecated options — `enableCellChangeFlash`, `groupIncludeFooter`, `suppressAsyncEvents`, `advancedFilterModel`, ColDef `suppressMenu`, … — documented in the v33 upgrade guide, https://www.ag-grid.com/javascript-data-grid/upgrading-to-ag-grid-33/). Practical consequence: regenerate the registry per supported AG Grid version at build time (it is a ~5-second script), and version the output file with the AG Grid version it was extracted from. A registry that lags one minor behind only yields false "unknown option" warnings for brand-new options, never false acceptance.

Also relevant: AG Grid v33+ ships its own opt-in **ValidationModule** that "adds console warnings and errors that help identify misconfiguration during development," recommended for dev builds only (https://www.ag-grid.com/javascript-data-grid/modules/). A CLJS wrapper should still keep its own registry — the wrapper must validate *kebab-case* keys before conversion, which AG Grid's validator never sees — but registering ValidationModule in dev is a free second layer.

## 4. Recommended extraction output shape

One generated EDN (or CLJC `def`) file per AG Grid version, e.g. `resources/ag-grid-surface/36.0.1.edn`:

```clojure
{:ag-grid-version "36.0.1"
 :grid-options   ;; 351 non-handler option keys
 {:row-data      {:camel "rowData"      :type :array   :doc "Set the data to be displayed as rows in the grid."}
  :column-menu   {:camel "columnMenu"   :type :string  :initial? true :doc "Changes the display type of the column menu."}
  :suppress-menu-hide {:camel "suppressMenuHide" :type :boolean :default true}
  ...}
 :col-def        ;; 153 keys, same entry shape, plus :col-group-def (27)
 {:header-name {:camel "headerName" :type :string :doc "..."}
  ...}
 :events         ;; 111 entries, from _PUBLIC_EVENT_HANDLERS_MAP
 {:cell-clicked {:event "cellClicked" :handler "onCellClicked"}
  ...}
 :deprecated     ;; keys carrying @deprecated, with the replacement parsed from the JSDoc text
 {:suppress-cell-flash {:camel "suppressCellFlash" :note "v31 use enableCellChangeFlash..."}}}
```

Why this shape serves both consumers:

- **(a) dev-mode typo warnings with did-you-mean:** the kebab keyword is the map key, so `(contains? grid-options k)` is the O(1) validity check on exactly what the user typed. Did-you-mean = Levenshtein/Jaro over `(keys grid-options)` (351 + 153 + 111 = ~600 strings; trivially fast, and computed only on the miss path). `:type` enables a second-tier warning ("expected boolean, got string"), `:deprecated` gives migration hints, and `:doc`'s first sentence makes the warning self-explanatory.
- **(b) kebab↔camel reference table:** each entry already pairs kebab (key) with `:camel`; the reverse table is `(into {} (map (juxt (comp keyword :camel) ...)))`. **Verified round-trip safety:** all 726 v36 keys survive camel→kebab→camel with the standard conversion and produce zero kebab collisions, so a single deterministic conversion function + this table as the authority is sound. The `:events` block additionally resolves the three-way naming (`:on-cell-clicked` option ↔ `"cellClicked"` addEventListener name ↔ `onCellClicked` handler prop) from AG Grid's own `_PUBLIC_EVENT_HANDLERS_MAP` instead of string munging.

Keep the extractor (a ~100-line Node script: runtime constants + ts-morph pass, `npm pack` the target version into a temp dir) in the repo under `tools/`, run it when bumping the AG Grid dependency, and commit the generated EDN so the CLJS build has no Node/TS dependency.

## Sources

- npm package `ag-grid-community` 32.3.9 / 33.3.2 / 34.3.1 / 35.3.1 / 36.0.1 — files cited above under `dist/types/src/`; runtime exports executed to confirm.
- https://github.com/ag-grid/ag-grid/blob/latest/packages/ag-grid-community/src/propertyKeys.ts (fetched; defines the typed key arrays and `_GET_ALL_GRID_OPTIONS`).
- https://www.ag-grid.com/javascript-data-grid/grid-options/ (official GridOptions reference — "Implements the GridOptions<TData> interface").
- https://www.ag-grid.com/javascript-data-grid/modules/ (ValidationModule: dev-only misconfiguration warnings, excluded from AllCommunityModule).
- https://www.ag-grid.com/javascript-data-grid/upgrading-to-ag-grid-33/ (v33 deprecation removals).

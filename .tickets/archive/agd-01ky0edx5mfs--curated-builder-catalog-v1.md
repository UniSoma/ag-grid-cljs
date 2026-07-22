---
id: agd-01ky0edx5mfs
title: Curated builder catalog v1
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:47.411155123Z'
updated: '2026-07-22T00:47:37.798496320Z'
closed: '2026-07-22T00:47:37.798496320Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
deps:
- agd-01ky0ed83xww
assignee: jonas
---

## Description

## Question

Select and name the v1 curated with-* builder set: which high-traffic options get documented builders, and which behavioral builders (bundling options + handlers with correct interaction semantics — with-range-fill, with-infinite-datasource, with-pending-rows from the inspiration snippet) make the cut for v1 vs later. Output: the named catalog with one-line contracts each, ready for the spec.

## Notes

**2026-07-21T01:38:54.642419814Z**

From the cell-renderer grilling (agd-01ky0ed8adbf): consider a with-data-type-definitions builder — AG Grid Cell Data Types (v31+) auto-wire boolean/date/number columns and dataTypeDefinitions is plain options data the conversion contract already handles; also worth documenting that built-in renderers are reachable by name string (:cell-renderer "agCheckboxCellRenderer"). No typed-renderer catalog in the wrapper (decided).

**2026-07-22T00:47:30.833686389Z**

# Resolution

v1 curated builder catalog locked.

**2026-07-22T00:47:37.798496320Z**

v1 curated builder catalog locked (8 entries): (options), with-columns, with-row-data (JS-by-contract anchor), with-row-id (kw/fn->getRowId), with-selection + with-pagination + with-cell-selection (v32.2+ object forms; cell-selection Enterprise), with-infinite-datasource (Community; getRows marshalling per event/callback ticket). Bar: coerce OR bundle, never merely name. Dropped with-default-col-def; raw noted as escape hatch not a builder. pending-rows + range-fill batch-flush are RECIPES not builders (stateful/app-level); data-type-definitions + built-in-renderers-by-name are prose. Columns stay raw-map (no col builder). Deferred post-v1: col builder, row-grouping bundle, theming. Key finding: inspiration snippet is ag-grid-react+hooks+step-pipeline; we wrap vanilla core and do NOT adopt that architecture.

## Selection bar
A v1 builder must COERCE input OR BUNDLE behavior — never merely name a single option. Discoverability-only sugar over a single boolean/enum is excluded; it's outdocumented by the committed kebab<->camel reference table + dev-mode key registry.

## v1 catalog (8 entries) — plain-map assoc sugar, opts-first, returns opts, ->-threadable, bang-free
- (options) / (options base) — constructor: start an EDN options map.
- with-columns — coercion: assoc :column-defs, vec-coerced.
- with-row-data — contract anchor: assoc :row-data; JS-by-contract, dev-warns on CLJS rows. (Pure assoc, but the sanctioned exception — it teaches the JS-rows contract the reference table can't, and pairs with set-rows!.)
- with-row-id — coercion: keyword-or-fn -> getRowId callback, string-coerced. Load-bearing for set-rows!/transact! diffing (per Fulcro + options-diffing tickets).
- with-selection — option-bundle: friendly shape -> v32.2+ rowSelection OBJECT ({mode, checkboxes, header-checkbox, enable-click-selection}); bundles interdependent flags + shields the v32.2 churn.
- with-pagination — option-bundle: pagination + page-size + page-size-selector; encodes auto-page-size x page-size mutual-exclusion.
- with-cell-selection — option-bundle (ENTERPRISE): friendly shape -> v32.2+ cellSelection object incl. {:handle {:mode "fill"}}. Cell twin of with-selection. No handlers.
- with-infinite-datasource — behavioral (COMMUNITY, Infinite Row Model): row-model-type "infinite" + cache-block-size/max-blocks-in-cache + a datasource whose getRows is CLJS-ergonomic (kebab-bean params in: start-row/end-row/sort-model/filter-model; consumer returns a promise of {:rows :last-row} -> success/fail callbacks). getRows marshalling rides the settled conversion-boundary contract; final handler shape aligns with the Event-and-callback-API-shape ticket (agd-01ky0edx2wec, on the frontier). Gets SIMPLER than the inspiration snippet: no hooks in vanilla core (datasource set once).

`raw` (already shipped) is noted in an "Escape hatches" footer of the catalog doc, NOT a numbered builder — it's a value-level marker, not a with-* opts builder.

## Dropped from catalog
- with-default-col-def — pure assoc, no coercion/bundling; reference table teaches :default-col-def. (May remain in skeleton code as convenience or be deleted at build-out — implementation, not catalog.)

## Not builders
- RECIPES (stateful/app-level; documented framework-agnostically, not shipped as builders): pending-rows (optimistic pinned-top rows keyed by temp-rowid, edit routing update-vs-persist); range-fill batch-flush (batch cell-value-changed between fill-start/fill-end + paste-start/paste-end, flush once). Both are stateful (a buffer / a use-state list) so they cannot be a pure opts->opts with-* fn at all.
- PROSE CAPABILITIES (documented, not builders): data-type-definitions (plain :data-type-definitions passthrough the conversion contract already handles); built-in renderers reachable by name string (:cell-renderer "agCheckboxCellRenderer").
- Columns stay RAW-MAP — no per-column col builder in v1 (ColDef ~100+ keys; a curated slice causes paradigm fall-off, a full mirror is the Malli treadmill already ruled out; conversion contract + key registry make {:field ...} ergonomic and typo-warned).

## Deferred post-v1
per-column col builder; row-grouping bundle; theming builders (theming is still map-fog: "Theming/CSS story").

## Key finding — the inspiration snippet
The snippet (with-range-fill/with-infinite-datasource/with-pending-rows) is an ag-grid-REACT + hooks app layer: React hooks throughout (use-callback/use-memo/use-state/use-ref/use-latest-ref), organized around a React-specific machine — a :grid/apply STEP PIPELINE (push-step), with-props merging into declarative React PROPS, and on-event translating :cell-value-changed -> an onCellValueChanged React prop wrapped in use-callback. We wrap VANILLA CORE (createGrid): no React props, no re-render churn — handlers are plain gridOptions fns (already covered by conversion contract + event/callback ticket), datasource set once imperatively. We therefore explicitly DO NOT adopt that step-pipeline/on-event architecture; builders stay plain-map assoc (as already decided). Each inspiration builder splits into a thin framework-agnostic config kernel (shipped: with-infinite-datasource, with-cell-selection) + a thick stateful app remainder (recipe: pending-rows, range-fill batch-flush).

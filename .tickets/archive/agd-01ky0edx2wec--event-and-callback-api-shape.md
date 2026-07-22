---
id: agd-01ky0edx2wec
title: Event and callback API shape
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:47.323230923Z'
updated: '2026-07-22T01:19:30.463461949Z'
closed: '2026-07-22T01:19:30.463461949Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
deps:
- agd-01ky0ed83xww
assignee: jonas
---

## Description

## Question

Decide the event/callback surface: naming ((grid/on-event :row-selected ...) vs per-event builders vs :on-* option keys); exactly what handlers receive (cljs-bean-wrapped event? raw JS event plus helpers? both?); how handler identity/re-registration works with the declarative options-diffing model; and how AG Grid's ~150 event names are exposed/validated (ties into the key registry). Informed by friction found in the core-mount skeleton.

## Notes

**2026-07-22T01:19:21.097812264Z**

**2026-07-22T01:19:30.463461949Z**

Event/callback surface locked: it reduces to ONE rule — every handler/callback is a plain function-valued option under the conversion-boundary contract, no event-specific API. (1) Primary surface = :on-* option keys (no new fn; rejected per-event builders + imperative on-event). (2) Uniform treatment — event/callback is a docs taxonomy: one lazy kebab-bean arg in, events return nil / callbacks forward-converted out, (ag/raw f) opts out. (3) Imperative gaps (post-creation, one-shot, global *) = raw grid-api only for v1; on-event helper deferred. (4) Codegen folds the :events-block-derived :on-* keys into the validator's known-key universe (did-you-mean works) and classifies all events :initial? false (updatable, latest closure wins via =). (5) Infinite-datasource getRows = raw callback style ((:success p) called with #js rows); with-infinite-datasource bundles row-model wiring, no marshalling sugar (resolves the catalog deferral). (6) Documented consequences only: grid- vs column-level onX validate via different registry blocks; return forward-conversion caveat for data-returning callbacks. No new tickets, no fog graduation.

## Resolution: Event and callback API shape

The event/callback surface reduces to ONE rule already locked by upstream tickets: **every event handler and value callback is just a function-valued key in the options map, governed by the conversion-boundary contract.** No event-specific API, no new fns, no per-event builders. Grilled through in one HITL session; six decisions:

### 1. Primary surface: `:on-*` option keys (no new registration fn)
Handlers are plain grid options: `(-> (options) (with-columns ...) (assoc :on-cell-clicked (fn [e] ...)))`. Falls straight out of decisions already made — AG Grid's own GridOptions carries 111 `onX` handler props; `:on-cell-clicked` -> `onCellClicked` is an already-registered, already-validated, already-updatable option. Rejected: per-event builders (violate the builder bar — 111 name-only builders is exactly what 'coerce or bundle, never merely name' forbids) and a blessed imperative `on-event` fn (reintroduces a handler side-channel the differ can't see).

### 2. Uniform 'function-valued option' surface — event/callback is a DOCS taxonomy, not an API branch
Two kinds AG Grid treats differently — **events** (~111 `onX`, return ignored) and **value callbacks** (`getRowId`, `getRowStyle`, `isRowSelectable`, `getContextMenuItems`, ColDef `valueGetter`/`valueFormatter`/`cellClassRules`, return consumed) — get ONE treatment. Per the conversion contract (agd-01ky0eck96vn): IN = one arg, a lazy kebab-bean over AG Grid's event/params object (`(:value e)` `(:data e)` `(:node e)` `(:api e)`; `:api` is a raw JS instance reached as a bean view); OUT = events return nil (ignored), callbacks' returns forward-converted; OPT-OUT = `(ag/raw f)` for hot paths. Value callbacks that deserve coercion already got dedicated builders (`with-row-id`); the rest stay plain function-valued keys.

### 3. Imperative gaps (post-creation listeners, one-shot/self-removing, global `*`): raw-API-only for v1
`:on-*` options can't express: registering a listener after creation without an `update-grid!` pass; one-shot listeners; the global `*` listener (AG Grid has no `onX` option for it). Answer: use the already-decided `grid-api` raw escape hatch — `(.addEventListener (grid-api h) \"cellClicked\" f)`. Build NOTHING. A wrapper helper was rejected because it (a) forks handler state across two models — partially un-deciding #1; (b) forces the wrapper to own listener lifecycle (removal on destroy!, dedup, unlisten handles) + new mutable GridHandle state; (c) the `*` case fans out the per-call bean cost to every event (worst case for the benchmark ticket); (d) can't be ergonomically consistent (raw events vs beans). Documented cost: raw-API path hands back the raw JS event, not a bean. Defer a minimal `on-event` to post-v1 if real friction appears.

### 4. Validation & updatability of `:on-*` keys
Wrinkle: `_GET_ALL_GRID_OPTIONS()` (registry `:grid-options`, 351 keys) EXCLUDES `on*` handlers — they live in the separate `:events` block (111 entries `{:event :handler}`). So:
- **Codegen derives an `:on-*` kebab key set** from each `:events` entry's `:handler` (`\"onCellClicked\"` -> `:on-cell-clicked`) and folds it into the validator's known-grid-options universe, so `:on-*` keys validate + get did-you-mean like any option (typo `:on-cel-clicked` warns; a real key never spuriously flags).
- **Events are categorically `:initial? false`** — block membership IS the classification (no per-event flag). The differ treats any events-block key as known-and-updatable; latest closure wins via `=` (per agd-01ky0edx8dzc). Keeps the uniform-surface promise honest down to validation — `:on-*` keys are never second-class.

### 5. Infinite-datasource `getRows`: raw callback style, no marshalling sugar (resolves the deferral from the catalog ticket agd-01ky0edx5mfs)
`getRows` rides the uniform rule: params arrive as a kebab-bean (`:start-row` `:end-row` `:sort-model` `:filter-model`), and AG Grid's supplied `:success`/`:fail` are raw JS fns reached through the bean — called `((:success p) #js {:rowData rows :rowCount total})` (rows are JS by contract). `with-infinite-datasource` earns its builder slot by BUNDLING `:row-model-type \"infinite\"` + `:datasource` wiring, NOT by marshalling the callbacks. Rejected: a return/promise-adapted style (builder would own an async protocol — value? promise? channel? — and hard-code the `success({rowData,rowCount})` shape, which already changed once in v31; drift-prone). Defer return/promise adaptation to post-v1.

### 6. Documented consequences (no machinery)
- **Grid- vs column-level `onX` validate via different registry blocks.** Grid-level via the events-derived set (#4); ColDef-level (`onCellClicked` etc. inside a column map) via position-aware `:col-def` validation (ts-morph picks up method-syntax members). Same uniform surface to the user; two registry sources under the hood — codegen already emits both.
- **Forward-conversion of returns can surprise data-returning callbacks.** A `valueGetter`/`getContextMenuItems` returning a CLJS map/vector is forward-converted (kebab->camel, keyword->camel-string) — correct for menu items, wrong for a callback returning data as a cell value; `(ag/raw f)` opts out. Events unaffected (return ignored).

### Inputs consumed
- Conversion boundary (agd-01ky0eck96vn): auto-wrap functions, lazy kebab-bean params, forward-converted returns, `(ag/raw f)` opt-out — the whole 'what handlers receive/return' half.
- Options-diffing (agd-01ky0edx8dzc): callbacks updatable where `:initial? false`, latest closure wins via `=` — the re-registration half.
- Options-surface research (agd-01ky0eck6erk): `:events` block from `_PUBLIC_EVENT_HANDLERS_MAP`; `on*` handlers excluded from `_GET_ALL_GRID_OPTIONS()` — motivated #4.
- Curated builder catalog (agd-01ky0edx5mfs): resolved its explicit `getRows` deferral (#5).

### Fog / new tickets
None. This ticket consumed inputs and confirmed the surface is nothing more than the general options surface; it produced no new fog and graduates nothing. Out-of-scope-here (owned elsewhere): module-gated events (enterprise smoke-test ticket); blessed `on-event` helper and datasource return/promise adaptation (post-v1); full-state declarative handler layer (Reagent/UIx ticket agd-01ky157wpbpv).

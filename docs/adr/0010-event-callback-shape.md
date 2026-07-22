# 0010. Event and callback API shape

- Status: accepted, 2026-07-22
- Origin: knot ticket agd-01ky0edx2wec (tickets are ephemeral; this record is self-contained)

The entire event/callback surface reduces to ONE rule: every event handler and value callback is a plain function-valued key in the options map, governed by the conversion-boundary contract (ADR 0005). No event-specific API, no new fns, no per-event builders.

## Context

The wrapper needed a decided event/callback surface: naming (`(grid/on-event :row-selected ...)` vs per-event builders vs `:on-*` option keys); exactly what handlers receive; how handler identity/re-registration works with the declarative options-diffing model (ADR 0008); and how AG Grid's event names are exposed and validated (ties into the key registry, ADR 0007).

Relevant facts from upstream work:

- AG Grid's own GridOptions carries 111 `onX` handler props, so `:on-cell-clicked` -> `onCellClicked` is an already-registered, already-validated, already-updatable option.
- AG Grid treats two kinds differently: **events** (~111 `onX`, return ignored) and **value callbacks** (`getRowId`, `getRowStyle`, `isRowSelectable`, `getContextMenuItems`, ColDef `valueGetter`/`valueFormatter`/`cellClassRules`, return consumed).
- The options-surface research found a registry wrinkle: `_GET_ALL_GRID_OPTIONS()` (registry `:grid-options`, 351 keys) EXCLUDES `on*` handlers — they live in a separate `:events` block (111 entries `{:event :handler}`, from `_PUBLIC_EVENT_HANDLERS_MAP`).
- The conversion boundary (ADR 0005) already settles what handlers receive and return: auto-wrapped functions, lazy kebab-bean params, forward-converted returns, `(ag/raw f)` opt-out.
- The builder catalog (ADR 0009) explicitly deferred the `getRows` handler shape of `with-infinite-datasource` to this decision.

## Decision

### 1. Primary surface: `:on-*` option keys (no new registration fn)

Handlers are plain grid options: `(-> (options) (with-columns ...) (assoc :on-cell-clicked (fn [e] ...)))`. This falls straight out of decisions already made — `:on-cell-clicked` is just an option.

### 2. Uniform "function-valued option" surface — event vs callback is a DOCS taxonomy, not an API branch

The two kinds AG Grid treats differently get ONE treatment. Per the conversion contract (ADR 0005): IN = one arg, a lazy kebab-bean over AG Grid's event/params object (`(:value e)`, `(:data e)`, `(:node e)`, `(:api e)`; `:api` is a raw JS instance reached as a bean view); OUT = events return nil (ignored), callbacks' returns are forward-converted; OPT-OUT = `(ag/raw f)` for hot paths. Value callbacks that deserve coercion already got dedicated builders (`with-row-id`); the rest stay plain function-valued keys.

### 3. Imperative gaps: raw `grid-api` only for v1

`:on-*` options cannot express: registering a listener after creation without an `update-grid!` pass; one-shot/self-removing listeners; the global `*` listener (AG Grid has no `onX` option for it). Answer: use the already-decided `grid-api` raw escape hatch — `(.addEventListener (grid-api h) "cellClicked" f)`. Build NOTHING. Documented cost: the raw-API path hands back the raw JS event, not a bean. Defer a minimal `on-event` helper to post-v1 if real friction appears.

### 4. Validation and updatability of `:on-*` keys

Because `on*` handlers are excluded from the `:grid-options` registry block:

- **Codegen derives an `:on-*` kebab key set** from each `:events` entry's `:handler` (`"onCellClicked"` -> `:on-cell-clicked`) and folds it into the validator's known-grid-options universe, so `:on-*` keys validate and get did-you-mean like any option (typo `:on-cel-clicked` warns; a real key never spuriously flags).
- **Events are categorically `:initial? false`** — block membership IS the classification (no per-event flag). The differ treats any events-block key as known-and-updatable; the latest closure wins via `=` (per ADR 0008). This keeps the uniform-surface promise honest down to validation — `:on-*` keys are never second-class.

### 5. Infinite-datasource `getRows`: raw callback style, no marshalling sugar

Resolves the deferral from ADR 0009. `getRows` rides the uniform rule: params arrive as a kebab-bean (`:start-row`, `:end-row`, `:sort-model`, `:filter-model`), and AG Grid's supplied `:success`/`:fail` are raw JS fns reached through the bean — called `((:success p) #js {:rowData rows :rowCount total})` (rows are JS by contract). `with-infinite-datasource` earns its builder slot by BUNDLING `:row-model-type "infinite"` + `:datasource` wiring, NOT by marshalling the callbacks. Return/promise adaptation is deferred to post-v1.

### 6. Documented consequences only (no machinery)

- **Grid- vs column-level `onX` validate via different registry blocks.** Grid-level via the events-derived set (#4); ColDef-level (`onCellClicked` etc. inside a column map) via position-aware `:col-def` validation (ts-morph picks up method-syntax members). Same uniform surface to the user; two registry sources under the hood — codegen already emits both.
- **Forward-conversion of returns can surprise data-returning callbacks.** A `valueGetter`/`getContextMenuItems` returning a CLJS map/vector is forward-converted (kebab->camel, keyword->camel-string) — correct for menu items, wrong for a callback returning data as a cell value; `(ag/raw f)` opts out. Events are unaffected (return ignored).

## Consequences

- The event surface is nothing more than the general options surface: no handler side-channel exists, so the options differ sees every handler, and the layered-API guarantees of ADR 0002 apply unchanged.
- Out of scope here, owned elsewhere: module-gated events (Enterprise smoke test); a blessed `on-event` helper and datasource return/promise adaptation (post-v1); a full-state declarative handler layer (Reagent/UIx integration).

## Considered options

- **Per-event builders (111 of them)** — rejected: they violate the builder admission bar of ADR 0009 — 111 name-only builders is exactly what "coerce or bundle, never merely name" forbids.
- **A blessed imperative `on-event` registration fn** — rejected for v1: it (a) forks handler state across two models, partially un-deciding the primary-surface decision; (b) forces the wrapper to own listener lifecycle (removal on `destroy!`, dedup, unlisten handles) plus new mutable GridHandle state; (c) the global `*` case fans out the per-call bean cost to every event (worst case for the conversion benchmark); (d) cannot be ergonomically consistent (raw events vs beans). Deferred, to be revisited post-v1 only if real friction appears.
- **Return/promise-adapted `getRows`** — rejected: the builder would own an async protocol (value? promise? channel?) and hard-code the `success({rowData, rowCount})` shape, which already changed once in v31; drift-prone. Deferred to post-v1.
- **Separate API branches for events vs value callbacks** — rejected: the distinction is a docs taxonomy; both kinds are function-valued options under one conversion contract.

## References

- ADR 0002 — layered API shape
- ADR 0005 — conversion boundary (what handlers receive/return; `(ag/raw f)` opt-out)
- ADR 0007 — key registry / codegen (`:events` block; did-you-mean validation)
- ADR 0008 — options-diffing semantics (updatability; latest closure wins via `=`)
- ADR 0009 — builder catalog v1 (`with-infinite-datasource`; the builder admission bar)

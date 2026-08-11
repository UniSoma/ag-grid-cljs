# 0009. Curated builder catalog v1

- Status: accepted, 2026-07-22
- Amended 2026-08-11: `## Node operations` added — the escape-hatch bar, one channel over from the builders
- Origin: knot ticket agd-01ky0edx5mfs (tickets are ephemeral; this record is self-contained)

The v1 curated `with-*` builder catalog is locked at 8 entries, selected by a strict admission bar: a builder must COERCE input or BUNDLE behavior — never merely name a single option. Everything else is a reference-table entry, a documented recipe, or prose.

## Context

ADR 0002 layers curated `with-*` builders as sugar over the plain EDN options map. This ADR answers which builders make v1: which high-traffic options get documented builders, and which behavioral builders (bundling options + handlers with correct interaction semantics) make the cut versus later.

An inspiration snippet supplied candidate builders (`with-range-fill`, `with-infinite-datasource`, `with-pending-rows`). Investigation found the snippet is an **ag-grid-react + hooks app layer**: React hooks throughout (`use-callback`/`use-memo`/`use-state`/`use-ref`/`use-latest-ref`), organized around a React-specific machine — a `:grid/apply` STEP PIPELINE (`push-step`), with `with-props` merging into declarative React PROPS, and `on-event` translating `:cell-value-changed` into an `onCellValueChanged` React prop wrapped in `use-callback`. We wrap VANILLA CORE (`createGrid`): no React props, no re-render churn — handlers are plain gridOptions fns (already covered by the conversion contract, ADR 0005, and the event/callback surface, ADR 0010), and the datasource is set once imperatively. We therefore explicitly do NOT adopt that step-pipeline/`on-event` architecture; builders stay plain-map assoc as already decided. Each inspiration builder splits into a thin framework-agnostic config kernel (shipped: `with-infinite-datasource`, `with-cell-selection`) plus a thick stateful app remainder (recipe: pending-rows, range-fill batch-flush).

A separate input (from the cell-renderer grilling): AG Grid Cell Data Types (v31+) auto-wire boolean/date/number columns, and `dataTypeDefinitions` is plain options data the conversion contract already handles; built-in renderers are reachable by name string (`:cell-renderer "agCheckboxCellRenderer"`). No typed-renderer catalog in the wrapper.

## Decision

### Selection bar

A v1 builder must COERCE input OR BUNDLE behavior — never merely name a single option. Discoverability-only sugar over a single boolean/enum is excluded; it's outdocumented by the committed kebab<->camel reference table + dev-mode key registry (ADR 0007).

The bar gains a second clause from ADR 0021: a builder's output must be **rebuild-stable** — `=` to itself given `=` inputs — so a consumer who rebuilds the options map per render gets a clean `update-grid!` diff. A builder that mints a fresh closure or object per call fails it unless construction is deferred to the conversion boundary. ADR 0021 states the clause as binding any public fn contributing an option value, not just the builders in this catalog, since it covers the renderer helpers (ADR 0011) too.

### v1 catalog (8 entries) — plain-map assoc sugar, opts-first, returns opts, `->`-threadable, bang-free

> **Entry #2's justification corrected (2026-07-29).** `with-columns` is admitted as a sanctioned exception alongside `with-row-data`, not as a coercion: `(vec col-defs)` normalizes the container but is not load-bearing — `->js` already converts anything `sequential?`, and `(= [x] '(x))`, so it buys neither conversion nor rebuild stability. It earns its keep by teaching the `:field`<->row-spelling pairing, which the reference table cannot express. Membership and the coerce-or-bundle bar are unchanged.

1. `(options)` / `(options base)` — constructor: start an EDN options map.
2. `with-columns` — coercion: assoc `:column-defs`, vec-coerced.
3. `with-row-data` — contract anchor: assoc `:row-data`; JS-by-contract, dev-warns on CLJS rows. (Pure assoc, but the sanctioned exception — it teaches the JS-rows contract the reference table can't, and pairs with `set-rows!`.)
4. `with-row-id` — coercion: keyword-or-fn -> `getRowId` callback, string-coerced. Load-bearing for `set-rows!`/`transact!` diffing (ADR 0008).
5. `with-selection` — option-bundle: friendly shape -> v32.2+ `rowSelection` OBJECT (`{mode, checkboxes, header-checkbox, enable-click-selection}`); bundles interdependent flags + shields the v32.2 churn.
6. `with-pagination` — option-bundle: pagination + page-size + page-size-selector; encodes the auto-page-size x page-size mutual-exclusion.
7. `with-cell-selection` — option-bundle (ENTERPRISE): friendly shape -> v32.2+ `cellSelection` object incl. `{:handle {:mode "fill"}}`. Cell twin of `with-selection`. No handlers.
8. `with-infinite-datasource` — behavioral (COMMUNITY, Infinite Row Model): `:row-model-type "infinite"` + cache-block-size/max-blocks-in-cache + a datasource whose `getRows` marshalling follows ADR 0010: params arrive as a kebab-bean (`:start-row`/`:end-row`/`:sort-model`/`:filter-model`), success/fail are raw callbacks — the builder bundles row-model wiring, not callback marshalling. Gets SIMPLER than the inspiration snippet: no hooks in vanilla core (datasource set once).

`raw` (already shipped) is noted in an "Escape hatches" footer of the catalog doc, NOT a numbered builder — it's a value-level marker, not a `with-*` opts builder.

### Dropped from catalog

- `with-default-col-def` — pure assoc, no coercion/bundling; the reference table teaches `:default-col-def`. (May remain in skeleton code as a convenience or be deleted at build-out — implementation, not catalog.)

### Not builders

- **RECIPES** (stateful/app-level; documented framework-agnostically, not shipped as builders): **pending-rows** (optimistic pinned-top rows keyed by temp-rowid, edit routing update-vs-persist); **range-fill batch-flush** (batch cell-value-changed between fill-start/fill-end + paste-start/paste-end, flush once). Both are stateful (a buffer / a use-state list) so they cannot be a pure opts->opts `with-*` fn at all.
- **PROSE CAPABILITIES** (documented, not builders): data-type-definitions (plain `:data-type-definitions` passthrough the conversion contract already handles); built-in renderers reachable by name string (`:cell-renderer "agCheckboxCellRenderer"`).
- **Columns stay RAW-MAP** — no per-column `col` builder in v1 (ColDef has ~100+ keys; a curated slice causes paradigm fall-off, a full mirror is the Malli treadmill already ruled out in ADR 0002; the conversion contract + key registry make `{:field ...}` ergonomic and typo-warned).

### Deferred post-v1

Per-column `col` builder; row-grouping bundle; theming builders (theming is docs-only in v1 per ADR 0013).

## Node operations

Added 2026-08-11. A proposal arrived for two RowNode wrappers — `set-data-value!` (coerce a column ident, call `.setDataValue`, pass a `source` sentinel) and `set-row-data!` (merge into `node.data`, call `.setData`) — pitched as a **node-ops** category beside the builders, with its own bar: *a pure wrapper over a documented RowNode method*. Both are rejected, and the category is not opened.

### The bar for the escape hatch

The proposed bar admits everything: `.setSelected`, `.setExpanded`, `.setRowHeight` and `.setRowSelectable` all clear "pure wrapper over a documented method" identically, which is the mirror-the-API treadmill ADR 0002 ruled out. ADR 0004 channel 3 already owns this ground — `grid-api` is the documented route to any imperative capability — so the question is not *what may we add* but *what must leave the hatch*.

The line, proposed by the requester and adopted: **does the call cross the wrapper's data-conversion boundary, and does its raw form fight a contract the wrapper already committed to?** Primitive-taking, row-object-free node methods are nothing the reference table cannot express; they stay behind `grid-api`. On this test `.setData` is the only interesting RowNode method — the only one taking a **row object**, which is JS-by-contract (ADR 0003) at a spelling the consumer fixed at datasource time.

### `set-data-value!` — rejected

Stripped down it is `(.setDataValue node (name col) v source)`: a `name` call plus a passthrough argument. The `name` is the keyword→string decision the library deliberately leaves to the consumer for `:field` (entry #3 above) — doing it here and nowhere else makes the library inconsistent with itself. And `source` is one arm of a protocol: the sentinel write is meaningless without the handler-side guard (`(when-not (= "restore" source) …)`) that breaks the write→fail→restore→write cycle. Shipping one arm as a function and leaving the other in prose implies an ownership the library does not have. It goes to the recipes, beside pending-rows, which this ADR sent there for the same reason.

### `set-row-data!` — clears the bar, loses on the converter

The merge is not sugar: `.setData` *replaces*, so a consumer changing one key silently drops the rest of the row. Teaching a contract the raw method fights is the `with-row-data` shape, and it clears the bar above. It is rejected anyway, on the fact that killed it at its own call sites: **a library-side merge has to convert with the same converter the rows were built with, and the library cannot know which that was.** Under a camelizing merge against literal-kebab rows, `{:row-id 7}` lands `rowId` beside the live `"row-id"`; the row-id getter then reads the ghost and rows lose identity after an edit — silent, and visible only downstream. Guessing the row recipe is the `:field` problem that rejected `set-data-value!`, one level up. The version that dodges it takes a JS object, at which point it is `Object.assign` plus `.setData` with a docstring.

Call-site evidence, from the requester, cuts the same way: two sites, one app, one code path; all merging; all nodes callback-derived; all keys bare `clj->js` on the literal recipe.

### What ships instead

Prose. One editable-grids article carrying pending-rows, the rollback loop (both arms plus the batch variant — an all-or-nothing server rollback restores N cells and fires N `cellValueChanged` events), and the server round-trip write-back with the `.setData` replace trap. The load-bearing rule from the withdrawn function survives as a rule: *a write into a row must use the converter the row was built with* — now also in `CONTEXT.md`'s **row recipe** entry, which previously scoped the pairing to rendering only.

### Not the reframing: beans

The review initially claimed `(:node params)` is a callback bean and that the helpers therefore needed a public `unwrap`. That is wrong. All three wrap points gate on `cljs.core/object?` — `(identical? (.-constructor x) js/Object)` — and `RowNode` is a class (`main.cjs.js:4259`), so it is handed back untouched; `.setDataValue` on it works in an ordinary non-`raw` handler. The gate is also the only reason `params.api` and `params.column` (`AgColumn`, `main.cjs.js:2411`) keep their methods. Nothing was blocked, and no unwrap is load-bearing here. The gate did contradict ADR 0018 §2's claim that the lookup law covers RowNode arguments; that was a documentation defect rather than a code one, and §2 now states the gate and the rule *beans cover data, not AG Grid objects*.

## Consequences

- The catalog is small and every entry justifies itself against the bar, so the catalog does not grow by naming-sugar accretion; new candidates must coerce or bundle.
- `with-infinite-datasource`'s `getRows` handler shape is owned by ADR 0010, keeping one callback contract across the wrapper.

## Considered options

- **Adopting the inspiration snippet's architecture (step pipeline, `with-props`, `on-event`, hook-wrapped builders)** — rejected: it is React-specific machinery for ag-grid-react props; we wrap vanilla core, where handlers are plain gridOptions fns and the datasource is set once. Builders stay plain-map assoc.
- **`with-range-fill` / `with-pending-rows` as builders** — rejected: their substance is stateful app-level behavior that cannot be a pure opts->opts fn; the config kernels that survive are `with-cell-selection` (fill handle) and `with-infinite-datasource`, and the stateful remainders become recipes.
- **`with-default-col-def`** — rejected: pure assoc with no coercion or bundling; fails the bar.
- **`with-data-type-definitions`** — rejected as a builder: plain passthrough the conversion contract already handles; documented as prose.
- **Per-column `col` builder** — deferred: a curated slice of ColDef's ~100+ keys causes paradigm fall-off; a full mirror is the schema treadmill ruled out by ADR 0002.
- **`raw` as a numbered catalog entry** — rejected: it's a value-level escape-hatch marker, not an opts builder; documented in an escape-hatches footer.

## References

- ADR 0002 — layered API shape (builders as sugar over the EDN options map)
- ADR 0005 — conversion boundary
- ADR 0007 — key registry / codegen (outdocuments naming-only sugar)
- ADR 0008 — options-diffing semantics (`with-row-id` is load-bearing for diffing)
- ADR 0010 — event and callback shape (`getRows` marshalling)
- ADR 0013 — theming docs-only in v1
- ADR 0021 — rebuild-stable option values (the second admission-bar clause)

Added 2026-08-11 with the node-operations section:

- ADR 0003 — JS-by-contract rows (the contract `.setData` writes into)
- ADR 0004 — update model (`grid-api` as channel 3, the home node ops keep)
- ADR 0018 §2 — callback beans (the `object?` gate, and the RowNode claim it contradicts)
- `docs/options-and-conversion.md` — the two **row recipes** the merge could not choose between

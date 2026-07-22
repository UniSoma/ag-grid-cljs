---
id: agd-01ky0ebxg01e
title: 'Map: ag-grid-cljs — spec + walking skeleton'
status: open
type: epic
priority: 1
mode: hitl
created: '2026-07-20T18:59:42.208243308Z'
updated: '2026-07-22T00:48:04.059566705Z'
tags:
- wayfinder:map
---

## Destination

A locked design spec plus a walking skeleton for **io.github.unisoma/ag-grid-cljs** — an MIT-licensed, open-source, framework-agnostic ClojureScript wrapper for AG Grid (Community + Enterprise), vanilla-core based, with Fulcro as the reference-consumer bar. Done when every design decision is resolved, the skeleton proves the five risk points (core mount, Fulcro integration, CLJS cell renderer, cljs.proxy benchmark, Enterprise smoke test), and implementation could start as a fresh effort with no open questions.

## Notes

- Domain: ClojureScript library design; JS interop; AG Grid v34+ (ESM, per-feature modules).
- Skills each session should use: /grilling + /domain-modeling for decision tickets, /prototype for skeleton tickets, /research subagents for research tickets.
- Tracker: knot (this repo). Ticket types carried as tags `wayfinder:research|prototype|grilling|task`; HITL/AFK via knot's native `--mode`.
- Toolchain floor: shadow-cljs >= 3.3.0 (ClojureScript 1.12.116+). AG Grid npm packages are peer dependencies, never bundled.
- Zero runtime dependencies besides the AG Grid peer packages: needed library code (cljs-bean slice) is vendored under an internal namespace, EPL headers retained, THIRD-PARTY.md notice.
- Research findings are recorded as notes on the research ticket itself (file-based tracker; no side branches).
- Side research: `docs/research/ag-grid-react-wrapper.md` — sourced analysis of ag-grid-react vs vanilla core. Corroborates the Foundation/Update-model/cell-renderer decisions; informs the options-diffing and Reagent/UIx tickets (see notes on those tickets). Version-pinned ~v31–32 wrapper source — re-check version-sensitive claims at implementation time.
- Map maintenance: this body is multi-section — update it with `knot update <map-id> --body`, never `--description` (that replaces only the ## Description section and stacks duplicates).
- Planning-first: tickets resolve decisions; the walking-skeleton prototype tickets are the only execution carried by this map.

## Decisions so far

Settled during charting (no ticket — resolved in the charting grilling session):

- **Scope** — spec + walking skeleton; shipping v0.1 is a separate later effort.
- **Foundation** — wrap vanilla ag-grid-community core (createGrid), not ag-grid-react; Fulcro is the reference-consumer bar; Community + Enterprise both wrapped.
- **API shape** — layered: plain EDN options map at bottom (kebab-case auto-translated to camelCase, full surface reachable via raw assoc); curated documented `with-*` builders on top; generated key registry powering dev-mode typo warnings and a kebab<->camel reference table.
- **Row data** — JS by contract; cljs-bean/js-interop ergonomics at every callback boundary; cljs.proxy gets a measure-and-decide benchmark.
- **Update model** — declarative options diffing + explicit transactional data channel (set-rows! / transact!) + raw grid API escape hatch.
- **Coordinates** — io.github.unisoma/ag-grid-cljs, MIT, Clojars.

Resolved tickets:

- [Research: AG Grid module registry, ESM packaging, and Enterprise licensing surface](agd-01ky0eck3myz) — consumer owns module registration; wrapper core depends only on ag-grid-community and exposes a thin optional `register!` plus an opt-in enterprise namespace with `set-license-key!` (must run before grid creation); selective modules save ~20–40% bundle only with an external ESM-aware bundler. Full sourced report in the ticket's notes.
- [Research: options-surface extraction from AG Grid TypeScript definitions](agd-01ky0eck6erk) — feasible and cheap: GridOptions keys, event names, and the event→handler map are runtime constants in ag-grid-community; only ColDef needs a small ts-morph pass; churn is additive-only (~8–20 keys/major, v33–v36); recommended output is per-version EDN keyed by kebab keyword with :camel/:type/:deprecated/:doc plus an events block — all 726 keys round-trip kebab↔camel with zero collisions. Full sourced report in the ticket's notes.
- [Conversion boundary rules: what converts, what passes through untouched](agd-01ky0eck96vn) — mechanical kebab→camel (registry = dev warnings only); type-driven recursion (CLJS maps/sequentials convert, everything else passes untouched, string keys verbatim); keywords translate in value position too (keyword = AG Grid vocabulary, string = verbatim); `(ag/raw x)` is the sole escape hatch; nil→null with key kept; sets pass through + dev-warn; callbacks receive lazy kebab-keyed beans and returns are forward-converted, with `raw` as the hot-path opt-out; cljs-bean slice vendored for zero deps. Full contract in the ticket's resolution note.
- [Walking skeleton: core mount from builder-produced EDN options](agd-01ky0ed83xww) — core design proven end to end: builder-produced EDN → conversion contract → createGrid, verified by node contract tests plus a headless mount check (fn survives conversion with lazy kebab bean params and converted return). Scaffold settled: src/main library + src/dev dev app + src/test, deps.edn with :dev alias, shadow-cljs :dev-app/:test builds, `npm run dev` / `npm test`. Builders are plain-map sugar over the EDN options map; cljs-bean rides as a direct dep pending vendoring; `ag-grid-cljs.*` namespaces provisional pending the naming ticket. Full resolution in the ticket's notes.
- [Walking skeleton: CLJS custom cell renderer helper](agd-01ky0ed8adbf) — three renderer tiers committed, no wrapper-owned DOM engine: bare fn = vanilla escape hatch (innerHTML string semantics kept, dev-warn on HTML-looking returns); `render/renderer` lifecycle-map + engine-free `dom-renderer` (string = text; BYO DOM by fn composition — mini hiccup engine cut); `react/react-renderer` with per-cell local root (portal variant deferred to the Fulcro skeleton ticket). No typed-renderer catalog — AG Grid cell data types + name-registered built-ins cover it. Full resolution in the ticket's notes.
- [Walking skeleton: Fulcro integration with transactional data updates](agd-01ky0ed8766f) — Fulcro bar proven with zero library support: class-based mount-point host (sCU false; confirmed canonical per the Fulcro guide, hooks variant weaker); explicit data channel (new core fns `set-rows!`/`transact!`/`destroy!`) preserves scroll/selection/focus across :add/:update transactions and set-rows! full swaps (AG Grid diffs by :get-row-id); transact!-from-a-cell with an explicit app reference is one line and acceptable — no portal variant. Dual bookkeeping eliminable via set-rows!-from-db as the default consumer pattern; auto-diff watcher deferred to the options-diffing ticket. Full resolution in the ticket's notes.
- [Namespace layout and public API naming](agd-01ky0m0btmrp) — root `ag-grid-cljs`; fat `core` (setup + builders + runtime API) plus satellites `render`, `react`, opt-in `enterprise` (set-license-key!); internals under `ag-grid-cljs.impl.*` as the "private, may change" marker; `!` marks side-effecting fns (`create-grid!`/`register!`/`destroy!`/`set-rows!`/`transact!`/`set-license-key!`) while pure builders/accessors (`options`, `raw`, `with-*`) stay bang-free; builders are `with-<thing>`, opts-first, return opts, `->`-threadable. Implied skeleton drift for implementation: rename `create-grid` → `create-grid!`. Full resolution in the ticket's notes.
- [Key registry: codegen pipeline and dev-warning design](agd-01ky0evm9nap) — codegen emits a `goog.DEBUG`-guarded CLJS literal at `ag-grid-cljs.impl.registry` (DCE'd whole from prod), NOT a runtime `.edn`; manual Node/ts-morph tool under `tools/`, generated CLJS committed, single AG-Grid-version pin stamped as `:ag-grid-version` (deps.cljs can't express the peer dep — README + package.json `peerDependencies` does); full rich per-key shape `{:camel :type :default :initial? :deprecated :doc}` (`:initial?` feeds the options-diffing ticket); position-aware dev validation (grid-options + known ColDef positions, opaque elsewhere), warn + kebab did-you-mean, never reject; MIDDLE division of labor vs AG Grid's `ValidationModule` — wrapper owns kebab unknown-key + did-you-mean + kebab deprecation warnings, delegates type/dependency/row-model to `ValidationModule` (off by default v36+), warnings deduped once per `[object-name key]`; same codegen pass emits committed `docs/reference/ag-grid-options.md`. Full resolution in the ticket's notes.
- [Options-diffing semantics: what is updatable and how changes apply](agd-01ky0edx8dzc) — `create-grid!` returns a `GridHandle` `{:api :opts}` (raw GridApi via `grid-api`); new `update-grid! [handle new-opts]` is a PATCH/MERGE differ compared by `=` uniformly (no function special-case), per top-level key: `setGridOption` only for keys PRESENT+changed, absent key = leave-as-is (no call), stash accumulates via merge. Registry `:initial?` is the sole classifier; initial-only change = dev-warn-once-per-key + ignore (recreate deferred); `:row-data` ignored (owned by set-rows!/transact!); unclassified changed keys applied optimistically + warn; `:column-defs` whole-value handoff, AG Grid owns column diffing (pin `colId`). Function stale-closure handled by architecture + guidance, not always-reapply. Full-state declarative layer punted to the Reagent/UIx ticket. Full resolution in the ticket's notes.
- [Curated builder catalog v1](agd-01ky0edx5mfs) — v1 catalog locked (8 entries): `(options)`, `with-columns`, `with-row-data` (JS-by-contract anchor), `with-row-id` (kw/fn→getRowId), `with-selection`/`with-pagination`/`with-cell-selection` (v32.2+ object forms; cell-selection Enterprise), `with-infinite-datasource` (Community; getRows marshalling per the event/callback ticket). Bar: a builder must COERCE input or BUNDLE behavior, never merely name an option (reference table + key registry cover naming). Dropped `with-default-col-def`; `raw` noted as an escape hatch, not a builder. pending-rows + range-fill batch-flush are RECIPES not builders (stateful/app-level); data-type-definitions + built-in-renderers-by-name are prose. Columns stay raw-map (no `col` builder). Deferred post-v1: `col` builder, row-grouping bundle, theming. Key finding: the inspiration snippet is ag-grid-react + hooks + a step-pipeline; we wrap vanilla core and explicitly do NOT adopt that architecture — builders stay plain-map assoc. Full resolution in the ticket's notes.

## Not yet specified

- Spec assembly: format and home of the final design spec (docs/adr? single spec doc?) once decisions accumulate.
- Docs/cljdoc strategy for the curated builders (the generated kebab↔camel reference table is resolved — committed `docs/reference/ag-grid-options.md`, see the key-registry ticket).
- Testing story for the library (unit vs browser; what CI looks like) — data points so far: the skeleton's node-test contract suite plus per-ticket Playwright headless-Chromium checks (scripts kept out of the repo).
- Theming/CSS story (AG Grid Theming API vs CSS files; how the wrapper exposes it).

## Out of scope

- Shipping v0.1 (Clojars release engineering, versioning, CI publish) — separate effort once the way is clear.
- Full Malli/spec typed schema layer over the options surface — ruled out as a maintenance treadmill; key registry only.
# ag-grid-cljs — Design Spec

**io.github.unisoma/ag-grid-cljs** — an MIT-licensed, open-source, framework-agnostic ClojureScript wrapper for AG Grid (Community + Enterprise), released on Clojars. It wraps the vanilla `createGrid` core (never ag-grid-react); AG Grid npm packages are peer dependencies, never bundled; the library ships zero runtime dependencies (a cljs-bean slice is vendored). Toolchain floor: shadow-cljs ≥ 3.3.0 (ClojureScript 1.12.116+), AG Grid v34+.

This spec is an index: every decision's full record — rationale, rejected alternatives, evidence — lives in its ADR under [docs/adr/](adr/). The glossary is [CONTEXT.md](../CONTEXT.md).

## Architecture overview

A consumer builds a plain EDN **options map** — optionally through pure `with-*` **builders** ([ADR 0002](adr/0002-layered-api-shape.md), [ADR 0009](adr/0009-builder-catalog-v1.md)) — and passes it to `create-grid!`. The map crosses the **conversion boundary** ([ADR 0005](adr/0005-conversion-boundary.md)): kebab keys become camelCase, CLJS collections convert, everything else (theme objects, JS rows, `raw`-wrapped values) passes through untouched; callbacks receive lazy kebab-keyed beans and their returns are forward-converted. Every event handler and callback is just a function-valued `:on-*` / callback option under that one rule ([ADR 0010](adr/0010-event-callback-shape.md)).

`create-grid!` returns a **GridHandle**. Post-creation change flows through three explicit channels ([ADR 0004](adr/0004-update-model.md)): `update-grid!`, a PATCH/MERGE options differ classified by the registry's `:initial?` flag ([ADR 0008](adr/0008-options-diffing-semantics.md)); the transactional **data channel** `set-rows!` / `transact!` for row data, which is **JS-by-contract** and never diffed ([ADR 0003](adr/0003-row-data-js-by-contract.md)); and the raw GridApi via `grid-api` as escape hatch.

Dev-mode help comes from the generated **key registry** ([ADR 0007](adr/0007-key-registry-codegen.md)) — a goog.DEBUG-guarded literal emitted by a Node/ts-morph codegen tool under `tools/`, powering unknown-key/did-you-mean/deprecation warnings and the generated options reference.

Public namespaces ([ADR 0006](adr/0006-namespace-and-naming.md)): a fat `ag-grid-cljs.core`, satellites `render` and `react` for the three **renderer tiers** ([ADR 0011](adr/0011-cell-renderer-tiers.md)), and opt-in `enterprise` with `set-license-key!`; the consumer owns module registration via a thin `register!` ([ADR 0001](adr/0001-wrap-vanilla-core.md)). Internals live under `ag-grid-cljs.impl.*`.

V1 ships no framework adapters ([ADR 0012](adr/0012-no-framework-adapters-v1.md)), no theming code ([ADR 0013](adr/0013-theming-docs-only.md)); both are docs recipes. Docs are cljdoc-canonical with docstrings as the per-var contract ([ADR 0014](adr/0014-docs-cljdoc-strategy.md)). Testing splits node (pure/fakeable) from a committed Playwright-driven browser suite, one CI workflow ([ADR 0015](adr/0015-testing-and-ci.md)).

## Decision records

| ADR | Decision |
|---|---|
| [0001](adr/0001-wrap-vanilla-core.md) | Wrap vanilla ag-grid-community core, not ag-grid-react; Fulcro is the reference-consumer bar; consumer owns module registration; opt-in `enterprise` ns |
| [0002](adr/0002-layered-api-shape.md) | Layered API: EDN options map at bottom, curated builders on top, registry-powered dev warnings |
| [0003](adr/0003-row-data-js-by-contract.md) | Row data is JS by contract; cljs.proxy benchmarked and rejected (correctness failures, no render win) |
| [0004](adr/0004-update-model.md) | Update model: options diffing + transactional data channel + raw escape hatch; Fulcro bar proven with zero adapter code |
| [0005](adr/0005-conversion-boundary.md) | Conversion boundary contract: type-driven recursion, kebab→camel, lazy beans inbound, `raw` sole escape hatch, vendored cljs-bean slice |
| [0006](adr/0006-namespace-and-naming.md) | Namespaces `core`/`render`/`react`/`enterprise` + `impl.*`; `!` on side effects; `with-*` opts-first threadable builders; scaffold layout |
| [0007](adr/0007-key-registry-codegen.md) | Key registry: ts-morph codegen under `tools/`, goog.DEBUG-guarded literal, warn-never-reject, generated options reference |
| [0008](adr/0008-options-diffing-semantics.md) | `GridHandle` + `update-grid!` PATCH differ; `:initial?` sole classifier; `:column-defs` whole-value handoff; `:row-data` excluded |
| [0009](adr/0009-builder-catalog-v1.md) | Builder catalog v1: 8 entries; coerce-or-bundle admission bar; `with-default-col-def` dropped; recipes over stateful builders |
| [0010](adr/0010-event-callback-shape.md) | Events/callbacks: one rule — function-valued options under the conversion contract; `:on-*` keys; raw `grid-api` for imperative gaps |
| [0011](adr/0011-cell-renderer-tiers.md) | Three renderer tiers (bare fn / lifecycle map + `dom-renderer` / `react-renderer`); no wrapper DOM engine, no typed catalog |
| [0012](adr/0012-no-framework-adapters-v1.md) | No framework adapters in v1; future adapters wrap three already-public seams; one framework-composition recipe page |
| [0013](adr/0013-theming-docs-only.md) | No theming code in v1: `:theme` is an opaque JS value through the boundary; docs recipe covers default/params/dark/legacy |
| [0014](adr/0014-docs-cljdoc-strategy.md) | cljdoc canonical; docstrings are the per-var contract; six topical articles + generated options reference; no separate site |
| [0015](adr/0015-testing-and-ci.md) | Node suite for pure/fakeable, committed `:browser-test` + Playwright driver for runtime behavior; one CI workflow, unlicensed Enterprise smoke |

**Status:** v1 implemented; the ADRs are the source of truth for each decision's contract.

## Scope boundary

Consciously out of this effort; a future redraw of the destination starts fresh:

- **Shipping v0.1** — Clojars release engineering, versioning, CI publish.
- **A full Malli/spec typed schema layer** over the options surface — rejected as a maintenance treadmill; the key registry is the ceiling.
- **Framework adapter code** — Reagent/UIx renderer sugar and any reactive/full-state-declarative layer (ADR 0012 names the seams a future effort would wrap).

## Research notes

Sourced background under [docs/research/](research/) (version-pinned):

- [ag-grid-module-registry-esm-enterprise.md](research/ag-grid-module-registry-esm-enterprise.md) — module system, ESM packaging, licensing surface (feeds ADR 0001).
- [ag-grid-options-surface-extraction.md](research/ag-grid-options-surface-extraction.md) — options-surface extraction feasibility (feeds ADR 0007).
- [ag-grid-react-wrapper.md](research/ag-grid-react-wrapper.md) — ag-grid-react vs vanilla core analysis (corroborates ADRs 0001, 0008, 0011).

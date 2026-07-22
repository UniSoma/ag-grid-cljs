---
id: agd-01ky0ebxg01e
title: 'Map: ag-grid-cljs — spec + walking skeleton'
status: closed
type: epic
priority: 1
mode: hitl
created: '2026-07-20T18:59:42.208243308Z'
updated: '2026-07-22T18:13:01.513470341Z'
closed: '2026-07-22T18:13:01.513470341Z'
tags:
- wayfinder:map
---

## Destination

A locked design spec plus a walking skeleton for **io.github.unisoma/ag-grid-cljs** — an MIT-licensed, open-source, framework-agnostic ClojureScript wrapper for AG Grid (Community + Enterprise), vanilla-core based, with Fulcro as the reference-consumer bar. Done when every design decision is resolved, the skeleton proves the five risk points (core mount, Fulcro integration, CLJS cell renderer, cljs.proxy benchmark, Enterprise smoke test), and implementation could start as a fresh effort with no open questions.

**DESTINATION REACHED (2026-07-22).** The spec is durable in-repo: `CONTEXT.md` (glossary) + `docs/spec.md` (index) + `docs/adr/0001–0015` (self-contained decision records) + `docs/research/`. This map and its tickets are superseded by those files and may be deleted.

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

**2026-07-22T18:13:01.513470341Z**

Destination reached: locked design spec (CONTEXT.md + docs/spec.md + docs/adr/0001-0015 + docs/research/) plus walking skeleton with all five risk points retired. Implementation can start as a fresh effort with no open questions; the spec files supersede this map and its tickets.

## Decisions so far

All decisions are now durably recorded in `docs/adr/` (see `docs/spec.md` for the index); the per-ticket gists below remain for provenance while the tickets exist.

Settled during charting (no ticket — resolved in the charting grilling session; now ADRs 0001–0004 plus CONTEXT/spec):

- **Scope** — spec + walking skeleton; shipping v0.1 is a separate later effort.
- **Foundation** — wrap vanilla ag-grid-community core (createGrid), not ag-grid-react; Fulcro is the reference-consumer bar; Community + Enterprise both wrapped. → ADR 0001
- **API shape** — layered: plain EDN options map at bottom (kebab-case auto-translated to camelCase, full surface reachable via raw assoc); curated documented `with-*` builders on top; generated key registry powering dev-mode typo warnings and a kebab<->camel reference table. → ADR 0002
- **Row data** — JS by contract; cljs-bean/js-interop ergonomics at every callback boundary; cljs.proxy gets a measure-and-decide benchmark. → ADR 0003
- **Update model** — declarative options diffing + explicit transactional data channel (set-rows! / transact!) + raw grid API escape hatch. → ADR 0004
- **Coordinates** — io.github.unisoma/ag-grid-cljs, MIT, Clojars. → docs/spec.md

Resolved tickets:

- [Research: AG Grid module registry, ESM packaging, and Enterprise licensing surface](agd-01ky0eck3myz) — consumer owns module registration; thin optional `register!` + opt-in enterprise namespace with `set-license-key!`. → docs/research/ag-grid-module-registry-esm-enterprise.md, ADR 0001
- [Research: options-surface extraction from AG Grid TypeScript definitions](agd-01ky0eck6erk) — extraction feasible and cheap; per-version EDN registry recommended. → docs/research/ag-grid-options-surface-extraction.md, ADR 0007
- [Conversion boundary rules: what converts, what passes through untouched](agd-01ky0eck96vn) → ADR 0005
- [Walking skeleton: core mount from builder-produced EDN options](agd-01ky0ed83xww) — core design proven end to end; scaffold settled. → ADR 0006 (scaffold), docs/spec.md (skeleton status)
- [Walking skeleton: CLJS custom cell renderer helper](agd-01ky0ed8adbf) — three renderer tiers committed. → ADR 0011
- [Walking skeleton: Fulcro integration with transactional data updates](agd-01ky0ed8766f) — Fulcro bar proven with zero library support; data channel confirmed. → ADR 0004
- [Namespace layout and public API naming](agd-01ky0m0btmrp) → ADR 0006
- [Key registry: codegen pipeline and dev-warning design](agd-01ky0evm9nap) → ADR 0007
- [Options-diffing semantics: what is updatable and how changes apply](agd-01ky0edx8dzc) — GridHandle + update-grid! PATCH differ. → ADR 0008
- [Curated builder catalog v1](agd-01ky0edx5mfs) — 8-entry catalog, coerce-or-bundle bar. → ADR 0009
- [Event and callback API shape](agd-01ky0edx2wec) — one rule: function-valued options under the conversion contract. → ADR 0010
- [Reagent/UIx adapters: in scope or out?](agd-01ky157wpbpv) — ruled OUT for v1; docs recipe only. → ADR 0012
- [Walking skeleton: cljs.proxy outbound-bridge benchmark](agd-01ky0ed8d7k2) — cljs.proxy REJECTED as row strategy; 5th risk point retired. → ADR 0003
- [Walking skeleton: Enterprise smoke test (modules, license, range selection)](agd-01ky0ed8fw7v) — Enterprise proven through the wrapper; all skeleton risks closed. → ADR 0001, docs/spec.md (skeleton status)
- [Theming/CSS story: Theming API vs CSS files, wrapper exposure](agd-01ky55n5xn5c) — no theming code in v1; docs recipe. → ADR 0013
- [Docs/cljdoc strategy: builder docs and the framework-composition recipe page](agd-01ky55neshd2) — cljdoc canonical, docstring contracts, six topical articles. → ADR 0014
- [Testing story: unit vs browser split and CI shape](agd-01ky55n9xka3) — node/browser split rule, committed :browser-test + Playwright driver, single CI job. → ADR 0015
- [Spec assembly: format and home of the final design spec](agd-01ky55nrwypz) — spec assembled: CONTEXT.md glossary + docs/spec.md index + 15 self-contained ADRs + extracted research; skeleton is a scaffold to keep, drift list in spec.md; definition of done met.

## Not yet specified

(empty — destination reached; no fog remains.)

## Out of scope

- Shipping v0.1 (Clojars release engineering, versioning, CI publish) — separate effort once the way is clear.
- Full Malli/spec typed schema layer over the options surface — ruled out as a maintenance treadmill; key registry only.
- Framework adapter code — Reagent/UIx cell-renderer sugar (`reagent-renderer`/`uix-renderer` namespaces + optional deps) and a reactive/full-state-declarative layer. Ruled out of V1 (docs-only); returns only as a future effort wrapping the already-public seams. See [Reagent/UIx adapters: in scope or out?](agd-01ky157wpbpv).
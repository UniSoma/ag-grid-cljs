---
id: agd-01ky0ebxg01e
title: 'Map: ag-grid-cljs — spec + walking skeleton'
status: open
type: epic
priority: 1
mode: hitl
created: '2026-07-20T18:59:42.208243308Z'
updated: '2026-07-20T20:08:21.363861849Z'
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

## Not yet specified

- Spec assembly: format and home of the final design spec (docs/adr? single spec doc?) once decisions accumulate.
- Docs/cljdoc strategy for builders and the reference table.
- Testing story for the library (unit vs browser; what CI looks like).
- Theming/CSS story (AG Grid Theming API vs CSS files; how the wrapper exposes it).
- Reagent/UIx thin adapters — may end up out of scope; revisit after cell-renderer helper lands.
- Namespace layout and public API naming conventions for the library.

## Out of scope

- Shipping v0.1 (Clojars release engineering, versioning, CI publish) — separate effort once the way is clear.
- Full Malli/spec typed schema layer over the options surface — ruled out as a maintenance treadmill; key registry only.
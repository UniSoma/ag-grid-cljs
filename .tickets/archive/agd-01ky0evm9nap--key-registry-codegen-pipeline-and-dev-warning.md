---
id: agd-01ky0evm9nap
title: 'Key registry: codegen pipeline and dev-warning design'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:08:17.077520809Z'
updated: '2026-07-21T19:09:43.902677623Z'
closed: '2026-07-21T19:09:43.902677623Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
assignee: jonas
---

## Description

## Question

Given the extraction research (see closed ticket agd-01ky0eck6erk: GridOptions/event names are runtime constants in ag-grid-community; only ColDef needs a small ts-morph pass; recommended output is per-version EDN keyed by kebab keyword with :camel/:type/:deprecated/:doc plus an events block), decide the registry design: where the codegen script lives and when it runs (per AG Grid release); the exact EDN artifact shape shipped in the library; dev-mode warning behavior (unknown key -> warn with did-you-mean, never reject; deprecation warnings?); how warnings are elided from production builds; and how the kebab<->camel reference table is rendered for docs.

## Notes

**2026-07-21T19:09:36.288122557Z**

**2026-07-21T19:09:43.902677623Z**

Key registry design locked: (1) codegen emits a goog.DEBUG-guarded CLJS literal (DCE'd from prod), not a runtime .edn; (2) manual Node/ts-morph codegen under tools/, committed, single AG-Grid-version pin stamped as :ag-grid-version (deps.cljs can't express the peer dep — README+peerDependencies does that); (3) full rich per-key shape {:camel :type :default :initial? :deprecated :doc} at ag-grid-cljs.impl.registry; (4) position-aware dev validation (grid-options + known ColDef positions, opaque elsewhere), warn+did-you-mean never reject; (5) MIDDLE division of labor — wrapper owns kebab unknown-key+did-you-mean and kebab deprecation warnings, delegates type/dependency/row-model to AG Grid's ValidationModule (off by default v36+); (6) committed docs/reference/ag-grid-options.md emitted by the same codegen pass. Full detail in resolution note.

## Resolution: Key registry design

Six decisions, grounded in the extraction research (agd-01ky0eck6erk) and two research subagents run during this session (shadow-cljs deps.cljs semantics; AG Grid ValidationModule behavior).

### 1. Production elision (load-bearing)
Codegen emits a CLJS/CLJC **source file with the registry as a literal**, guarded by `^boolean goog.DEBUG`. `:advanced` + `:closure-defines {goog.DEBUG false}` dead-code-eliminates both the ~600-key data and the did-you-mean code. NOT a runtime-loaded .edn (a live io/resource ref can't be DCE'd). Implementation risk to verify on the skeleton: the top-level `def` of the big literal must be referenced ONLY from goog.DEBUG-guarded code so DCE actually drops it — confirm with a :pseudo-names / bundle-size check.

### 2. Codegen: when it runs, how many versions
- Language forced: Node script under `tools/`, pinning `typescript@5.x` (ColDef needs the TS compiler API via ts-morph; GridOptions/events come from `require('ag-grid-community')` runtime constants).
- **Manual, on AG Grid dependency bump, generated CLJS committed** — CLJS build/CI stays pure-Clojure, zero Node/TS dependency.
- **Single registry, pinned to one AG Grid version** (newest supported at release), version stamped in the artifact as `:ag-grid-version`. Churn is additive-only, so lag only yields false 'unknown option' warnings for brand-new keys, never false acceptance.
- Version-pin home: the codegen tool stamps `:ag-grid-version` directly (it knows what it extracted) — that's the authority. deps.cljs is NOT the install mechanism for the peer dep: research showed `:npm-deps` is a single-version auto-installed pin with no peer/range semantic and first-wins conflict resolution. The peer requirement (ag-grid-community >= 34) is documented in README + package.json `peerDependencies`. deps.cljs may optionally mirror the version as readable EDN, but is not load-bearing.

### 3. Registry shape: FULL RICH
Per-key entry `{:camel :type :default :initial? :deprecated :doc}`; blocks `:grid-options / :col-def / :col-group-def / :events`; top-level `:ag-grid-version`. Generated file at `ag-grid-cljs.impl.registry` (internals ns per the naming ticket). Rationale: the runtime kebab->camel conversion is the deterministic algorithm (per closed ticket agd-01ky0eck96vn: all 726 keys round-trip, zero collisions), so the registry is dev-warnings + docs authority ONLY and DCE's away entirely — richness costs only dev-bundle/repo size. `:initial?` additionally feeds the options-diffing ticket (agd-01ky0edx8dzc: what is updatable vs init-only).

### 4. Validation scope: POSITION-AWARE (B)
Dev-only validation runs in the conversion walk, on kebab keys, before camelization. Validate top-level GridOptions against `:grid-options`; recurse into KNOWN ColDef-bearing positions (`:column-defs` items, `:default-col-def`, `:auto-group-column-def`) against `:col-def`; treat everything else (`:cell-renderer-params`, `:context`, callback params) as OPAQUE — never validate. Policy: unknown key -> warn with did-you-mean, NEVER reject. (Rejected: shallow-only (misses common ColDef typos) and validate-union (false-warns on user data).)

### 5. Division of labor vs AG Grid's ValidationModule: MIDDLE
Research confirmed AG Grid's ValidationModule does unknown+fuzzy-did-you-mean, type checks, deprecation-with-replacement, module-missing, and dependency/row-model rules — BUT only on post-conversion camelCase (its did-you-mean is camelCase, useless to a kebab user; can't tell typo from raw passthrough), is opt-in and OFF by default from v36+, and silent in production.
Wrapper validator does: **unknown-key + kebab did-you-mean** (the strictly-unique layer) **and kebab-native deprecation warnings** (cheap `(get deprecated k)` lookup with replacement note; AG Grid's is off-by-default on v36). Wrapper does NOT reimplement type-checking / dependency / row-model — delegate to ValidationModule. Warnings -> `js/console.warn`, deduped once per `[object-name key]` via a dev-only warned-set atom. Expose a tiny `enable-dev-validations!` helper + document registering ValidationModule in dev for the deeper checks.

### 6. Docs reference table: committed Markdown from the codegen tool
Same Node codegen pass emits a committed `docs/reference/ag-grid-options.md` (regenerated on AG Grid bump, never hand-edited): single file, H2 sections (Grid Options / Column Definitions / Column Groups / Events), columns `kebab | camelCase | type | init-only? | deprecated -> replacement | description`. Optional dev-only REPL helper `(registry/reference-table)` noted, not built now. Broader docs/cljdoc strategy for builders remains map fog.

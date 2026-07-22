# 0007. Key registry: codegen pipeline and dev-warning design

- Status: accepted, 2026-07-21
- Origin: knot ticket agd-01ky0evm9nap (tickets are ephemeral; this record is self-contained)

The generated key registry (the assist layer of ADR 0002) is a goog.DEBUG-guarded CLJS literal emitted by a manual Node/ts-morph codegen tool under `tools/`, pinned to a single AG Grid version, with a full rich per-key shape. Dev-mode validation is position-aware, warns with did-you-mean and never rejects, and splits responsibilities with AG Grid's own ValidationModule. The same codegen pass emits a committed Markdown reference table.

## Context

ADR 0002 committed to a generated registry of known option keys powering dev-mode typo warnings and a kebab<->camel reference table. This record decides the registry's design: where the codegen lives and when it runs, the exact artifact shape shipped in the library, dev-mode warning behavior, how warnings are elided from production builds, and how the reference table is rendered for docs.

Extraction feasibility research (docs/research/ag-grid-options-surface-extraction.md) established the ground truth:

- GridOptions keys and event names are **runtime constants** in `ag-grid-community` — extractable via `require('ag-grid-community')`, no TypeScript tooling needed.
- Only **ColDef** requires a small ts-morph pass over the TypeScript compiler API.
- Churn across AG Grid majors is **additive**, roughly 8–20 keys per major release.
- All **726 keys round-trip kebab<->camel with zero collisions**, so the runtime kebab->camel conversion (ADR 0005) is a deterministic algorithm that never consults the registry — the registry is dev-warnings + docs authority only.

Two further research findings shaped the design:

- **shadow-cljs `deps.cljs` semantics**: `:npm-deps` is a single-version auto-installed pin with no peer/range semantic and first-wins conflict resolution — it cannot express "peer dependency, ag-grid-community >= 34".
- **AG Grid's ValidationModule** does unknown-key checks with fuzzy did-you-mean, type checks, deprecation-with-replacement, module-missing, and dependency/row-model rules — but only on post-conversion camelCase (its did-you-mean suggests camelCase names, useless to a kebab user, and it cannot tell a typo from a raw passthrough), is opt-in and OFF by default from v36+, and is silent in production.

## Decision

Six decisions:

### 1. Production elision (load-bearing)

Codegen emits a CLJS/CLJC **source file with the registry as a literal**, guarded by `^boolean goog.DEBUG`. `:advanced` compilation plus `:closure-defines {goog.DEBUG false}` dead-code-eliminates both the ~600-key data and the did-you-mean code from production builds. NOT a runtime-loaded `.edn` — a live `io/resource` reference cannot be DCE'd. Implementation risk to verify on the skeleton: the top-level `def` of the big literal must be referenced ONLY from goog.DEBUG-guarded code so DCE actually drops it — confirm with a `:pseudo-names` / bundle-size check.

### 2. Codegen: when it runs, how many versions

- Language forced: **Node script under `tools/`**, pinning `typescript@5.x` (ColDef needs the TS compiler API via ts-morph; GridOptions/events come from `require('ag-grid-community')` runtime constants).
- **Manual, run on AG Grid dependency bump, generated CLJS committed** — the CLJS build and CI stay pure-Clojure, zero Node/TS dependency.
- **Single registry, pinned to one AG Grid version** (newest supported at release), version stamped in the artifact as `:ag-grid-version`. Churn is additive-only, so lagging the pin only yields false "unknown option" warnings for brand-new keys, never false acceptance.
- Version-pin home: the codegen tool stamps `:ag-grid-version` directly (it knows what it extracted) — that is the authority. `deps.cljs` is NOT the install mechanism for the peer dep: `:npm-deps` has no peer/range semantic. The peer requirement (ag-grid-community >= 34) is documented in the README and in package.json `peerDependencies`. `deps.cljs` may optionally mirror the version as readable EDN, but is not load-bearing.

### 3. Registry shape: full rich

Per-key entry `{:camel :type :default :initial? :deprecated :doc}`; blocks `:grid-options` / `:col-def` / `:col-group-def` / `:events`; top-level `:ag-grid-version`. Generated file lives at `ag-grid-cljs.impl.registry` (internals namespace). Rationale: since the runtime kebab->camel conversion is the deterministic algorithm (all 726 keys round-trip, zero collisions), the registry is dev-warnings + docs authority ONLY and DCE's away entirely — richness costs only dev-bundle and repo size. `:initial?` additionally feeds the options-diffing contract (ADR 0008), which uses it as the sole updatable-vs-initial-only classifier.

### 4. Validation scope: position-aware

Dev-only validation runs in the conversion walk (ADR 0005), on kebab keys, before camelization. Validate top-level GridOptions against `:grid-options`; recurse into KNOWN ColDef-bearing positions (`:column-defs` items, `:default-col-def`, `:auto-group-column-def`) against `:col-def`; treat everything else (`:cell-renderer-params`, `:context`, callback params) as OPAQUE — never validate. Policy: unknown key -> warn with did-you-mean, NEVER reject (per ADR 0002's open-surface guarantee).

### 5. Division of labor vs AG Grid's ValidationModule: middle

The wrapper validator does **unknown-key + kebab did-you-mean** (the strictly-unique layer AG Grid cannot provide) **and kebab-native deprecation warnings** (a cheap `(get deprecated k)` lookup with replacement note; AG Grid's own deprecation warnings are off by default on v36+). The wrapper does NOT reimplement type-checking, option-dependency, or row-model rules — those are delegated to AG Grid's ValidationModule. Warnings go to `js/console.warn`, deduped once per `[object-name key]` via a dev-only warned-set atom. Expose a tiny `enable-dev-validations!` helper and document registering ValidationModule in dev for the deeper checks.

### 6. Docs reference table: committed Markdown from the codegen tool

The same Node codegen pass emits a committed `docs/reference/ag-grid-options.md` (regenerated on AG Grid bump, never hand-edited): a single file, H2 sections (Grid Options / Column Definitions / Column Groups / Events), columns `kebab | camelCase | type | init-only? | deprecated -> replacement | description`. An optional dev-only REPL helper `(registry/reference-table)` was noted but not built.

## Consequences

- Production builds carry zero registry weight; the DCE claim must be verified on the walking skeleton with a bundle-size check before it can be trusted.
- The registry can lag AG Grid releases safely: the failure mode is a spurious dev warning on a genuinely new key, never rejection or false acceptance.
- Consumers who want type/dependency/row-model validation must opt in to AG Grid's ValidationModule in dev; the wrapper only tells them so, it does not wrap those checks.

## Considered options

- **Runtime-loaded `.edn` registry** — rejected: a live resource reference cannot be dead-code-eliminated, so production builds would carry the full ~600-key table.
- **Shallow-only validation** (top-level GridOptions only) — rejected: misses common ColDef typos, the most frequent option-writing surface.
- **Validate-union** (check every nested map against the union of all key blocks) — rejected: false-warns on user data in opaque positions like `:cell-renderer-params` and `:context`.
- **Reimplementing type/dependency/row-model checks in the wrapper** — rejected: AG Grid's ValidationModule already does these; the wrapper adds only what is kebab-specific.
- **`deps.cljs` as the peer-dependency mechanism** — rejected: `:npm-deps` is a single-version pin with first-wins conflict resolution and no peer/range semantic.

## References

- ADR 0002 — layered API shape (registry is the assist layer; warn-never-reject policy)
- ADR 0005 — conversion boundary (validation runs in the conversion walk, pre-camelization)
- ADR 0008 — options-diffing semantics (consumes the registry's `:initial?` flag)
- ADR 0014 — docs/cljdoc strategy (broader docs home for the generated reference table)
- docs/research/ag-grid-options-surface-extraction.md — extraction feasibility research

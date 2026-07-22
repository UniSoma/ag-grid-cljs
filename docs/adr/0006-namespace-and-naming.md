# 0006. Namespace layout and public API naming

- Status: accepted, 2026-07-21
- Origin: knot tickets agd-01ky0m0btmrp, agd-01ky0ed83xww (tickets are ephemeral; this record is self-contained)

The library lives under root `ag-grid-cljs` with a fat `core` namespace plus `render`, `react`, and opt-in `enterprise` satellites; internals sit under `ag-grid-cljs.impl.*`; `!` marks side-effecting functions; builders are `with-<thing>`, opts-first, `->`-threadable. The repo scaffold is deps.edn with `src/main` library paths, a `src/dev` dev app, and shadow-cljs builds driven by `npm run dev` / `npm test`.

## Context

The walking skeleton established provisional names to react against: `ag-grid-cljs.core` (`raw`, `register!`, `options`, `with-*`, `create-grid`) and `ag-grid-cljs.impl.convert` (plus the future vendored `ag-grid-cljs.impl.bean`). Open questions: root segment (`ag-grid-cljs` vs `unisoma.ag-grid` vs other), one core namespace vs split public namespaces (including a separate enterprise namespace per the module-registry decision), the `impl.*` convention, and naming rules for builders and API functions (`with-*` prefix, bang conventions for `register!`/`set-license-key!`). The skeleton ticket also settled the library repo scaffold (deps.edn/package.json layout, dev app location). Publishing coordinates are `io.github.unisoma/ag-grid-cljs` on Clojars, MIT-licensed.

## Decision

### Root segment

`ag-grid-cljs` — matches the artifact name; an idiomatic short root; always aliased in practice, so collision risk is low. Org-scoped (`unisoma.ag-grid`) and Maven-matched roots were rejected as buying little over an aliased short root.

### Public namespaces — fat core + satellites

- `ag-grid-cljs.core` — setup + builders + runtime API: `register!`, `raw`, `options`, `with-*`, `create-grid!`, `destroy!`, `set-rows!`, `transact!`
- `ag-grid-cljs.render` — `renderer`, `dom-renderer`
- `ag-grid-cljs.react` — `react-renderer` (isolates the React dependency from grid-only users)
- `ag-grid-cljs.enterprise` — opt-in; `set-license-key!` + enterprise-only helpers (name mirrors AG Grid's Community/Enterprise vocabulary, per the module-registry decision)

Rejected: splitting builders into their own namespace, and a single-facade namespace re-exporting everything.

### Internals

Everything internal lives under `ag-grid-cljs.impl.*` (`impl.convert`, future `impl.bean`). `.impl` is the committed "private, may change" marker; nothing outside `.impl` is off-limits to consumers.

### Naming rules

- **`!` marks any side-effecting function** (DOM mutation, global registration, live-grid mutation): `create-grid!`, `register!`, `destroy!`, `set-rows!`, `transact!`, `set-license-key!`. Pure map-builders and accessors stay bang-free: `options`, `raw`, all `with-*`.
- **Builders:** every curated builder is `with-<thing>`, takes opts first, returns opts, and is `->`-threadable. (Catalog contents are ADR 0009's concern.)

Known skeleton drift: the walking skeleton still has `create-grid`; it is to be renamed `create-grid!` in `ag-grid-cljs.core` at implementation.

### Repo scaffold (settled by the walking skeleton)

- **deps.edn**: `:paths ["src/main"]`; a `:dev` alias adds `src/dev` + `src/test` and `thheller/shadow-cljs`.
- **package.json**: shadow-cljs and ag-grid-community as devDependencies of the dev harness; consumers own ag-grid-community as a peer.
- **shadow-cljs.edn**: `:deps {:aliases [:dev]}`; a `:dev-app` browser build served from `src/dev/public` on port 8080; a `:test` node-test build.
- **Dev app location**: `src/dev/ag_grid_cljs/dev/app.cljs` + `src/dev/public/index.html`.
- **Commands**: `npm run dev` / `npm test`.

### Publishing

Coordinates `io.github.unisoma/ag-grid-cljs`, MIT license, published to Clojars.

## Consequences

- Grid-only consumers never pull the React dependency; Enterprise stays invisible unless `ag-grid-cljs.enterprise` is required.
- The `.impl` boundary is the only stability contract needed: anything outside it is public API, anything under it may change without notice.
- The bang convention makes the pure/effectful split visible at every call site; threading builders through `->` reads as the canonical options-construction style.

## Considered options

- **Root `unisoma.ag-grid` or a Maven-matched root** — rejected: buys little over an always-aliased short root.
- **Splitting builders into their own namespace** — rejected in favor of the fat core.
- **Single-facade namespace re-exporting everything** — rejected.

## References

- ADR 0002 — layered API shape (options map + `with-*` builders this layout hosts)
- ADR 0005 — conversion boundary (`raw`, `impl.convert`, vendored `impl.bean`)

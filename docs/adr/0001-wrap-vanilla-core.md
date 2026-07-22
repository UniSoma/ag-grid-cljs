# 0001. Wrap vanilla AG Grid core, not ag-grid-react

- Status: accepted, 2026-07-22 (foundation itself settled 2026-07-20)
- Origin: charting session of the wayfinder map plus knot tickets agd-01ky0eck3myz, agd-01ky0ed8fw7v (tickets are ephemeral; this record is self-contained)

ag-grid-cljs wraps the vanilla `ag-grid-community` core (`createGrid`) directly — not `ag-grid-react`. Both Community and Enterprise are wrapped; the consumer owns module registration, and the wrapper core depends only on `ag-grid-community`, with Enterprise concerns isolated in an opt-in namespace.

## Context

AG Grid ships two candidate integration surfaces: the vanilla JS core (`createGrid` in `ag-grid-community`) and the official React wrapper (`ag-grid-react`). Research into the React wrapper (docs/research/ag-grid-react-wrapper.md) found that most of `ag-grid-react`'s value is React-idiomatic plumbing, not grid functionality:

- The reconciliation-critical machinery — identity-based row diffing via `getRowId`, immutable-data reference comparison, and `applyTransaction` — lives entirely in the vanilla core. A ClojureScript wrapper inherits it for free by feeding data through core APIs.
- The wrapper's genuine additions are narrow: the ReactUI rendering engine that mounts React components into grid DOM, the declarative `reactiveCustomComponents` contract, and a prop→grid bridge that re-applies every prop each render — the last of which is redundant and mildly hostile to a normalized-DB model like Fulcro's.
- ReactUI uses one private internal React root with no public injection point, so reproducing ReactUI-style direct mounting is not viable from outside; plain-DOM renderers driven by the vanilla core are the safe default (see ADR 0011 cell-renderer-tiers).

Fulcro is the reference-consumer bar: a proof target the wrapper must work well under, not shipped code (see ADR 0012 no-framework-adapters-v1).

On the module/packaging side (research captured in docs/research/ag-grid-module-registry-esm-enterprise.md), the findings the decision rests on:

- Since v33, all legacy `@ag-grid-community/**` / `@ag-grid-enterprise/**` packages were removed; every feature module lives in just two packages, `ag-grid-community` and `ag-grid-enterprise`. Registration is global via `ModuleRegistry.registerModules([...])` (or per-grid via `:modules` grid params) and must run before any grid is instantiated; a row-model module is mandatory.
- `AllCommunityModule` / `AllEnterpriseModule` reproduce pre-v33 "everything works" behavior but prevent tree-shaking; selective module imports save roughly 20–40% of bundle size.
- Each package ships one bundled ESM file (~1 MB minified each), so the saving comes entirely from the consumer's bundler doing export-level dead-code elimination driven by which module symbols are imported. Under default shadow-cljs npm bundling there is no npm tree-shaking at all — selective registration gives correctness/clarity but no size benefit. Capturing the saving requires an external ESM-aware bundler (`:target :esm` / `:js-provider :external`, `:entry-keys` preferring `"module"`).
- Enterprise licensing: `LicenseManager.setLicenseKey` (imported from `ag-grid-enterprise`) must be called before any grid is instantiated; no separate license module needs registering. Without a valid key, Enterprise features still work, producing only a watermark and console warnings. Any import from `ag-grid-enterprise` pulls the ~1 MB enterprise bundle into the build, so licensing/enterprise concerns must stay out of the wrapper's core namespace.

Module registration is a one-time, app-init, global side effect — exactly what a library should not perform implicitly on namespace load — and the wrapper cannot know which modules an app needs; tree-shaking depends on the consumer's own JS imports and build pipeline.

## Decision

ag-grid-cljs wraps the vanilla `ag-grid-community` core (`createGrid`) directly — NOT `ag-grid-react`. Fulcro is the reference-consumer bar: a proof target, not shipped code. Both Community and Enterprise are wrapped.

Module/Enterprise posture:

- The consumer owns module registration. The wrapper core depends only on `ag-grid-community` and exposes a thin optional `register!` (a trivial wrapper over `ModuleRegistry.registerModules` accepting the consumer's imported module objects), plus per-grid `:modules` passthrough in grid params.
- An opt-in `ag-grid-cljs.enterprise` namespace provides `set-license-key!` (wrapping `LicenseManager.setLicenseKey`), documented as "call before creating any grid". The core never imports `ag-grid-enterprise`, so Community-only consumers pay nothing.
- In development, the license key is injected at compile time via a macro reading the `AG_GRID_LICENSE` environment variable; the key is never committed (it lands only in gitignored build output; with the env var unset, e.g. on CI, the macro expands to `""`).
- AG Grid npm packages are peer dependencies, never bundled by the wrapper. Selective modules save ~20–40% bundle only with an external ESM-aware bundler.

Toolchain floor: shadow-cljs >= 3.3.0 (ClojureScript 1.12.116+), AG Grid v34+.

## Consequences

- Enterprise is proven end-to-end through the wrapper (headless-Chromium probe, both license states): registering `AllEnterpriseModule` via `register!`, setting a license via `set-license-key!`, and exercising cell range selection + fill handle through the builder API (`with-cell-selection`, see ADR 0009 builder-catalog-v1) works identically with and without a license — the no-license path only logs AG Grid's "License Key Not Found" console error; nothing throws or degrades. The wrapper imposes nothing that blocks Enterprise.
- Under default shadow-cljs settings, consumers ship the whole ~1 MB community bundle regardless of which modules they register. Documentation should show `AllCommunityModule` registration (plus `ValidationModule` in dev builds — it is excluded from the All bundles), and link AG Grid's module selector for production slimming with the external-bundler caveat.
- Custom cell content cannot reuse ReactUI's mounting; renderer strategy is layered separately (ADR 0011).
- Data flows through core APIs (`getRowId`, `applyTransaction`), which map cleanly onto delta-producing state models (see ADR 0003 row-data-js-by-contract, ADR 0004 update-model).

## Considered options

**Wrap `ag-grid-react` (rejected).** This was the real alternative: ride the official React wrapper and its ReactUI component mounting. Rejected because its reconciliation value is illusory (that machinery is vanilla-core), its prop→grid bridge re-applies every prop each render — redundant and mildly hostile to Fulcro's normalized-DB update model — and its one genuine asset, ReactUI cell mounting, is a private internal React root with no public injection point, so a CLJS wrapper could not reuse it anyway. Wrapping it would also add a React dependency to a wrapper whose core should stay framework-free (ADR 0012). Full analysis in docs/research/ag-grid-react-wrapper.md.

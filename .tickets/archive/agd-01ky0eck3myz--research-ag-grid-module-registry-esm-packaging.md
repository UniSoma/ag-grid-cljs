---
id: agd-01ky0eck3myz
title: 'Research: AG Grid module registry, ESM packaging, and Enterprise licensing surface'
status: closed
type: task
priority: 1
mode: afk
created: '2026-07-20T19:00:04.340159261Z'
updated: '2026-07-20T19:04:51.129821128Z'
closed: '2026-07-20T19:04:51.129821128Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:research
assignee: jonas
---

## Description

## Question

What must the wrapper expose for AG Grid v34's module system and Enterprise licensing? Specifically: how ModuleRegistry.registerModules works across ag-grid-community/ag-grid-enterprise ESM packages; which modules map to which features (and what AllCommunityModule/AllEnterpriseModule cost in bundle size); how LicenseManager.setLicenseKey is applied and when; interaction with shadow-cljs tree-shaking; and whether the wrapper should own module registration (e.g. a register! fn) or leave it to the consumer. Primary sources: AG Grid docs and package source.

## Notes

**2026-07-20T19:04:41.235782214Z**

# Research: AG Grid v34 module registry, ESM packaging, Enterprise licensing — findings

**2026-07-20T19:04:51.129821128Z**

v33+ collapsed everything into ag-grid-community/ag-grid-enterprise; ModuleRegistry.registerModules (global) or per-grid :modules must run before first grid, with a row-model module mandatory. AllCommunity/AllEnterpriseModule prevent tree-shaking (selective imports save ~20-40%), but under default shadow-cljs npm bundling there is no npm tree-shaking at all — an external bundler (:target :esm / :js-provider :external, entry-keys preferring "module") is needed to realize savings. LicenseManager.setLicenseKey (import from ag-grid-enterprise) must be called before grid instantiation; missing key = watermark + console warnings. Recommendation: consumer owns module selection; wrapper core depends only on ag-grid-community and exposes a thin optional register! helper plus an opt-in enterprise namespace with set-license-key!.

## 1. Module system in v33/v34 (current architecture)

- Since v33.0, all legacy `@ag-grid-community/**` / `@ag-grid-enterprise/**` npm packages were **removed**; every feature module now lives in just two packages, `ag-grid-community` and `ag-grid-enterprise`. Tree-shaking no longer requires separate packages. (Source: https://www.ag-grid.com/javascript-data-grid/upgrading-to-ag-grid-33/)
- Registration is global via `ModuleRegistry.registerModules([...])`, importing modules by name from either package, e.g.:

  ```js
  import { ModuleRegistry, ClientSideRowModelModule, CsvExportModule } from 'ag-grid-community';
  import { ExcelExportModule, MasterDetailModule } from 'ag-grid-enterprise';
  ModuleRegistry.registerModules([ClientSideRowModelModule, CsvExportModule, ExcelExportModule, MasterDetailModule]);
  ```

  and the docs require that "this is done **before any grids are instantiated**". (Source: https://www.ag-grid.com/javascript-data-grid/modules/)
- Alternative: **per-grid** registration by passing `{ modules: [...] }` as grid params (`createGrid(el, gridOptions, { modules: [...] })`), useful when different grids need different features / for code splitting. (Source: https://www.ag-grid.com/javascript-data-grid/modules/)
- A **row model module is mandatory** (usually `ClientSideRowModelModule`; others: Infinite, ServerSide, Viewport). (Source: https://www.ag-grid.com/javascript-data-grid/modules/)

## 2. Module → feature mapping and bundle-size cost

- `AllCommunityModule` = every community module; `AllEnterpriseModule` = AllCommunity + every enterprise module. They reproduce pre-v33 "everything works" behavior. (Source: https://www.ag-grid.com/javascript-data-grid/modules/)
- Cost: "Using the AllCommunityModule / AllEnterpriseModule will prevent tree shaking"; selective module imports give a bundle-size reduction of roughly **20–40%** depending on features used. (Sources: https://www.ag-grid.com/javascript-data-grid/upgrading-to-ag-grid-33/, https://blog.ag-grid.com/whats-new-in-ag-grid-33/)
- Feature mapping is fine-grained (one module per feature area): community e.g. `ClientSideRowModelModule`, `TextFilterModule` / `NumberFilterModule` / `DateFilterModule`, `CsvExportModule`, `PaginationModule`, `RowSelectionModule`, `ValidationModule`; enterprise e.g. `ServerSideRowModelModule`, `CellSelectionModule` (the v33+ name for range selection), `ClipboardModule`, `ExcelExportModule`, `MasterDetailModule`, `RowGroupingModule`, `PivotModule`, `TreeDataModule`, `SetFilterModule`, `SideBarModule`, `StatusBarModule`, `IntegratedChartsModule`, `SparklinesModule`. The docs host an interactive **module selector** that generates the exact registration code per selected feature — the authoritative mapping, so the wrapper should link to it rather than duplicate the table. (Source: https://www.ag-grid.com/javascript-data-grid/modules/)
- `ValidationModule` (dev-time config error messages) is **not included** in the current All bundles — docs: "It is not included in the AllCommunityModule/AllEnterpriseModule bundles, so opt into it yourself", and "we recommend doing so only in development builds to keep production bundles small". (Source: https://www.ag-grid.com/react-data-grid/modules/)
- Raw package sizes (v34.3.1, single-file bundles): `ag-grid-community` `main.esm.min.mjs` **1.15 MB**, `ag-grid-enterprise` `main.esm.min.mjs` **1.03 MB** (enterprise additionally depends on community). (Source: https://app.unpkg.com/ag-grid-community@34.3.1/files/dist/package, https://app.unpkg.com/ag-grid-enterprise@34.3.1/files/dist/package)

## 3. ESM packaging

- Each package ships **one bundled file per format**, not many small files: `main` → `./dist/package/main.cjs.js`, `module` → `./dist/package/main.esm.mjs`, plus min variants; `sideEffects` lists only CSS files (JS is side-effect-free, so bundlers may tree-shake unused exports **within** the single ESM file). (Source: https://unpkg.com/ag-grid-community@34/package.json)
- Consequence: the 20–40% saving comes entirely from the consumer's bundler doing export-level dead-code elimination on that one ESM file, driven by which module symbols are imported.

## 4. Enterprise licensing surface

- `import { LicenseManager } from 'ag-grid-enterprise'; LicenseManager.setLicenseKey('KEY');` — must run **before any grid is instantiated**; docs pair it directly with registering an enterprise module (`ModuleRegistry.registerModules([AllEnterpriseModule])` in the example). No separate "license module" needs registering — importing `LicenseManager` and calling `setLicenseKey` suffices. (Source: https://www.ag-grid.com/javascript-data-grid/license-install/)
- Without a valid key, enterprise features still work but produce a **watermark and console warnings**. (Source: https://www.ag-grid.com/javascript-data-grid/license-install/)
- Note: any import from `ag-grid-enterprise` pulls the enterprise bundle into the build, so licensing/enterprise concerns must stay out of the wrapper's core namespace.

## 5. Interaction with shadow-cljs tree-shaking

- shadow-cljs's default npm handling (`:js-provider :shadow`) does **not** tree-shake npm dependencies: Closure `:advanced` DCE applies to CLJS code only, and CommonJS npm code is "simply impossible to tree-shake"; thheller's recommendation is to use ESM and/or delegate JS bundling to an external bundler. (Sources: https://github.com/thheller/shadow-cljs/issues/21, https://github.com/thheller/shadow-cljs/issues/412, https://clojurians-log.clojureverse.org/shadow-cljs/2023-06-23)
- Practical implication for AG Grid: because the package is a single ~2 MB file per format, under default shadow-cljs settings **selective module registration gives correctness/clarity but no bundle-size benefit** — the whole `main.(cjs|esm)` file ships regardless of which modules are registered.
- To actually capture the 20–40% saving from a CLJS app: use `:target :esm` (or `:js-provider :external`) and let webpack/esbuild bundle the JS side with tree-shaking, and set `:js-options {:entry-keys ["module" "browser" "main"]}` so the ESM entry is resolved instead of the CJS one. (Sources: https://github.com/thheller/shadow-cljs/blob/master/doc/esm.md, https://shadow-cljs.github.io/docs/UsersGuide.html, https://deh.li/2022/11/04/shadow-cljs-webpack.html)

## 6. Recommendation: who owns module registration

**Leave module selection and registration ownership with the consumer; the wrapper provides thin optional helpers.**

- Registration is a one-time, app-init, before-first-grid concern with global effect — exactly the kind of side effect a library should not perform implicitly on namespace load.
- If the wrapper itself `require`d `ag-grid-enterprise` (e.g. to auto-register `AllEnterpriseModule` or expose `LicenseManager` from core), every community-only consumer would ship the ~1 MB enterprise bundle. The wrapper core must depend only on `ag-grid-community`.
- Tree-shaking benefits depend on the consumer's own JS import statements and build pipeline (external bundler under shadow-cljs); the wrapper cannot know which modules the app needs.

Concretely:
1. Core namespace: depend on `ag-grid-community` only; expose `(register! modules)` — a trivial wrapper over `ModuleRegistry.registerModules` accepting the consumer's imported module objects — plus optional per-grid `:modules` passthrough in grid params.
2. Separate `...enterprise` namespace (opt-in require): `(set-license-key! key)` wrapping `LicenseManager.setLicenseKey`, documented as "call before creating any grid".
3. Quick-start docs: show `AllCommunityModule` registration + `ValidationModule` in dev builds; link the AG Grid module selector for production slimming, with the shadow-cljs caveat from §5.

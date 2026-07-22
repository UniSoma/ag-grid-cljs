# Third-party notices

ag-grid-cljs is distributed under its own license. It ships **zero runtime
dependencies** beyond the AG Grid peer packages (ADR 0005 §8). The one piece of
third-party code baked into the shipped library is a vendored slice of
cljs-bean, noted below.

## cljs-bean

- **Origin:** https://github.com/mfikes/cljs-bean, version 1.9.0
- **Author:** Mike Fikes and contributors
- **License:** Eclipse Public License 1.0 (EPL-1.0) — https://opensource.org/licenses/EPL-1.0
- **What is vendored:** the `cljs-bean.core` essentials (the lazy `bean` map
  view with `:prop->key` / `:key->prop` / `:recursive` support, plus `object`)
  and its private helper namespace `cljs-bean.from.cljs.core`. The `transit`
  namespace is not vendored.
- **Where:** `src/main/ag_grid_cljs/impl/bean.cljs` (from `cljs-bean.core`) and
  `src/main/ag_grid_cljs/impl/bean/from_core.cljs` (from
  `cljs-bean.from.cljs.core`).
- **Modifications:** the namespaces were renamed to `ag-grid-cljs.impl.bean` and
  `ag-grid-cljs.impl.bean.from-core` (to avoid a version clash with consumers
  who use cljs-bean directly), and both got `^:no-doc`. The code bodies are
  otherwise unchanged from upstream.
- **License notices:** `from_core.cljs` carries its upstream EPL-1.0 header
  verbatim (retained). `cljs-bean.core` ships with no per-file header upstream,
  so an EPL-1.0 notice was added at the top of `bean.cljs`.

EPL-1.0 is a file-scoped license: carrying these notices on the vendored files
satisfies its terms without changing the license of the rest of the project.

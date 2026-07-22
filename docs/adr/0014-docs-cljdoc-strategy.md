# 0014. Documentation strategy: cljdoc canonical, docstring contracts, six topical articles

- Status: accepted, 2026-07-22
- Origin: knot ticket agd-01ky55neshd2 (tickets are ephemeral; this record is self-contained)

cljdoc is the canonical documentation site: Markdown articles committed under `docs/`, tree curated via `doc/cljdoc.edn`, docstrings rendered as the API reference. Docstrings are the per-var contract for the 8 builders and the runtime fns; six topical articles carry the recipes earlier decisions assigned to docs; the generated `docs/reference/ag-grid-options.md` joins the tree as a machine-generated article.

## Context

Several earlier decisions assigned their deliverables to "the docs" without fixing a home: the pending-rows and range-fill batch-flush recipes and the data-type-definitions and built-in-renderers-by-name prose (ADR 0009, ADR 0011), the set-rows!-from-db consumer pattern, the framework-composition recipe page (ADR 0012), and the theming recipe (ADR 0013). The strategy had to decide how the 8 curated builders are documented (articles? README? docstrings?), where those prose homes live, how the codegen-emitted `docs/reference/ag-grid-options.md` (ADR 0007) is surfaced, and what the doc set must contain for the design spec to count as complete.

## Decision

1. **Architecture — cljdoc is canonical.** README = pitch + quickstart; cljdoc = the documentation site (Markdown articles committed under `docs/`, tree curated via `doc/cljdoc.edn`, docstrings rendered as API reference). No separate docs site (no GitHub Pages/mkdocs). cljdoc builds on release only — during the design phase the articles are GitHub-readable Markdown, which is fine since shipping is out of scope.

2. **Builders — docstrings are the canonical contract; no builders-catalog article.** Each of the 8 builders AND the runtime fns (`create-grid!`, `set-rows!`, `transact!`, `update-grid!`, `destroy!`, `grid-api`, `raw`, `register!`, `set-license-key!`) gets a full docstring stating: what it coerces/bundles (the catalog bar, ADR 0009), the EDN shape accepted, a minimal `->`-threaded example, which AG Grid option keys it writes, and Enterprise/version constraints. One "Getting started" article shows builders in narrative context and links to vars — it never restates a contract. Rationale: docstrings travel with the code (editor/REPL/cljdoc); a catalog page would duplicate and rot.

3. **Article inventory — six topical pages (not one cookbook), each a cljdoc article:**
   - `docs/getting-started.md` — install, module registration, first grid, builder tour, Enterprise setup (`register!` + `set-license-key!` folded in; not a separate page).
   - `docs/options-and-conversion.md` — EDN options map, kebab→camel, conversion boundary rules, `raw`, keywords-in-value-position, dev warnings (added beyond the assigned homes: the conversion contract is the library's core idea).
   - `docs/updating-data.md` — `set-rows!`/`transact!`/`update-grid!` semantics + the set-rows!-from-db pattern + the pending-rows recipe + the range-fill batch-flush recipe (it's "how to batch transactions", so it lives here, not with Enterprise).
   - `docs/cell-rendering.md` — the three renderer tiers (ADR 0011) + built-in-renderers-by-name + data-type-definitions prose.
   - `docs/framework-composition.md` — exactly as scoped by ADR 0012: Reagent `r/as-element` + UIx `$` into react-renderer, static-mount-div split, nested-createRoot caveat, reactive-seams pointer.
   - `docs/theming.md` — exactly as scoped by ADR 0013: default theme, `.withParams`, dark mode, `theme: "legacy"` escape hatch.

4. **Generated reference is a cljdoc article.** `docs/reference/ag-grid-options.md` (committed, codegen-emitted per ADR 0007) joins the cljdoc tree as "AG Grid options reference", last in the sidebar, with a generated header: AG Grid version stamp, machine-generated notice (edit the codegen tool, not the file), pointer to AG Grid docs as semantic source of truth. Regeneration on a new AG Grid pin is a normal commit; no extra process. Browser text search covers the kebab↔camel lookup job.

5. **Completeness bar for the spec.** The spec is complete on the docs axis when it locks: (a) the cljdoc-canonical architecture + the `doc/cljdoc.edn` tree, (b) the six-article inventory with a content outline per article (outlines = the scoped deliverables from earlier tickets; this decision fixed their homes), (c) the docstring convention, (d) the generated reference as the seventh article. Writing the actual prose/docstrings is implementation-phase work — the spec hands implementation a complete doc plan with nothing left to decide.

## Considered options

- **A builders-catalog article** — rejected: it would duplicate the docstring contracts and rot; docstrings travel with the code through editor, REPL, and cljdoc.
- **A separate docs site (GitHub Pages/mkdocs)** — rejected: cljdoc renders both articles and API reference from the repo with no extra infrastructure.
- **One cookbook page instead of topical articles** — rejected in favor of six topical pages, each owning a coherent subject and the recipes earlier tickets assigned to it.
- **A separate Enterprise-setup page** — rejected: `register!` + `set-license-key!` fold into getting-started.

## References

- ADR 0007 — generated key registry (source of the generated options reference)
- ADR 0009 — builder catalog v1 (the coerce/bundle bar the docstrings state; pending-rows and range-fill recipes)
- ADR 0011 — cell renderer tiers (cell-rendering article content)
- ADR 0012 — no framework adapters in v1 (framework-composition article scope)
- ADR 0013 — theming docs-only (theming article scope)

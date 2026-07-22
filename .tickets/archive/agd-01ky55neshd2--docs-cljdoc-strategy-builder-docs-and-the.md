---
id: agd-01ky55neshd2
title: 'Docs/cljdoc strategy: builder docs and the framework-composition recipe page'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-22T15:03:49.809751591Z'
updated: '2026-07-22T15:36:05.974484127Z'
closed: '2026-07-22T15:36:05.974484127Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

Decide the documentation strategy: how the 8 curated builders are documented (cljdoc articles? README? docstring conventions?); where the prose homes live for the pieces earlier decisions assigned to docs — the pending-rows and range-fill batch-flush RECIPES, data-type-definitions and built-in-renderers-by-name prose (builder-catalog ticket), the set-rows!-from-db consumer pattern (Fulcro ticket), and the framework-composition recipe page (Reagent r/as-element + UIx $ into react-renderer, static-mount-div split, nested-createRoot caveat, reactive-seams pointer — adapters ticket); how the generated docs/reference/ag-grid-options.md is surfaced; and what the doc set must contain for the spec to count as complete.

## Notes

**2026-07-22T15:11:07.247187271Z**

Newly assigned prose home from the theming decision (agd-01ky55n5xn5c): the theming recipe page — default theme, .withParams customization, dark mode (dark base / colorSchemeDark part), theme: "legacy" CSS-files escape hatch. Add it to this ticket's prose-homes list.

**2026-07-22T15:35:57.345532835Z**

RESOLUTION (grilling session, 2026-07-22):

**1. Architecture — cljdoc is canonical.** README = pitch + quickstart; cljdoc = the documentation site (Markdown articles committed under docs/, tree curated via doc/cljdoc.edn, docstrings rendered as API reference). No separate docs site (no GitHub Pages/mkdocs). cljdoc builds on release only — during the design phase the articles are GitHub-readable Markdown, which is fine since shipping is out of scope.

**2. Builders — docstrings are the canonical contract; no builders catalog article.** Each of the 8 builders AND the runtime fns (create-grid!, set-rows!, transact!, update-grid!, destroy!, grid-api, raw, register!, set-license-key!) gets a full docstring stating: what it coerces/bundles (the catalog bar), the EDN shape accepted, a minimal ->-threaded example, which AG Grid option keys it writes, and Enterprise/version constraints. One "Getting started" article shows builders in narrative context and links to vars — it never restates a contract. Rationale: docstrings travel with the code (editor/REPL/cljdoc); a catalog page would duplicate and rot.

**3. Article inventory — six topical pages (not one cookbook), each a cljdoc article:**
- docs/getting-started.md — install, module registration, first grid, builder tour, Enterprise setup (register! + set-license-key! folded in; not a separate page)
- docs/options-and-conversion.md — EDN options map, kebab→camel, conversion boundary rules, raw, keywords-in-value-position, dev warnings (added beyond the assigned homes: the conversion contract is the library's core idea)
- docs/updating-data.md — set-rows!/transact!/update-grid! semantics + set-rows!-from-db pattern (Fulcro ticket) + pending-rows recipe + range-fill batch-flush recipe (catalog ticket; it's "how to batch transactions", so it lives here, not with Enterprise)
- docs/cell-rendering.md — three renderer tiers + built-in-renderers-by-name + data-type-definitions prose (catalog ticket)
- docs/framework-composition.md — exactly as scoped by the adapters ticket (Reagent r/as-element + UIx $ into react-renderer, static-mount-div split, nested-createRoot caveat, reactive-seams pointer)
- docs/theming.md — exactly as scoped by the theming ticket (default theme, .withParams, dark mode, theme: "legacy" escape hatch)

**4. Generated reference is a cljdoc article.** docs/reference/ag-grid-options.md (committed, codegen-emitted per the key-registry ticket) joins the cljdoc tree as "AG Grid options reference", last in the sidebar, with a generated header: AG Grid version stamp, machine-generated notice (edit the codegen tool, not the file), pointer to AG Grid docs as semantic source of truth. Regeneration on a new AG Grid pin is a normal commit; no extra process. Browser text search covers the kebab↔camel lookup job.

**5. Completeness bar for the spec.** The spec is complete on the docs axis when it locks: (a) the cljdoc-canonical architecture + doc/cljdoc.edn tree, (b) the six-article inventory with a content outline per article (outlines = the scoped deliverables from earlier tickets; this ticket fixed their homes), (c) the docstring convention, (d) the generated reference as the seventh article. Writing the actual prose/docstrings is implementation-phase work, outside this map — the spec hands implementation a complete doc plan with nothing left to decide.

**2026-07-22T15:36:05.974484127Z**

cljdoc is canonical (articles under docs/, doc/cljdoc.edn tree, README = pitch+quickstart, no separate site). Docstrings are the per-var contract (8 builders + runtime fns: coerce/bundle statement, EDN shape, threaded example, written keys, Enterprise/version constraints); no builders-catalog article. Six topical articles: getting-started (+Enterprise setup), options-and-conversion, updating-data (set-rows!-from-db, pending-rows, range-fill flush), cell-rendering (built-ins, data-type-definitions), framework-composition, theming. Generated docs/reference/ag-grid-options.md joins the cljdoc tree as a machine-generated article. Spec completeness = architecture + inventory w/ outlines + docstring convention + generated reference locked; prose written at implementation.

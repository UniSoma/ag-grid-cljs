---
id: agd-01ky5hj3awrs
title: 'Docs II: updating-data, cell-rendering, framework-composition, theming'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-22T18:31:42.681995271Z'
updated: '2026-07-22T20:51:42.164582867Z'
closed: '2026-07-22T20:51:42.164582867Z'
acceptance:
- title: updating-data.md covers the three channels plus the pending-rows and range-fill recipes
  done: false
- title: cell-rendering.md covers the three tiers, built-ins by name, data-type-definitions
  done: false
- title: framework-composition.md and theming.md match their ADR-scoped outlines exactly
  done: false
- title: articles added to the cljdoc.edn tree
  done: false
deps:
- agd-01ky5hj32e7j
---

## Description

The four remaining topical articles per ADR 0014: docs/updating-data.md (set-rows!/transact!/update-grid! semantics, the set-rows!-from-db pattern, the pending-rows recipe, the range-fill batch-flush recipe); docs/cell-rendering.md (the three renderer tiers of ADR 0011, built-in renderers by name string, data-type-definitions prose); docs/framework-composition.md (exactly the ADR 0012 scope: Reagent r/as-element and UIx $ into react-renderer, static-mount-div split, nested-createRoot caveat, reactive-seams pointer); docs/theming.md (exactly the ADR 0013 scope: default theme, .withParams, dark mode, theme legacy escape hatch). Recipes stay framework-agnostic where their ADRs demand it.

## Notes

**2026-07-22T20:51:42.164582867Z**

Four topical articles per ADR 0014, filling the cljdoc.edn placeholders from Docs I (tree entries already present, so criterion 4 was satisfied by Docs I; files now exist). updating-data.md: three channels + set-rows!-from-db + pending-rows + range-fill batch-flush recipes (framework-agnostic atoms). cell-rendering.md: three tiers (bare fn / render/dom-renderer / react/react-renderer), built-ins by name string, data-type-definitions. framework-composition.md: exactly ADR 0012 (Reagent/UIx one-liner, static-mount-div split, nested-createRoot caveat, reactive-seams pointer). theming.md: exactly ADR 0013. Committed on branch docs-ii-articles (75e94ad).

---
id: agd-01ky5hj3awrs
title: 'Docs II: updating-data, cell-rendering, framework-composition, theming'
status: open
type: task
priority: 2
mode: afk
created: '2026-07-22T18:31:42.681995271Z'
updated: '2026-07-22T18:31:42.681995271Z'
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

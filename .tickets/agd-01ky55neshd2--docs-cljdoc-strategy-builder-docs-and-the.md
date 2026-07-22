---
id: agd-01ky55neshd2
title: 'Docs/cljdoc strategy: builder docs and the framework-composition recipe page'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-22T15:03:49.809751591Z'
updated: '2026-07-22T15:11:07.247187271Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
---

## Description

## Question

Decide the documentation strategy: how the 8 curated builders are documented (cljdoc articles? README? docstring conventions?); where the prose homes live for the pieces earlier decisions assigned to docs — the pending-rows and range-fill batch-flush RECIPES, data-type-definitions and built-in-renderers-by-name prose (builder-catalog ticket), the set-rows!-from-db consumer pattern (Fulcro ticket), and the framework-composition recipe page (Reagent r/as-element + UIx $ into react-renderer, static-mount-div split, nested-createRoot caveat, reactive-seams pointer — adapters ticket); how the generated docs/reference/ag-grid-options.md is surfaced; and what the doc set must contain for the spec to count as complete.

## Notes

**2026-07-22T15:11:07.247187271Z**

Newly assigned prose home from the theming decision (agd-01ky55n5xn5c): the theming recipe page — default theme, .withParams customization, dark mode (dark base / colorSchemeDark part), theme: "legacy" CSS-files escape hatch. Add it to this ticket's prose-homes list.

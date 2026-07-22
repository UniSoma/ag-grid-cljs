---
id: agd-01ky55n5xn5c
title: 'Theming/CSS story: Theming API vs CSS files, wrapper exposure'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-22T15:03:40.725758546Z'
updated: '2026-07-22T15:11:05.999659262Z'
closed: '2026-07-22T15:11:05.999659262Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
assignee: jonas
---

## Description

## Question

How does the wrapper expose AG Grid's styling? Decide: Theming API (v33+ default, JS theme objects) vs legacy CSS files; whether the wrapper offers any theming sugar (a builder? plain option passthrough under the conversion contract?) or stays hands-off; how themes interact with the conversion boundary (theme objects are opaque JS values — raw passthrough?); and what the docs say a consumer does for the common cases (default theme, dark mode, custom params).

## Notes

**2026-07-22T15:11:05.908710854Z**

**2026-07-22T15:11:05.999659262Z**

No theming code in v1: :theme rides the conversion boundary as an opaque JS value (already true by contract, zero special-casing); builder/coercion sugar stays deferred per catalog v1. Deliverable is a docs recipe — default theme, .withParams customization (raw interop, camelCase params), dark mode (dark base or colorSchemeDark part), and the theme: "legacy" CSS-files escape hatch. Recipe home lands with the docs strategy ticket.

## Resolution: no theming code in v1 — docs recipe only

Decision (resolves all four sub-questions):

1. **No theming code in v1.** No `with-theme` builder, no params-coercion helper — honors the builder-catalog v1 deferral (agd-01ky0edx5mfs deferred theming post-v1). Option B (kebab-keyed .withParams coercion sugar) considered and rejected for v1: it meets the coerce bar but the catalog already deferred it, and plain interop is acceptable.
2. **Conversion boundary: already true by contract.** A theme object (`themeQuartz`, `.withParams(...)` result) is an opaque JS value, not a CLJS map, so `:theme` rides the conversion boundary untouched (per agd-01ky0eck96vn: non-CLJS values pass through verbatim). Zero special-casing. `:theme` is a known GridOptions key in the registry — dev validation and :initial? classification come for free.
3. **Theming API is the assumed default** (v33+; we target v34+). Legacy CSS files are the escape hatch, not the primary story.
4. **Deliverable: one docs recipe** covering — default theme (no :theme key = quartz); customization via `.withParams` in plain interop (`(.withParams themeQuartz #js {:accentColor "red"})` — camelCase param names, it's raw interop past the boundary); dark mode (withParams on a dark base theme or composing the `colorSchemeDark` part via `.withPart`); and the `theme: "legacy"` escape hatch (string value; consumer imports ag-grid.css + ag-theme-*.css through their bundler).

The recipe's home lands with the docs/cljdoc strategy ticket (agd-01ky55neshd2), which owns prose-home placement.

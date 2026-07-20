---
id: agd-01ky0eck96vn
title: 'Conversion boundary rules: what converts, what passes through untouched'
status: closed
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:04.518255534Z'
updated: '2026-07-20T20:03:21.190774006Z'
closed: '2026-07-20T20:03:21.190774006Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
assignee: jonas
---

## Description

## Question

Define the exact rules for the EDN->JS options conversion: which keys get kebab->camel renaming and at what nesting depths; which values must pass through untouched (row data, functions, JS class instances, datasources); how string enum values (e.g. :multi-row vs "multiRow") are handled; how users force raw pass-through for a subtree; and how the same rules run in reverse for what callbacks receive (cljs-bean boundaries). Output: a decision on the conversion contract precise enough to implement in the walking skeleton.

## Notes

**2026-07-20T20:03:13.153909197Z**

# Resolution: the conversion contract

**2026-07-20T20:03:21.190774006Z**

Conversion contract locked: mechanical kebab->camel (registry = warnings only), type-driven recursion (CLJS maps/sequentials convert, everything else untouched, string keys verbatim), keywords translate in value position too (keyword=vocabulary, string=verbatim), (ag/raw x) sole escape hatch, nil->null key kept, sets pass through + dev-warn. Reverse: callbacks get lazy kebab beans, returns forward-converted, raw f opts out for hot paths. Zero deps: cljs-bean slice vendored (EPL headers retained).

## Forward: EDN -> JS options

1. **Mechanical, not registry-driven.** Keyword -> camelCase by pure string transform: split the keyword name on `-`, capitalize each segment after the first, join. `:row-data` -> "rowData"; single-segment `:sortable` -> "sortable"; already-camel `:rowData` passes unchanged (docs copy-paste works). No other munging. The key registry is consulted only for dev-mode warnings (unknown key, deprecated, did-you-mean) - never to decide output. Rationale: works for keys the registry doesn't cover (newer AG Grid, cellRendererParams, user params); one law users can hold in their head; research verified the transform is collision-free over all 726 real keys.

2. **Type-driven recursion, no key/depth tables.** CLJS map -> JS object (keyword keys camelized, string keys copied verbatim, values recursed); CLJS sequential (vector/list/seq) -> JS array (elements recursed); **everything else passes through untouched** - functions, strings, numbers, booleans, JS objects/arrays, class instances, js/Date, datasources. Row data arriving as JS passes untouched by construction (JS-by-contract needs no special case).

3. **Keywords translate in value position too.** `:multi-row` -> "multiRow", `:dom-layout :auto-height` -> "autoHeight". One law: keyword = AG Grid vocabulary, translate; string = verbatim, hands off. Applies to `:field` (`:field :first-name` -> "firstName"; snake_case data uses the string form). Namespaced keywords convert by name with a dev warning on the dropped namespace.

4. **Sole escape hatch: `(ag/raw x)` wrapper** (tiny wrapper type; converter unwraps and emits x untouched, no recursion, no renaming). Metadata `^:raw` rejected: invisible and silently lost through collection ops. Canonical use: `:context (ag/raw my-cljs-map)` for identity round-trip. Also works on functions (see 7).

5. **Dev-mode warning on data-carrying keys** (`:row-data`, `:pinned-top-row-data`, `:context`, ...) receiving bare CLJS collections - the JS-by-contract nudge, pointing at `raw` or clj->js.

6. **Edge cases.** nil -> null with the key kept in the emitted object ("explicitly unset" stays expressible; matters for options diffing). CLJS sets pass through untouched + dev warning: converting to array would emit non-deterministic hash order (silent reshuffles for order-sensitive options, phantom diffs for the diffing layer) and mask what is almost certainly a `#{}`-for-`[]` mistake.

## Reverse: what user functions receive and return

7. **Functions in the options tree are auto-wrapped.** Params arrive as a lazy cljs-bean `bean` with the kebab<->camel key transform: `(:row-index params)`, `(:value params)`; only accessed keys pay conversion; the bean is a view, not a copy - the underlying JS object (incl. row data) is reachable and never converted. Return values run through the forward converter (scalars free; `{:font-weight "bold"}` -> JS object; keyword enums -> camel strings). **`(ag/raw f)` opts out entirely** - raw JS params in, return passed as-is - the documented hot-path idiom (per-cell getters on large grids). The walking-skeleton benchmark ticket measures the bean-per-call cost.

## Dependency posture

8. **Zero runtime dependencies besides the AG Grid peer packages.** Bean semantics come from a **vendored slice of cljs-bean** (the cljs-bean.core essentials incl. its :prop->key/:key->prop support) under an internal namespace (e.g. `ag-grid-cljs.impl.bean`), with EPL headers retained on those files and a THIRD-PARTY.md notice - the standard MIT-project-with-vendored-EPL-files pattern (EPL is file-scoped; verified cljs-bean is EPL-licensed). Rename avoids version conflicts with consumers using cljs-bean directly. Fallback if pure-MIT ever becomes a goal: hand-rolled minimal lazy bean (~200 lines), consciously not chosen now because cljs-bean's map-semantics edge cases are battle-tested.

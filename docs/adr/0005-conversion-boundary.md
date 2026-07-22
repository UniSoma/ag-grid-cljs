# 0005. Conversion boundary: what converts, what passes through untouched

- Status: accepted, 2026-07-20
- Origin: knot ticket agd-01ky0eck96vn (tickets are ephemeral; this record is self-contained)

The EDN->JS conversion at the options boundary is a mechanical, type-driven transform: kebab-case keywords camelize by pure string manipulation, CLJS maps and sequentials recurse, everything else passes through untouched, and `(ag/raw x)` is the sole escape hatch. The reverse direction wraps user callbacks so params arrive as lazy kebab-keyed beans and return values are forward-converted.

## Context

The wrapper's core promise (ADR 0002) is an idiomatic EDN options map over AG Grid's camelCase JS options — which requires exact rules for the conversion: which keys get kebab->camel renaming and at what nesting depths; which values must pass through untouched (row data, functions, JS class instances, datasources); how keyword enum values (e.g. `:multi-row` vs `"multiRow"`) are handled; how users force raw pass-through for a subtree; and how the same rules run in reverse for what callbacks receive. Row data is JS by contract (ADR 0003), so the boundary must not touch it. The rules had to be precise enough to implement directly in the walking skeleton.

## Decision

### Forward: EDN -> JS options

1. **Mechanical, not registry-driven.** Keyword -> camelCase by pure string transform: split the keyword name on `-`, capitalize each segment after the first, join. `:row-data` -> `"rowData"`; single-segment `:sortable` -> `"sortable"`; already-camel `:rowData` passes unchanged (docs copy-paste works). No other munging. The key registry (ADR 0007) is consulted only for dev-mode warnings (unknown key, deprecated, did-you-mean) — never to decide output. Rationale: the mechanical transform works for keys the registry doesn't cover (newer AG Grid, `cellRendererParams`, user params); it is one law users can hold in their head; and research verified the transform is collision-free over all 726 real AG Grid option keys.

2. **Type-driven recursion, no key/depth tables.** CLJS map -> JS object (keyword keys camelized, string keys copied verbatim, values recursed); CLJS sequential (vector/list/seq) -> JS array (elements recursed); **everything else passes through untouched** — functions, strings, numbers, booleans, JS objects/arrays, class instances, `js/Date`, datasources. Row data arriving as JS passes untouched by construction — JS-by-contract (ADR 0003) needs no special case.

3. **Keywords translate in value position too.** `:multi-row` -> `"multiRow"`; `:dom-layout :auto-height` -> `"autoHeight"`. One law: keyword = AG Grid vocabulary, translate; string = verbatim, hands off. This applies to `:field` (`:field :first-name` -> `"firstName"`; snake_case data uses the string form). Namespaced keywords convert by name with a dev warning on the dropped namespace.

4. **Sole escape hatch: the `(ag/raw x)` wrapper** — a tiny wrapper type; the converter unwraps and emits `x` untouched, no recursion, no renaming. Metadata `^:raw` was rejected: invisible, and silently lost through collection operations. Canonical use: `:context (ag/raw my-cljs-map)` for an identity round-trip. Also works on functions (see point 7).

5. **Dev-mode warning on data-carrying keys** (`:row-data`, `:pinned-top-row-data`, `:context`, ...) receiving bare CLJS collections — the JS-by-contract nudge, pointing at `raw` or `clj->js`.

6. **Edge cases.** `nil` -> `null` with the key kept in the emitted object ("explicitly unset" stays expressible; matters for options diffing). CLJS sets pass through untouched plus a dev warning: converting a set to an array would emit non-deterministic hash order (silent reshuffles for order-sensitive options, phantom diffs for the diffing layer) and would mask what is almost certainly a `#{}`-for-`[]` mistake.

### Reverse: what user functions receive and return

7. **Functions in the options tree are auto-wrapped.** Params arrive as a lazy cljs-bean `bean` with the kebab<->camel key transform: `(:row-index params)`, `(:value params)`; only accessed keys pay conversion; the bean is a view, not a copy — the underlying JS object (including row data) is reachable and never converted. Return values run through the forward converter (scalars free; `{:font-weight "bold"}` -> JS object; keyword enums -> camel strings). **`(ag/raw f)` opts out entirely** — raw JS params in, return passed as-is — the documented hot-path idiom (per-cell getters on large grids). The event/callback API shape is ADR 0010.

### Dependency posture

8. **Zero runtime dependencies besides the AG Grid peer packages.** Bean semantics come from a **vendored slice of cljs-bean** (the `cljs-bean.core` essentials, including its `:prop->key`/`:key->prop` support) under an internal namespace (`ag-grid-cljs.impl.bean`), with EPL headers retained on those files and a THIRD-PARTY.md notice — the standard MIT-project-with-vendored-EPL-files pattern (EPL is file-scoped; verified cljs-bean is EPL-licensed). The namespace rename avoids version conflicts with consumers using cljs-bean directly.

## Consequences

- One mechanical law, no lookup tables: users can predict any conversion without consulting a registry, and new AG Grid options work without wrapper updates.
- The keyword-vs-string distinction becomes API vocabulary: keyword means "AG Grid term, translate me", string means "my data, hands off".
- `nil` values survive into the emitted object, so the diffing layer can distinguish "set to null" from "absent".
- Callback beans are lazy views, so the per-call cost scales with keys actually accessed; the walking-skeleton benchmark ticket measures the bean-per-call cost, and `raw` remains the documented opt-out for hot paths.

## Considered options

- **Registry-driven key translation** — rejected: fails for keys the registry doesn't cover; the registry stays advisory (warnings only, per ADR 0002's open-surface guarantee).
- **`^:raw` metadata as the escape hatch** — rejected: invisible in code and silently lost through collection operations; the visible `(ag/raw x)` wrapper type won.
- **Converting CLJS sets to JS arrays** — rejected: non-deterministic hash order causes silent reshuffles and phantom diffs, and hides `#{}`-for-`[]` mistakes.
- **Hand-rolled minimal lazy bean (~200 lines) instead of vendoring cljs-bean** — consciously not chosen now because cljs-bean's map-semantics edge cases are battle-tested; remains the fallback if pure-MIT licensing ever becomes a goal.

## References

- ADR 0002 — layered API shape (EDN options map this boundary serves)
- ADR 0003 — row data is JS by contract (passes untouched by construction)
- ADR 0007 — generated key registry (dev warnings only, never gatekeeping)
- ADR 0010 — event and callback API shape

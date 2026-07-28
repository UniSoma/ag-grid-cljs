---
id: agd-01kymfv9sh68
title: Check cited component names against :components at creation
status: closed
type: bug
priority: 2
mode: hitl
created: '2026-07-28T13:50:23.533757142Z'
updated: '2026-07-28T15:03:21.924077158Z'
closed: '2026-07-28T15:03:21.924077158Z'
tags:
- conversion
- dx
acceptance:
- title: Runs always-on off the entry point from agd-01kyjsex69qt, at creation and on update; dead-code-eliminated under :advanced
  done: false
- title: Node suite covers resolved-silent, near-match-warns, dedup, and each non-name citation shape
  done: false
- title: A cited component name that resolves to no :components registration and has no near-match stays silent (no built-in component list, no version pin)
  done: false
- title: A cited component name with a near-match against one of the consumer's own :components registrations warns once with a did-you-mean naming the registered spelling
  done: false
- title: Registrations collected from :components only; citations from :cell-renderer, :cell-editor and :filter
  done: false
- title: 'Non-name citation values never warn: fn/class/true renderers and editors, built-in filter names, raw-wrapped subtrees'
  done: false
- title: The other three consumer-keyed citation sites (:agg-func/:initial-agg-func, :type, :cell-data-type) are NOT checked here — AG Grid warns on them ungated
  done: false
- title: CONTEXT.md gains the Name reference check term; docs/options-and-conversion.md promotes ValidationModule to the primary answer for name mismatches and states the registration-vs-citation camelization rule
  done: false
deps:
- agd-01kyjsex69qt
---

## Description

Four **consumer-keyed options** (CONTEXT.md) register names the consumer coins, each cited from elsewhere in the options map. Research against AG Grid 36.0.2 found that **three of the four already warn natively**, so this ticket is scoped to the one that does not:

| Registration | Cited from | AG Grid's own check |
|---|---|---|
| `:agg-funcs` | `:agg-func`, `:initial-agg-func` | `_warn(109)` — `Could not find 'myTotal' aggregate function… Did you mean: [my-total]?` Ungated. |
| `:column-types` | `:type` | `_warn(36)` at column creation. Ungated. |
| `:data-type-definitions` | `:cell-data-type` | `_warn(47)` at data-type resolution. Ungated. |
| `:components` | `:cell-renderer`, `:cell-editor`, `:filter` | `_warn(101)` — **gated on ValidationModule** *and* deferred to component instantiation. |

AG Grid's `_fuzzySuggestions` also crosses the camelization asymmetry in both directions (input `"myTotal"` against registered `["my-total"]` returns `my-total` first), so the flagship `{:agg-funcs {"my-total" f} :agg-func :my-total}` case already gets a native did-you-mean naming the registered spelling. Without ValidationModule the ungated warnings still fire, just terse: `warning #47 Visit …/errors/47?cellDataType=x`, with the offending value in the URL params.

**The remaining hole is `:components`.** `missingUserComponent` is a method on ValidationService (community main.cjs.js:57499), reached only through `beans.validation?.missingUserComponent(…)` (main.cjs.js:22768), so with no ValidationModule registered there is no warning at all. Even with it registered the warning waits for the component to be instantiated — a mistyped `:cell-editor` stays silent until someone opens an editor, and a mistyped `:filter` until someone opens a filter, which in dev may be never. A mistyped `:cell-renderer` does surface on first paint.

Reimplementing the other three is explicitly out of scope: `validate.cljs`'s docstring commits to never reimplementing checks delegated to ValidationModule, and a second prose copy of a warning AG Grid already emits is drift for no coverage.

Background and the reasoning behind the original split: ADR 0019.

## Design

**Home.** `impl.validate`, extending the always-on entry point the sibling ticket creates. `validate-options!` (validate.cljs:164-173) already walks exactly the tree this needs — grid options, `:column-defs` recursing `:children`, `:default-col-def`, `:auto-group-column-def` — and `levenshtein` / `closest` / `suggest` already exist. Both the registrations and the citations live in the options map and stay there (unlike rows, which leave it per ADR 0004), so no api listeners are needed.

**Drift-free, therefore always-on.** Do *not* carry a list of AG Grid's built-in component names. Warn only when the unresolved citation is a **near-match against one of the consumer's own `:components` registrations**. A genuine built-in the consumer never registered has no near-match and stays silent. That is what makes the check registry-free — it compares two consumer-supplied things to each other, exactly like the field check — so ADR 0017 §1's criterion applies and it needs no `enable-dev-validations!` gate.

**What this adds over AG Grid.** Earlier (creation time, not instantiation time) and unconditional (no ValidationModule needed). It does not add fuzzy matching AG Grid lacks — AG Grid's is fine when it runs; the point is that for `:cell-editor` and `:filter` it may never run.

**Shape.** Collect `:components` keys in emitted form (camelize keyword keys, strings verbatim). Collect `:cell-renderer` / `:cell-editor` / `:filter` citations from the ColDef walk in emitted form. Warn per unresolved citation with a near-match, deduped through `warn-once!`.

**Care needed on the citation sites** — several accept things that are not names at all, and none of those should warn:
- `:cell-renderer` / `:cell-editor` / `:filter` accept a function, a class, or `true` instead of a name
- `:filter` also accepts AG Grid's built-in filter names (which, having no near-match among the consumer's registrations, fall out silently by construction)
- `raw`-wrapped subtrees are opaque and must stay opaque (see the `opaque-positions-never-warn` test)

**Docs, covering the three sites this check drops.** `docs/options-and-conversion.md:290` already points at ValidationModule; promote that to the primary answer for name mismatches, and state the registration-vs-citation rule for consumer-keyed names: a keyword key camelizes, a string key does not, so `{:agg-funcs {"my-total" f} :agg-func :my-total}` registers one spelling and cites another. Taught as a rule, the way ADR 0019 §68 handled the analogous class-rules case.

**Glossary.** Proposed term when this lands: **Name reference check** — the always-on dev diagnostic comparing each cited component name against the names registered in `:components`, warning with a did-you-mean. Parallel to the existing **Field check**. Not added to CONTEXT.md until built.

## Notes

**2026-07-28T15:00:10.603644228Z**

Scope narrowed after reading AG Grid 36.0.2 source. Native coverage found: _warn(109) for aggFunc (enterprise main.cjs.js:30808, 61571, with fuzzy did-you-mean), _warn(36) for colDef.type (community main.cjs.js:1515, at column creation), _warn(47) for cellDataType (community main.cjs.js:21327) — all three ungated, reaching the console even with no ValidationModule via minifiedLog (getErrorParts, main.cjs.js:925), which prints 'warning #N Visit .../errors/N?<value>'. Verified AG Grid's _fuzzySuggestions crosses the camelization asymmetry both ways: input "myTotal" against ["my-total",...] returns "my-total" first, and vice versa. Only :components is uncovered: missingUserComponent is a ValidationService method (main.cjs.js:57499) reached via beans.validation?. (main.cjs.js:22768), so it needs ValidationModule, and it fires at component instantiation — never for an unopened :cell-editor or :filter.

**2026-07-28T15:03:21.924077158Z**

wontdo: AG Grid already covers this. Research against AG Grid 36.0.2 found native warnings on three of the four consumer-keyed citation sites, ungated and reaching the console even with no ValidationModule registered (minifiedLog fallback in getErrorParts, community main.cjs.js:925): _warn(109) for aggFunc with a fuzzy did-you-mean (enterprise main.cjs.js:30808, 61571), _warn(36) for colDef.type at column creation (community:1515), _warn(47) for cellDataType (community:21327). AG Grid's own _fuzzySuggestions crosses the camelization asymmetry both ways (input "myTotal" against ["my-total"] returns "my-total" first), so the ticket's flagship case already gets a native did-you-mean naming the registered spelling — the premise that these mismatches are silent was wrong. The one real gap, :components, is narrow enough not to earn a check of its own: missingUserComponent is a ValidationService method (community:57499) reached via beans.validation?. (community:22768), so registering ValidationModule closes it, which docs/options-and-conversion.md:290 already tells consumers to do; a mistyped :cell-renderer surfaces on first paint, leaving only :cell-editor and :filter deferred to instantiation. Reimplementing any of it would also cut against validate.cljs's commitment to delegate type/dependency/row-model checks to ValidationModule. The registration-vs-citation camelization rule (keyword keys camelize, string keys do not) remains worth teaching in docs/options-and-conversion.md, the way ADR 0019 handled the analogous class-rules rule — open a docs ticket if wanted. Full source evidence in the ticket notes.

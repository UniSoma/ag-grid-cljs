---
id: agd-01kynwzbcmnt
title: Deferred values at the conversion boundary; with-row-id assocs data
status: closed
type: bug
priority: 1
mode: afk
created: '2026-07-29T02:59:02.159577697Z'
updated: '2026-07-29T12:03:37.603991342Z'
closed: '2026-07-29T12:03:37.603991342Z'
tags:
- convert
- builders
- docs
acceptance:
- title: Each with-row-id input shape — keyword, camel-fallback keyword, top-level fn, (raw f) — returns a value that is = across two calls with the same input
  done: true
- title: (with-row-id opts (raw f)) produces a callback that receives raw JS params and str-coerces its return, instead of throwing a TypeError on the first row
  done: true
- title: The existing with-row-id tests pass untouched — keyword camel/literal/priority/own-property lookup and the plain-fn bean plus str-coercion — as the evidence the relocation preserved runtime behavior
  done: true
- title: rebuilt-opts in the rebuild-stability fixture carries (with-row-id :id), and the rebuild test still yields zero setGridOption calls and zero warnings, with its positive control still shipping exactly one call
  done: true
- title: A table-driven check calls each of the eight ADR 0009 catalog entries twice with the same arguments and asserts = (renderer helpers excluded until agd-01kynwzt3a16)
  done: true
- title: Public raw keeps its single arity and verbatim meaning; the tag, the tagged constructor and the dispatch stay internal to impl, and the untagged ->js path stays a direct field read rather than a dispatch
  done: true
- title: updating-data.md documents the promise with the renderer caveat, framework-composition.md carries the pointer, and the with-row-id docstring is accurate about (raw f) and about the keyword branch's no-bean hot path
  done: true
deps:
- agd-01kynwykcdyg
---

## Description

`with-row-id` mints a fresh callback on every call, in both branches, so a builder pipeline re-run during render produces an options map that is never `=` to the previous one. `:get-row-id` is `:initial? true`, so the first `update-grid!` after mount emits "grid option `:get-row-id` is initial-only and cannot change after creation; update-grid! ignored it" on every grid in the app, for a value that did not semantically change. No consumer-side memoization can suppress it — the closure is minted inside the builder, so the only workaround is to reach around the builder entirely.

## What to build

Turn `with-row-id` into a builder that assocs **data**, and move callback construction to the conversion boundary, so the value stashed in the options map is the consumer's own input and equal inputs compare `=`. This lands the deferred-value mechanism ADR 0021 §4 specifies — the dispatch on `Raw`'s internal tag, with the untagged case still a direct field read — plus its first user, end to end: builder, boundary, tests, docs. `Raw` already carries the tag and compares by value (agd-01kynwykcdyg); only the dispatch and this user are new.

Three input shapes must survive the relocation with today's runtime behavior byte-for-byte, which is what the untouched existing tests are the evidence for:

- **keyword** — camel-then-literal own-property read off the raw JS row per ADR 0018 §4 (camel keeps priority; presence, not truthiness, decides), `str`-coerced, no bean allocated.
- **fn** — receives the kebab-bean params and its return is `str`-coerced. The construction method must apply the bean wrapping itself: a tagged `Raw` bypasses the converter's generic fn auto-wrapping, so producing `(wrap-fn (fn [p] (str (user-fn p))))` is what reproduces current behavior exactly (forward-converting a string return is a no-op).
- **`(raw f)`** — unwrap, pass raw JS params, still `str`-coerce. This is the documented hot-path idiom, and it throws a TypeError today because `Raw` implements no `IFn`; routing construction through the tag makes it work for the first time.

The `str` coercion stays. AG Grid coerces too (`_getRowIdCallback` does `String(id)` after a deduped warning), so ours buys warning suppression rather than correctness — but it is the coercion that earns the builder its slot against ADR 0009's admission bar.

One thing ADR 0021 §4 leaves open: the `:row-id` construction registers inside `convert`, not `core`, because it needs the converter's own key transform and fn wrapper. `core/with-row-id` only stashes. The ADR's "each helper namespace registers its own construction" applies to `render`/`react` in agd-01kynwzt3a16 — the open-dispatch design is what keeps `convert` from having to require them.

Docs are part of this slice, per ADR 0021's Consequences: `updating-data.md` §"The options channel" owns the promise and the division of responsibility (rebuilding the whole map per render is supported; wrapper-manufactured values are rebuild-stable; your own closures are yours to keep stable), with the renderer helpers named as the one remaining caveat. `framework-composition.md` §"Stateful components" gets a pointer — its "not by re-rendering" line is right about rows but reads as forbidding the options pattern blessed here.

No new browser test: six existing browser tests already mount real grids with `(with-row-id :id)`, covering the runtime half.

## Blocked by

None - can start immediately. agd-01kynwykcdyg landed the `Raw` half (tag field, value equality, tag-only hash).

## Notes

**2026-07-29T12:03:37.603991342Z**

with-row-id now assocs a tagged Raw holding the consumer's input and the conversion boundary constructs the getRowId callback (ADR 0021 §4), so equal inputs give = options maps and a rebuilt map no longer draws the initial-only warning on :get-row-id. New in impl/convert: the internal deferred constructor, the construct multimethod ->js dispatches through (default = today's plain unwrap), and the :row-id method — registered here rather than in core because it needs the converter's own key transform and fn wrapper. The untagged ->js path stays a direct field read, not a dispatch; public raw keeps its single arity and verbatim meaning. All three input shapes keep today's runtime behavior, evidenced by the three existing with-row-id tests being untouched, and (with-row-id opts (raw f)) works for the first time — it previously called a Raw as a function and raised a TypeError on the first row. Tests: four-shape rebuild-stability check, a raw-params test, a table-driven pass over all eight ADR 0009 catalog entries, a boundary-dispatch test (untagged field read, unregistered tag falls back, registered tag constructs at conversion), and rebuilt-opts now carries (with-row-id :id). Docs: updating-data.md gains the rebuild promise, the division of responsibility and the renderer-helper caveat; framework-composition.md points at it. Node 90 tests / 292 assertions green, browser 14 tests / 45 assertions green, clj-kondo clean. Commit 2efd1b4. Known non-goal: (raw :id) — a raw-wrapped keyword rather than a fn, an undocumented shape — now yields an empty id instead of the old TypeError; left unguarded rather than adding a check for a shape no docstring offers.

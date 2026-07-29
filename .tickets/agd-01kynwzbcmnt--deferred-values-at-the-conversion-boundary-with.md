---
id: agd-01kynwzbcmnt
title: Deferred values at the conversion boundary; with-row-id assocs data
status: open
type: bug
priority: 1
mode: afk
created: '2026-07-29T02:59:02.159577697Z'
updated: '2026-07-29T02:59:02.159577697Z'
tags:
- convert
- builders
- docs
acceptance:
- title: 'with-row-id returns = values across two calls for each input shape: keyword, camel-fallback keyword, stable fn, raw-wrapped fn'
  done: false
- title: (with-row-id opts (raw f)) produces a callback that receives raw JS params and str-coerces its return, instead of throwing
  done: false
- title: 'Keyword and plain-fn behavior are unchanged: the existing with-row-id tests pass untouched'
  done: false
- title: The contract test from the previous ticket, extended to include :get-row-id, still yields zero setGridOption calls and zero warnings
  done: false
- title: A table-driven check calls each of the eight ADR 0009 builders twice with the same arguments and asserts = (renderer helpers excluded until the next ticket)
  done: false
- title: Public raw keeps its single arity; the tag and the multimethod are internal to impl
  done: false
- title: updating-data.md documents the promise with the renderer caveat, framework-composition.md carries the pointer, and the with-row-id docstring is accurate about raw
  done: false
deps:
- agd-01kynwykcdyg
---

## Description

with-row-id manufactures a fresh closure on every call, in both branches. A builder pipeline re-run during render therefore produces an options map that is never `=` to the previous one, and since `:get-row-id` is registered `:initial? true`, the first `update-grid!` after mount emits "grid option :get-row-id is initial-only and cannot change after creation; update-grid! ignored it" on every grid in the app. The value did not semantically change, and no consumer-side memoization can suppress it — the closure is built inside the builder. The only local workaround is to reach around the builder entirely.

This ticket makes the builder assoc data and moves construction to the conversion boundary, which is what keeps the stashed EDN comparable. It also fixes a documented idiom that throws today: `(with-row-id opts (raw f))` takes the non-keyword branch and calls a Raw as a function, but Raw implements no IFn, so it raises a TypeError on the first row — despite the docstring promising raw-wrapped fns receive raw JS params.

## Design

Raw gains an optional internal tag; the `raw?` branch of `->js` dispatches through an internal multimethod whose default is today's unwrap. Public `raw` keeps its single arity and its verbatim meaning (CONTEXT.md); an internal two-arg constructor mints tagged values. Keep the untagged case a direct field read rather than a dispatch: `->js` runs on every non-raw callback return, which is a hot path.

Open dispatch rather than a case: it lets each helper namespace register its own construction, so convert never has to require render or react. That matters for the next ticket — react is an optional namespace precisely so core consumers need not install react-dom. Load order is safe by construction, since a tagged value can only exist if the namespace that mints it was loaded.

with-row-id coerces keyword-or-fn into a deferred value; the :row-id method builds the closures that exist today, unchanged:
- keyword: camel-then-literal unchecked-get on the raw JS row per ADR 0018 §4, str-coerced, no bean allocated
- fn: the method must apply the bean wrapping itself, because a Raw bypasses the converter's generic fn auto-wrapping — wrapping `(fn [p] (str (user-fn p)))` in the converter's own fn wrapper reproduces current behavior exactly, since forward-converting a string return is a no-op
- raw-wrapped fn: unwrap, pass raw JS params, still str-coerce — the documented hot-path idiom, working for the first time

The str coercion stays. AG Grid does coerce (_getRowIdCallback does String(id) after a deduped warning), so ours buys warning suppression rather than correctness — but it is the coercion that earns the builder its slot against ADR 0009's admission bar, and keeping it is what lets the existing with-row-id tests stand unchanged as evidence the relocation preserved behavior.

Docs: updating-data.md §"The options channel" owns the promise and the division of responsibility (rebuilding the whole map per render is supported; wrapper-manufactured values are rebuild-stable; your own closures are yours to keep stable), with the renderer helpers named as the remaining caveat. framework-composition.md §"Stateful components" gets a short pointer — its "not by re-rendering" line is right about rows but reads as forbidding the options pattern being blessed here. The with-row-id docstring's raw line becomes true.

No new browser test: six existing browser tests already mount real grids with (with-row-id :id), covering the runtime half of the relocation.

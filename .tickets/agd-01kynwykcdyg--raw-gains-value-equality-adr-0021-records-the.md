---
id: agd-01kynwykcdyg
title: Raw gains value equality; ADR 0021 records the rebuild-stability contract
status: open
type: bug
priority: 1
mode: afk
created: '2026-07-29T02:58:37.581392463Z'
updated: '2026-07-29T02:58:37.581392463Z'
tags:
- convert
- differ
- adr
acceptance:
- title: Two Raws wrapping the same fn are =; two Raws wrapping equal-but-distinct CLJS maps are =; Raws wrapping unequal values are not =
  done: false
- title: A tagged Raw is never = to an untagged Raw wrapping the same value
  done: false
- title: Equal Raws hash equally, and hashing a Raw never adds a property to the wrapped value
  done: false
- title: An options map built twice by the same fn, carrying :context (raw m), a top-level-def-d callback and :column-defs, produces zero setGridOption calls and zero warnings through update-grid!
  done: false
- title: 'Positive control in the same test: changing one key produces exactly one setGridOption call'
  done: false
- title: ADR 0021 is written, with the renderer helpers named as an open gap; ADR 0005/0008/0009 carry cross-reference pointers and 0009 carries the admission-bar clause
  done: false
- title: CONTEXT.md carries the Rebuild-stable and Deferred value terms
  done: false
---

## Description

A rebuilt options map carrying `:context (ag/raw {...})` — ADR 0005 §4's canonical idiom, and an `:initial? true` key — draws a spurious "initial-only and cannot change" warning on the first `update-grid!` after mount, and churns `setGridOption` for updatable raw-valued keys. Raw is a deftype with no IEquiv, so `=` falls through to identity and two Raws wrapping the same value are never equal.

This ticket makes Raw compare by value and records the contract the whole change series serves: every value the wrapper manufactures must be `=` to itself given `=` inputs, so a consumer who rebuilds the options map during render (the only shape available to a React-family consumer, since ADR 0012 rules out adapters) gets a clean diff. The consumer's own callbacks remain the consumer's half of that contract.

Discovered downstream: the differ has exactly one non-test call site in this repo and the Fulcro reference consumer never calls it, so the whole-rebuilt-map path was never exercised here.

## Design

Equality: compare the wrapped value with `=`, not `identical?` — that is what makes `(raw {:a 1})` values compare equal and fixes the `:context` case; for functions `=` degrades to identity anyway. The tag introduced in the next ticket participates in equality. IHash derives from the tag alone: the natural `(hash [x tag])` routes a wrapped JS object or function through `goog/getUid`, which mutates it with a `closure_uid_` property, and the values we wrap include consumer renderer classes and callbacks.

ADR 0021 records the promise and the full mechanism, including the tagged dispatch built in the next ticket, since the decision was taken as a whole. Considered options to record, all rejected in design: a key-keyed rule at the converter (`->js` recursion is depth-uniform, so a rule on the prop `getRowId` also fires on a consumer's own `:get-row-id` nested inside `:cell-renderer-params` or a non-raw `:context`, silently replacing their value — and it contradicts ADR 0005 §1-2's mechanical, no-key-tables law); builder-internal memoization (does not generalize to the renderer helpers, whose lifecycle map is a fresh object every call); a marker record (records are maps, so the type test must precede the `map?` branch, and a hand-written `{:get-row-id :id}` silently converts to a string); a public tag with consumer defmethods (makes the boundary an open extension point, against ADR 0005's one-law promise, and leaves Raw meaning two incompatible things in the docs).

ADR cross-references, as pointers not rewrites (repo practice is correction notes only): ADR 0005 gains a pointer — §4's "sole escape hatch" stays literally true because the tag is internal; ADR 0008 gains a pointer — its `=` differ now has a stated counterpart obligation on the wrapper's own output; ADR 0009 gains the admission-bar clause, stated in 0021 as covering any public fn contributing an option value, not just builders. ADR 0004's whole-map phrasing needs no correction — it becomes the accurate reading.

CONTEXT.md gains two terms, Rebuild-stable and Deferred value. The Raw entry stays untouched: it describes the public one-arity fn, which still means verbatim.

Testing is node-only per ADR 0015 — every assertion is about our contract, not AG Grid's runtime.

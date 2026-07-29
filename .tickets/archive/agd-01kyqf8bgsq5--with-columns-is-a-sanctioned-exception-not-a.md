---
id: agd-01kyqf8bgsq5
title: with-columns is a sanctioned exception, not a coercion
status: closed
type: task
priority: 3
mode: afk
created: '2026-07-29T17:37:46.004826761Z'
updated: '2026-07-29T17:44:44.106617485Z'
closed: '2026-07-29T17:39:55.343207039Z'
tags:
- builders
- adr
- docs
acceptance:
- title: with-columns has a docstring teaching the :field<->row pairing; nothing in it claims vec coerces
  done: true
- title: ADR 0009 carries the dated blockquote; membership and the coerce-or-bundle bar unchanged
  done: true
- title: CONTEXT.md Builder entry is true of all 8 catalog entries
  done: true
- title: Node :test build green
  done: true
- title: No stale coercion claim for with-columns survives a repo grep
  done: true
- title: ADR 0019 line 83 carries no dangling ticket reference
  done: true
---

## Description

ADR 0009 admits `with-columns` as catalog entry #2 on the justification "coercion: assoc `:column-defs`, vec-coerced". That justification is hollow:

- `->js` already converts anything `sequential?` (impl/convert.cljs), so lazy seqs, lists and ranges need no `vec`.
- `(= [{:field :id}] '({:field :id}))` is true, so `vec` buys no rebuild stability (ADR 0021) either.
- The only input `vec` rescues is a set, which is meaningless for ordered columns.
- Every call site in the repo — 3 dev apps, 9 browser tests, the unit tests, 4 docs — passes a literal vector, so it has never fired.

On a strict reading of the coerce-or-bundle bar this is naming-only sugar, the same charge that dropped `with-default-col-def`. But `with-columns` earns its keep the way `with-row-data` does, as a sanctioned exception: it teaches the `:field`<->row-spelling pairing, which the kebab<->camel reference table cannot express. It had no docstring at all, so that teaching was not being done.

Resolution: keep the function and keep `vec` (demoted to incidental normalization, no longer the justification); write the docstring; annotate the record rather than rewrite it.

ADR 0019 line 83 cited ticket agd-01kyjsd6sk2s as the home for amendments to ADR 0009, but that ticket was never created. The dangling reference is dropped rather than repointed at a replacement: line 83 now records the open coercion question in the ADR's own prose and names no ticket, which is what "this record is self-contained" asks for anyway.

## Design

1. Docstring on `with-columns`: the `:field`<->row pairing and its blank-cell failure mode, the field check as the safety net, both recipes deferred to "Options and conversion", and ADR 0009's "columns stay raw-map / no per-column builder" decision.
2. ADR 0009: dated in-place blockquote correcting entry #2's justification, using the annotation mechanism ADR 0007 established. Membership and the bar are unchanged; no new ADR, since nothing reverses.
3. ADR 0019: drop the dangling ticket reference from line 83; the open `with-data-type-definitions` coercion question stays recorded in the ADR's own prose, where its analysis already lives.
4. CONTEXT.md: the **Builder** term excludes `with-row-data` and (now) `with-columns`, 2 of 8 entries. Add a third clause for the teaching exception; keep the "never merely renames an option" teeth.

`vec` is deliberately kept: removing it changes behavior (nil -> nil rather than [], sets start warning at the boundary) for nothing but tidiness, and it keeps `(:column-defs opts)` a concrete indexable vector when columns are built programmatically.

## Notes

**2026-07-29T17:39:55.343207039Z**

with-columns keeps its catalog seat but not its stated reason for having one. ADR 0009 justified entry #2 as "coercion: vec-coerced"; that is hollow — ->js already converts anything sequential?, (= [x] '(x)) so there is no rebuild-stability gain either, the only input vec rescues is a set (meaningless for ordered columns), and every call site in the repo passes a literal vector. It is admitted instead as a sanctioned exception alongside with-row-data, earning its keep by teaching the :field<->row-spelling pairing the kebab<->camel reference table cannot express — a docstring it previously lacked entirely.

vec is kept, demoted to incidental normalization: removing it changes behavior (nil -> nil rather than [], sets start warning at the boundary) for nothing but tidiness, and it keeps (:column-defs opts) a concrete indexable vector when columns are built programmatically.

ADR 0009 is annotated, not rewritten — a dated blockquote above the catalog list, the mechanism ADR 0007 established — since membership and the coerce-or-bundle bar are both unchanged and no decision reverses. CONTEXT.md's Builder term gained a third clause: it excluded with-row-data and would now exclude with-columns, 2 of 8 entries.

ADR 0019 line 83 cited agd-01kyjsd6sk2s as the home for amendments to ADR 0009, a ticket that was never created. The reference is dropped rather than repointed at a replacement: the surviving with-data-type-definitions coercion question stays in ADR 0019's own prose, where its analysis lives, and no ticket is named.
---
id: agd-01kygja77mxj
title: Literal-key fallback in callback beans
status: closed
type: feature
priority: 2
mode: afk
created: '2026-07-27T01:16:31.859984302Z'
updated: '2026-07-27T15:00:05.603928205Z'
closed: '2026-07-27T15:00:05.603928205Z'
tags:
- conversion
- bean
links:
- agd-01kygjftnhwa
- agd-01kygjg6avt2
- agd-01kygjge8hrb
deps:
- agd-01kygjftnhwa
acceptance:
- title: 'Literal lookup works: (:first-name (:data p)) reads "first-name"'
  done: true
- title: Camel lookup remains compatible and wins when both spellings are present, including falsy camel values
  done: true
- title: Fallback reaches nested-only dashed keys and object elements inside arrays
  done: true
- title: Heterogeneous camel and kebab rows work regardless of row order
  done: true
- title: AG Grid props retain camel priority on callback params and RowNode objects
  done: true
- title: Direct-data callbacks and RowNode.data receive the same callback-bean lookup law
  done: true
- title: with-row-id keyword lookup supports camel and literal dashed row keys without bean allocation
  done: true
- title: Implementation has no row-shape scan, per-grid gate, GridApi WeakMap, or registry dependency
  done: true
- title: A browser test renders a string-field kebab row and reads it through a real callback
  done: true
- title: Raw callbacks remain unbeaned and unchanged
  done: true
- title: Vendored cljs-bean bodies have no changes beyond those already documented in THIRD-PARTY.md
  done: true
- title: Node and realistic browser performance measurements are recorded after key-transform optimization
  done: true
- title: Callback bean writes are documented as unsupported for AG Grid object mutation
  done: true
- title: Write-like operations over callback beans do not become fallback-aware mutation semantics
  done: true
- title: Callback return conversion remains the normal EDN->JS path and may camelize bean-derived keyword keys
  done: true
---

## Description

Kebab-keyed consumer data is unreachable from today's callback beans. `clj->js` turns `{:first-name "Ada"}` into a JS object carrying `"first-name"`, while keyword lookup camelizes unconditionally and asks for `firstName`.

Give every auto-beaned callback object one deterministic **literal-key fallback**: a keyword resolves to its camelized property when that property is present, otherwise to its literal name. Camel keeps priority. The rule is object-local and recursive; it does not infer a grid-wide row shape.

This changes callback access only. A kebab-keyed row still needs a string column field such as `{:field "first-name"}` to render. Raw callbacks remain raw.

The accepted design is ADR 0018 (`docs/adr/0018-literal-key-fallback-callback-beans.md`).

## Design

Use one resolver for callback bean reads and wrapper-owned row reads:

```clojure
literal = (name k)

if literal has no "-":
  literal
else:
  camel = kebab->camel(literal)
  if object contains camel: camel
  else: literal
```

Presence, not truthiness, chooses the camel property. A present `false`, `nil`, or `undefined` camel value still wins.

`params-bean` starts an object-aware bean at every object argument. Its cljs-bean `:transform` creates another object-aware bean for each recursively reached object, so nested objects and object elements inside arrays capture the object they test. Apply this to the whole callback-bean tree rather than identifying `params.data`: `wrap-fn` also beans direct row arguments, `RowNode` arguments, and object cell values.

This is a lookup/read law, not a callback-bean write contract. `assoc`, `dissoc`, `conj`, `update`, and nested updates are ordinary cljs-bean collection operations; they do not mutate AG Grid params, rows, `RowNode.data`, nested objects, or arrays. Their results may snapshot or clone values, and any non-raw callback return still crosses the normal EDN→JS converter, where keyword keys camelize. A write-like return can therefore create `firstName` even when the original object carried only `"first-name"`.

Callbacks that must mutate AG Grid objects or preserve literal data-return keys should use `(ag/raw f)`, explicit JS objects/string keys, or explicitly unwrap the backing JS object and mutate it through JS/API calls. Do not change vendored cljs-bean bodies to support fallback-aware writes.

Do not add a `WeakMap`, inspect the first row, scan row keys, subscribe to grid events, or expose a row-style option. Those approaches make correctness depend on row order or callback shape. The no-dash branch keeps common reads such as `:value`, `:data`, `:node`, `:api`, and `:id` on the direct path.

Reuse the resolver in the keyword form of `with-row-id`; keep its callback raw and allocation-free. Do not change vendored cljs-bean bodies. Caching nested bean instances is optional implementation work, not observable contract.

Key transforms are already optimized (agd-01kygjftnhwa; methodology and recorded runs in docs/research/key-transform-benchmarks.md — nested-bean construction is the remaining measured cost, and ADR 0018 §8 permits caching nested beans). Record realistic browser render and 100k-row sort/filter measurements. If deterministic fallback misses the performance bar, do not replace it with shape sampling; retain camel-normalized rows as the documented recipe. `(ag/raw f)` remains the explicit hot-path opt-out.

## Verification

- Node tests: literal and camel rows, camel priority with falsy values, nested-only dashed keys, array elements, heterogeneous row order, direct-data callbacks, `RowNode.data`, root AG Grid vocabulary, `with-row-id`, and `raw`.
- Browser test: a string field renders a kebab-keyed row and a real AG Grid callback reads the same key.
- Vendoring check: no changes beyond the namespace, `^:no-doc`, and license-header modifications already documented in THIRD-PARTY.md.
- Performance evidence: optimized baseline versus broad fallback in node and realistic browser paths.

## Notes

**2026-07-27T13:49:34.226012223Z**

Post-transform-optimization benchmarks for the ADR 0018 prototype are recorded in docs/research/key-transform-benchmarks.md (run 'bb bench'; the bench-local prototype lives in src/bench/ag_grid_cljs/bench/transforms.cljs). Release-build headline: the fallback branch itself is cheap — kebab and camel rows cost the same (706 vs 692 ns for a nested :first-name read) and dashless reads are free (18.7 vs 20.6 ns on a live bean). The cost is nested-object construction: a resolver closure plus a bean per reached object takes ':col-def on a live bean' from 20 to 384 ns and the nested :data read from 221 to 692 ns. So the open perf question is nested-bean construction (ADR 0018 §8 already permits caching a nested bean), not the lookup law. Browser render and 100k-row sort/filter measurements are still outstanding.

**2026-07-27T13:56:12.793338458Z**

Correction to the earlier bench note: the recorded numbers were re-measured after the lookup memo changed. Release headline is unchanged in shape — dashless reads free (25.5 vs 20.9 ns on a live bean), kebab and camel rows equal (723 vs 683 ns), nested-object construction the cost (:col-def on a live bean 20 -> 372 ns, nested :data read 207 -> 683 ns). See docs/research/key-transform-benchmarks.md for the current table.

**2026-07-27T14:59:53.432337563Z**

Review notes at implementation (commit fd954ea): (1) Presence is own-property — js-in was caught by review resolving :value-of to the inherited Object.prototype.valueOf, permanently shadowing a literal "value-of" row key; the resolver and with-row-id use js/Object.hasOwn, pinned by tests. (2) Beaning predicate boundary: the fallback law rides cljs-bean's existing plain-object (object?) predicate. Real class instances stay raw exactly as before — (:api p) keeps its ADR 0010 raw-GridApi contract, and a real browser RowNode reached as (:node p) or passed directly is still a raw class instance. Node tests model RowNode as a plain object (like the rest of the suite's fakes); the RowNode acceptance items hold at that level. Making class instances beanable would break (.method (:api p)) and Date cell values in comparators, so a uniform broadening was rejected. (3) Vendoring verified by inspection (git diff on the vendored files is empty); no automated diff-against-upstream check was built. (4) Nested-bean WeakMap memo is scoped to transform-reached objects — memoizing per-call root params objects measured worse (4.9s vs 2.6s on the 100k browser sort) from dead-key GC pressure.

**2026-07-27T15:00:05.603928205Z**

Shipped the ADR 0018 literal-key fallback (commit fd954ea). params-bean resolves keywords camel-first, literal-second per object: dashless keys stay on the direct path; dashed keys pay one bounded-memo transform plus an Object.hasOwn own-property presence test (prototype members cannot shadow literal keys). Its :transform gives every recursively reached plain object — nested objects and array elements — its own object-aware bean, with nested beans memoized in a WeakMap scoped to transform-reached objects (ADR 0018 §8). with-row-id's keyword form follows the same law bean-free. Raw callbacks and vendored cljs-bean untouched. Covered by 13 node tests plus a browser test where a string-field kebab row renders and a real value-getter reads the same key. Measurements recorded in docs/research/key-transform-benchmarks.md (bb bench / new bb bench-browser): node steady-state at parity with the pre-fallback bean; 100k-row browser sort ~20% over the pre-fallback wrapper (2.13 vs 1.78s), with :value-cache true (0.30s) and ag/raw (0.15s) as the hot-path recipes. Docs: options-and-conversion.md states the lookup law and that callback-bean writes are not AG Grid object mutation. Boundary note on the ticket: class instances (GridApi, real RowNode) remain raw per the existing object? predicate.

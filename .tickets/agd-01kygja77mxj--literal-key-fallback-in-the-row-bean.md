---
id: agd-01kygja77mxj
title: Literal-key fallback in callback beans
status: open
type: feature
priority: 2
mode: afk
created: '2026-07-27T01:16:31.859984302Z'
updated: '2026-07-27T12:16:01.034503384Z'
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
  done: false
- title: Camel lookup remains compatible and wins when both spellings are present, including falsy camel values
  done: false
- title: Fallback reaches nested-only dashed keys and object elements inside arrays
  done: false
- title: Heterogeneous camel and kebab rows work regardless of row order
  done: false
- title: AG Grid props retain camel priority on callback params and RowNode objects
  done: false
- title: Direct-data callbacks and RowNode.data receive the same callback-bean lookup law
  done: false
- title: with-row-id keyword lookup supports camel and literal dashed row keys without bean allocation
  done: false
- title: Implementation has no row-shape scan, per-grid gate, GridApi WeakMap, or registry dependency
  done: false
- title: A browser test renders a string-field kebab row and reads it through a real callback
  done: false
- title: Raw callbacks remain unbeaned and unchanged
  done: false
- title: Vendored cljs-bean bodies have no changes beyond those already documented in THIRD-PARTY.md
  done: false
- title: Node and realistic browser performance measurements are recorded after key-transform optimization
  done: false
- title: Callback bean writes are documented as unsupported for AG Grid object mutation
  done: false
- title: Write-like operations over callback beans do not become fallback-aware mutation semantics
  done: false
- title: Callback return conversion remains the normal EDN->JS path and may camelize bean-derived keyword keys
  done: false
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

Optimize key transforms first (agd-01kygjftnhwa), then record warmed node measurements and realistic browser render plus 100k-row sort/filter measurements. If deterministic fallback misses the performance bar, do not replace it with shape sampling; retain camel-normalized rows as the documented recipe. `(ag/raw f)` remains the explicit hot-path opt-out.

## Verification

- Node tests: literal and camel rows, camel priority with falsy values, nested-only dashed keys, array elements, heterogeneous row order, direct-data callbacks, `RowNode.data`, root AG Grid vocabulary, `with-row-id`, and `raw`.
- Browser test: a string field renders a kebab-keyed row and a real AG Grid callback reads the same key.
- Vendoring check: no changes beyond the namespace, `^:no-doc`, and license-header modifications already documented in THIRD-PARTY.md.
- Performance evidence: optimized baseline versus broad fallback in node and realistic browser paths.
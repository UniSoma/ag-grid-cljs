# 0018. Literal-key fallback in callback beans

- Status: accepted, 2026-07-27
- Origin: knot ticket agd-01kygja77mxj (tickets are ephemeral; this record is self-contained)

Callback beans gain a **literal-key fallback**. For every auto-beaned JS object, a keyword lookup uses its camelized property when that property is present and otherwise uses the keyword's literal name. The decision is object-local and deterministic: no row-shape inference, per-grid gate, `WeakMap`, registry, or callback-order dependence. Camel keeps priority, so existing AG Grid vocabulary and camel-keyed data retain their current meaning.

## Context

ADR 0005 §7 settled that functions in the options tree are auto-wrapped. Object arguments arrive as lazy cljs-bean views whose keywords map mechanically from kebab to camel: `:row-index` reads `rowIndex`, and nested objects inherit the same mapping. The view is lazy and never converts the underlying JS object.

That rule makes kebab-keyed consumer data unreachable. `clj->js` keys objects with `(name k)`, so `{:first-name "Ada"}` becomes `{"first-name": "Ada"}`. A callback expression such as `(:first-name (:data p))` asks for `firstName` and returns nil while the literal property is present.

Rendering is a separate concern. `{:field :first-name}` emits `"firstName"`, while `{:field "first-name"}` emits the consumer's string verbatim. A kebab-keyed row therefore needs a string field to render. Fixing callback lookup must not silently rewrite row data or column fields.

The callback surface is broader than `params.data`. `wrap-fn` beans every object argument. AG Grid also invokes callbacks with row data directly (`getDataPath`, `isRowMaster`, `getServerSideGroupKey`), with a `RowNode` (`isRowSelectable`, `doesExternalFilterPass`), and with arbitrary object cell values (`equals`, comparators). A rule scoped to `params.data` cannot cover the interface the wrapper actually exposes.

Three invariants drive the decision:

1. The same object and key must resolve the same way regardless of grid history or row order.
2. Existing camel lookups must keep their value, including when both camel and literal spellings exist.
3. The wrapper must not infer a permanent grid-wide data shape from a sample.

`key->prop` receives only the lookup key, not the object whose property it will read. Object-local presence testing therefore needs one closure per bean. cljs-bean's public `:transform` option supplies each nested object so it can receive a bean with the correct closure.

cljs-bean also implements ordinary collection update protocols, but callback beans are a lazy read view, not a row mutation API. `assoc`, `dissoc`, `conj`, `update`, and `update-in` either snapshot or clone values under cljs-bean's existing semantics. They do not provide object-local, fallback-aware mutation of AG Grid objects.

## Decision

1. **One callback-bean lookup law.** Every auto-beaned JS object uses camel-first, literal-second lookup. Given keyword `k` and object `o`:

   - let `literal` be `(name k)`;
   - if `literal` contains no `-`, return it directly because camel and literal are identical;
   - otherwise compute `camel` with `kebab->camel`;
   - return `camel` when that property is present on `o`, otherwise return `literal`.

   Presence, not truthiness, decides priority. A present camel property containing `nil`, `false`, or `undefined` still wins. Only lookups that miss today can gain a value.

   This is a lookup law. It covers keyword reads and lookup-like operations over the bean interface, such as `get`, invocation, `contains?`, and `find`. It does not define write semantics for persistent or transient collection operations.

2. **Apply the law to the whole callback-bean tree.** `params-bean` starts an object-aware bean at the root, and its `:transform` creates another object-aware bean for each recursively reached object. Arrays remain lazy vectors whose object elements receive the same treatment. This covers params, `params.data`, `params.node.data`, direct row arguments, `RowNode` arguments, custom params, and object cell values without classifying them.

3. **AG Grid vocabulary stays stable through camel priority.** On a params object carrying `rowIndex`, `(:row-index p)` still reads `rowIndex`. If a consumer object also carries a literal `"row-index"`, camel remains authoritative wherever both are present. No AG Grid key list or production deny-list is needed.

4. **Reuse the resolver in wrapper-owned row reads.** The keyword form of `with-row-id` remains a raw callback, but its property read follows the same camel-first, literal-second law. This preserves its allocation-free hot path while making `(with-row-id :record-id)` work with either `recordId` or `"record-id"` rows.

5. **No shape gate or global state.** Do not inspect the first row, scan `js-keys` to classify a grid, cache a boolean by GridApi, subscribe to row events, or expose a mode option. The property decision happens at the lookup that has both facts required to answer it: the object and the key.

6. **No change to vendored cljs-bean bodies.** Implement the rule through `:prop->key`, `:key->prop`, `:recursive`, and `:transform`. THIRD-PARTY.md's narrower claim remains true: apart from the already documented namespace, `^:no-doc`, and license-header changes, the vendored code bodies remain unchanged.

7. **Optimize deterministic lookup, not correctness.** The no-dash branch avoids conversion and presence testing for common reads such as `:value`, `:data`, `:node`, `:api`, and `:id`. Key-transform fast paths and scoped memoization are independent work. Benchmark the broad callback-bean rule after those changes against realistic render and 100k-row sort/filter paths. If the deterministic fallback cannot meet the performance bar, reject the feature and document camel-normalized rows rather than introduce a sampling heuristic. `(ag/raw f)` remains the explicit opt-out for hot callbacks.

8. **Do not make bean identity a contract.** Caching a nested bean may be used if measurement justifies it, but repeated `(:data p)` calls need only return equivalent views over the same JS object. Object identity and cache lifetime are implementation details.

9. **Do not support callback-bean writes as object mutation.** `assoc`, `conj`, `update`, and nested updates over a callback bean are ordinary CLJS collection operations; they may snapshot into persistent maps or cloned array-backed values and then cross the callback return boundary as normal EDN. `dissoc` may clone a JS object under cljs-bean's existing implementation, but it is not a supported AG Grid object mutation operation and must not be specified through the fallback resolver. Code that must mutate AG Grid objects should use `(ag/raw f)` or explicitly unwrap the backing JS object and use JS mutation/API calls.

## Consequences

- Kebab-keyed data from bare `clj->js` becomes reachable in callbacks, including nested-only dashed keys and arrays of nested objects.
- Heterogeneous rows behave independently of order. A camel first row cannot strand a later kebab row.
- Direct-data and `RowNode` callbacks follow the same law as ordinary params callbacks.
- Literal custom properties on non-row callback objects become reachable when their camel spelling is absent. This broadening is intentional; camel priority means no existing successful lookup changes.
- Common undashed lookups need neither camelization nor a presence check. Dashed lookups pay the object-local presence test.
- An object carrying both `firstName` and `"first-name"` remains ambiguous when enumerated: `prop->key` collapses both to `:first-name`, lookup returns the camel value, `keys` reports the key twice, and `(into {} bean)` may retain the later entry. Detecting or policing such rows would require a hot-path key scan and is not justified.
- The field check remains correct and separate. It compares emitted field strings with row keys; the callback fallback does not make `{:field :first-name}` render a `"first-name"` property.
- `(ag/raw f)` remains fully raw: raw JS arguments in, raw return out, with no literal-key fallback.
- Callback bean update operations are not a data update channel. `(assoc bean :first-name v)` and `(update-in bean [:data :first-name] f)` do not mutate the underlying row or params object. If their result is returned from a non-raw callback, the normal forward converter applies, so keyword keys may camelize to properties such as `firstName`.
- A bean-derived persistent map can therefore create `firstName` on callback return even when the original JS object carried only `"first-name"`. Use string keys, explicit JS objects, or `(ag/raw f)` for data-returning callbacks that must preserve literal property names.
- The previous **row bean** distinction disappears. The **callback bean** itself owns the lookup law, which keeps the conversion boundary's interface smaller and more predictable.

## Evidence and verification

The row-scoped prototype established that cljs-bean's public options can implement object-local fallback without changing the vendored files. On Node/V8 with memoized transforms, its construct-plus-one-row-read path measured 1772 ns against a 1092 ns baseline, and a renderer reading only `:value` measured 932 ns against 810 ns. Those figures prove feasibility but do not justify a first-row gate, and they do not measure the broader rule accepted here.

Verification therefore covers both read semantics and cost:

- node contract tests for camel priority, literal fallback, falsy values, nested-only dashed keys, arrays, heterogeneous row order, direct data, `RowNode.data`, and `with-row-id`;
- a browser test proving a string field renders a kebab-keyed row and a real AG Grid callback reads the same key;
- warmed node microbenchmarks plus browser render and 100k-row sort/filter measurements after key-transform optimization;
- a vendoring check that allows only the modifications already documented in THIRD-PARTY.md;
- documentation or contract coverage that callback-bean writes are unsupported as object mutation and that callback returns still use the normal forward converter.

## Considered options

- **Camelize rows at the `clj->js` boundary** with a camelizing `:keyword-fn` — retained as the zero-read-overhead recipe, rejected as the only answer. It forces consumer data into AG Grid's vocabulary to satisfy callback-bean mechanics. The JS-by-contract warning must still describe this recipe accurately.
- **Fallback only under `params.data` / `params.node.data`** — rejected. It needs identity matching, leaves direct-data and `RowNode` callback shapes incomplete, and makes the wrapper classify objects that the uniform function wrapper deliberately treats alike.
- **Per-grid first-row gate in a `WeakMap<GridApi, boolean>`** — rejected. It makes behavior depend on row order, misses rows whose dashed keys exist only below an undashed root, and cannot cover object callback arguments without a GridApi. A false result cannot both be cached forever and later move monotonically to true without re-sampling.
- **Per-row or repeated shape sampling** — rejected. It scans keys to predict a decision that the lookup itself can make exactly. Caching also becomes stale when rows are heterogeneous or change shape.
- **A mapper built from column fields** — rejected. It misses keys with no column, nested collection keys, and direct callback data. Giving it precedence can also shadow AG Grid params vocabulary.
- **A generated AG Grid collision list** — rejected. It would move the version-pinned key registry into production correctness, contrary to ADR 0007's dev-only posture.
- **An explicit grid option for literal rows** — rejected. It expands the interface and can be configured incorrectly, while object-local lookup has the facts needed to decide safely.
- **Making bean writes follow the fallback resolver** — rejected. The feature is about callback reads. Vendored cljs-bean's update operations snapshot or clone values, and specifying object-local write behavior would either require changing vendored bodies or teaching users a second, surprising mutation surface. Explicit JS mutation through `raw` or `bean/object` is clearer.
- **Forking the vendored bean** — rejected. Prototype measurements found no property-read advantage, while a fork would turn future cljs-bean syncs from copies into merges.

## References

- ADR 0003 — row data is JS by contract
- ADR 0005 §7–8 — callback beans, `raw`, and the vendored cljs-bean slice
- ADR 0007 §1 — key registry production dead-code-elimination posture
- ADR 0010 — uniform function-valued callback surface
- ADR 0015 — node/browser testing split
- ADR 0017 — field check and the separate rendering failure

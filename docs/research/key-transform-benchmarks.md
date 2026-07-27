# Key-transform and callback-bean benchmarks

Numbers behind the transform fast paths (ticket agd-01kygjftnhwa) and the
performance question ADR 0018 §7 asked about the literal-key fallback, answered
by the shipped implementation (ticket agd-01kygja77mxj).

## Methodology

- Node harness: `src/bench/ag_grid_cljs/bench/transforms.cljs`, run with
  `bb bench`. It compiles the `:bench` build twice — dev (no optimizations,
  `goog.DEBUG=true`) and release (`:advanced`, `goog.DEBUG=false`) — and runs
  each under node.
- 500 000 measured calls per case after 50 000 warmup calls, timed with
  `process.hrtime.bigint`, reported as nanoseconds per call.
- Every measured value is written to a global sink so `:advanced` cannot
  eliminate the timed work.
- The pre-change transform bodies and the pre-fallback mechanical bean are kept
  in the harness as baselines, so before and after come from the same run on
  the same machine.
- Browser harness: `src/bench/ag_grid_cljs/bench/browser_grid.cljs`, run with
  `bb bench-browser` (release build, headless Chromium via
  `test/browser/bench.mjs`). Each variant mounts a real 100k-row grid with a
  value-getter column, measures createGrid→firstDataRendered, then times the
  synchronous `applyColumnState` sort and `quickFilterText` calls. Two passes
  run with a GC between variants; pass 2 is recorded.
- Runs below: node v24.18.0 / Chromium (Playwright), Linux x86-64, single run
  per build. Case labels are the ones the harnesses print. Treat the numbers
  as magnitudes, not a contract — successive runs of the same build move by
  10–30 ns on the node bean cases and by 10–20% on the browser cases, which is
  enough to reorder neighbours.

## Results (ns per call)

### Standalone transforms

| Case                                  | dev   | release |
|---------------------------------------|-------|---------|
| baseline `kebab->camel "first-name"`  | 570.9 | 593.3   |
| baseline `kebab->camel "value"`       | 270.3 | 309.9   |
| baseline `camel->kebab "firstName"`   | 261.0 | 286.8   |
| baseline `camel->kebab "value"`       | 162.7 | 177.6   |
| `kebab->camel "first-name"`           | 83.1  | 98.5    |
| `kebab->camel "value"`                | 11.3  | 13.4    |
| `kebab->camel "row-data"` (memoized)  | 8.5   | 11.4    |
| `camel->kebab "firstName"`            | 279.9 | 298.6   |
| `camel->kebab "first-name"`           | 24.7  | 26.4    |
| `camel->kebab "value"`                | 19.6  | 22.2    |
| `camel->kebab "firstName"` (memoized) | 8.9   | 10.7    |
| `lookup-prop :value` (memo bypassed)  | 13.5  | 12.4    |
| `lookup-prop :row-index` (memo hit)   | 12.8  | 16.1    |

The dashless fast paths are ~28x (`kebab->camel`) and ~8x (`camel->kebab`)
cheaper than the bodies they replace. The dashed `kebab->camel` loop is ~6x
cheaper than the `split`/`apply str` version. `camel->kebab` on a genuinely
camel input is unchanged — it still pays the regex — which is fine, because that
direction only runs when a bean is enumerated (`keys`, `seq`, `into {}`), never
on the lookup path.

### Flat callback-bean lookup (pre-fallback mechanical bean)

The bean as it shipped between the fast paths and the ADR 0018 fallback:
mechanical `kebab->camel` lookup, no per-object resolver, no `:transform`.
Kept bench-local as the fallback's baseline.

| Case                                                            | dev   | release |
|-----------------------------------------------------------------|-------|---------|
| baseline construct + read `:value`                              | 639.0 | 548.3   |
| baseline construct + read `:row-index`                          | 956.0 | 924.4   |
| construct + read `:value`                                       | 257.8 | 181.5   |
| construct + read `:row-index`                                   | 270.8 | 182.0   |
| read `:value` on a live bean                                    | 21.9  | 23.1    |
| read `:col-def` on a live bean                                  | 36.3  | 21.7    |
| construct + read `:value` (unbounded memo, both directions)     | 195.7 | 143.3   |
| construct + read `:row-index` (unbounded memo, both directions) | 193.4 | 147.3   |
| construct + nested read `:first-name` of `:data`                | 293.3 | 210.3   |

Construct-plus-read is ~5x cheaper in release than the pre-fast-path
baseline (924 → 182 ns for a dashed key), and a dashed key costs about what a
dashless one costs, where before it carried a ~300 ns penalty.

## Decisions this supports

**Fast paths, both directions.** `kebab->camel` returns its argument when the
input has no `-`; `camel->kebab` returns its argument when the input is made
only of `[a-z0-9_$-]`. Non-ASCII input takes the slow path so its result is
unchanged (`"É"` still lower-cases). Parity for empty strings, leading, trailing
and doubled dashes, all-caps runs and digits is pinned by `key-transform-edges`
in `src/test/ag_grid_cljs/impl/convert_test.cljs`.

**A bounded memo, on the lookup direction only.** `convert/lookup-prop` caches
`kebab->camel` for dashed lookup keys in a `js/Map` of at most 512 entries,
clearing it when it fills. Dashless keys never enter it. The reverse direction,
`prop->key`, stays uncached because it receives arbitrary JS property names
during bean enumeration — the unbounded-cache-of-everything row above is exactly
what the ticket rules out, and it buys 30–40 ns of the bean path in this run.

Two things the numbers settled:

- The cache is keyed by **name**, not by keyword identity. Keying on the keyword
  object would need no bound at all, since a keyword literal is one hoisted
  constant — but only in a release build. Dev builds construct a fresh keyword
  per evaluation, and an identity-keyed cache measured 258 ns on
  `lookup-prop :row-index` there (against 82 ns for no cache at all), because
  every lookup missed and then inserted. A cache that only works in the build
  consumers do not develop against is worse than none.
- The bound **clears** rather than stops inserting. A lookup key can be built
  from runtime data — `(get (:data p) (keyword col-id))`, or the keys
  cljs-bean's `-equiv` passes back from a snapshot of arbitrary props — so a
  first-512-wins policy would let runtime junk fill the cache once and starve
  the real lookup sites for the rest of the process.

## ADR 0018 literal-key fallback (shipped `params-bean`)

The shipped implementation follows ADR 0018 §1–2: one object-local
camel-first/literal-second resolver closure per bean, with cljs-bean's
`:transform` making another object-aware bean for each recursively reached
plain object. Two cost levers ship with it:

- The resolver reuses the bounded `cached-camel` memo, so a dashed lookup pays
  one `js/Map` hit plus one own-property presence test (`Object.hasOwn` — the
  prototype chain deliberately does not count, so `Object.prototype.valueOf`
  cannot shadow a literal `"value-of"` key).
- Nested beans are memoized in a `WeakMap` keyed by the wrapped JS object
  (ADR 0018 §8 sanctions this: bean identity is an implementation detail).
  The memo is scoped to `:transform`-reached objects only — root callback
  params are fresh per call, and an earlier whole-tree memo measured *worse*
  in the browser (2.6 → 4.9 s on the 100k sort) because millions of dead
  WeakMap keys cost more than they save.

### Node (steady state: fixture objects are stable, so the memo hits)

| Case                                            | dev   | release |
|-------------------------------------------------|-------|---------|
| construct + read `:value`                       | 239.2 | 182.6   |
| read `:value` on a live bean                    | 23.5  | 27.1    |
| read `:col-def` on a live bean                  | 35.9  | 40.6    |
| camel row: nested read `:first-name` of `:data` | 293.2 | 220.3   |
| kebab row: nested read `:first-name` of `:data` | 298.9 | 229.2   |
| kebab row: nested read via a live bean          | 62.0  | 70.0    |

Against the mechanical bean above: dashless reads are free as ADR 0018 §7
predicted (27.1 vs 23.1 ns on a live bean, within run noise), kebab and camel
rows cost the same (229 vs 220 ns), and the memo brings the nested `:data`
read to parity with the pre-fallback bean (220.3 vs 210.3 ns). Before the
memo, every nested read allocated a resolver closure plus a bean and measured
~460–505 ns.

### Browser (100k-row grid, release build; pass 2 of `bb bench-browser`)

All variants auto-wrapped except the raw floor; the pre-fallback variant goes
through a faithful clone of the old wrapper so both sides pay the same
`wrap-fn` overhead. Milliseconds per operation.

| Variant                                     | render | sort   | quick-filter |
|---------------------------------------------|--------|--------|--------------|
| raw JS getter, camel rows                    | 83.9   | 153.8  | 32.5         |
| mechanical bean (pre-fallback), camel rows   | 68.8   | 1776.4 | 91.6         |
| fallback bean (shipped), camel rows          | 73.2   | 2134.1 | 104.9        |
| fallback bean (shipped), kebab rows          | 68.9   | 2140.4 | 103.6        |
| fallback bean + `:value-cache true`          | 72.8   | 296.7  | 32.6         |

What this settles for ADR 0018 §7's decision:

- **Render is unaffected.** Row virtualization means initial render touches a
  screenful of cells; every variant lands in the same 65–85 ms band.
- **The fallback itself is order-neutral and spelling-neutral.** Kebab rows
  cost the same as camel rows end to end.
- **The deterministic fallback meets the bar.** The adversarial path — a
  100k-row sort where AG Grid evaluates an auto-wrapped value-getter per
  comparison — costs ~20% over the pre-fallback wrapper (2.13 vs 1.78 s).
  The wrapper's bean machinery, not the fallback, dominates: both bean
  variants sit an order of magnitude above the 0.15 s raw floor.
- **The hot-path answers stay the documented ones.** AG Grid's own
  `:value-cache true` collapses per-comparison evaluation to once per row
  (0.30 s), and `(ag/raw f)` remains the full opt-out (0.15 s). Camel-normalized
  rows are not needed for performance, only as the zero-read-overhead recipe.

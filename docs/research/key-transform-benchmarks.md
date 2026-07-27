# Key-transform and callback-bean benchmarks

Numbers behind the transform fast paths (ticket agd-01kygjftnhwa) and the
performance question ADR 0018 §7 leaves open for the literal-key fallback
(ticket agd-01kygja77mxj).

## Methodology

- Harness: `src/bench/ag_grid_cljs/bench/transforms.cljs`, run with `bb bench`.
  It compiles the `:bench` build twice — dev (no optimizations, `goog.DEBUG=true`)
  and release (`:advanced`, `goog.DEBUG=false`) — and runs each under node.
- 500 000 measured calls per case after 50 000 warmup calls, timed with
  `process.hrtime.bigint`, reported as nanoseconds per call.
- Every measured value is written to a global sink so `:advanced` cannot
  eliminate the timed work.
- The pre-change transform bodies are kept in the harness as `baseline-*`, so
  before and after come from the same run on the same machine.
- Run below: node v24.18.0, Linux x86-64, single run per build. Case labels are
  the ones `bb bench` prints. Treat the numbers as magnitudes, not a contract —
  successive runs of the same build move by 10–30 ns on the bean cases, which is
  enough to reorder neighbours.

## Results (ns per call)

### Standalone transforms

| Case                                  | dev   | release |
|---------------------------------------|-------|---------|
| baseline `kebab->camel "first-name"`  | 581.0 | 547.7   |
| baseline `kebab->camel "value"`       | 299.9 | 278.1   |
| baseline `camel->kebab "firstName"`   | 281.2 | 262.4   |
| baseline `camel->kebab "value"`       | 157.0 | 160.5   |
| `kebab->camel "first-name"`           | 82.8  | 86.1    |
| `kebab->camel "value"`                | 10.9  | 10.4    |
| `kebab->camel "row-data"` (memoized)  | 8.5   | 10.1    |
| `camel->kebab "firstName"`            | 280.1 | 287.1   |
| `camel->kebab "first-name"`           | 24.0  | 22.4    |
| `camel->kebab "value"`                | 19.6  | 20.3    |
| `camel->kebab "firstName"` (memoized) | 8.5   | 8.5     |
| `lookup-prop :value` (memo bypassed)  | 11.2  | 10.1    |
| `lookup-prop :row-index` (memo hit)   | 13.2  | 14.1    |

The dashless fast paths are ~28x (`kebab->camel`) and ~8x (`camel->kebab`)
cheaper than the bodies they replace. The dashed `kebab->camel` loop is ~6x
cheaper than the `split`/`apply str` version. `camel->kebab` on a genuinely
camel input is unchanged — it still pays the regex — which is fine, because that
direction only runs when a bean is enumerated (`keys`, `seq`, `into {}`), never
on the lookup path.

### Flat callback-bean lookup (shipped `params-bean`)

| Case                                                            | dev   | release |
|-----------------------------------------------------------------|-------|---------|
| baseline construct + read `:value`                              | 641.0 | 508.9   |
| baseline construct + read `:row-index`                          | 963.1 | 830.3   |
| construct + read `:value`                                       | 264.1 | 172.0   |
| construct + read `:row-index`                                   | 265.7 | 177.5   |
| read `:value` on a live bean                                    | 20.6  | 20.9    |
| read `:col-def` on a live bean                                  | 31.4  | 19.7    |
| construct + read `:value` (unbounded memo, both directions)     | 185.0 | 143.6   |
| construct + read `:row-index` (unbounded memo, both directions) | 193.0 | 144.6   |
| construct + nested read `:first-name` of `:data`                | 302.1 | 206.6   |

Construct-plus-read is ~4.7x cheaper in release (830 → 178 ns for a dashed
key), and a dashed key now costs about what a dashless one costs, where before
it carried a ~300 ns penalty.

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

## ADR 0018 literal-key fallback prototype

Measured with the fast paths in place. The prototype (bench-local, not shipped)
follows ADR 0018 §1–2: one object-local camel-first/literal-second resolver
closure per bean, with cljs-bean's `:transform` making another for each
recursively reached object.

| Case                                            | dev   | release |
|-------------------------------------------------|-------|---------|
| construct + read `:value`                       | 299.2 | 224.6   |
| read `:value` on a live bean                    | 30.9  | 25.5    |
| read `:col-def` on a live bean                  | 441.8 | 371.6   |
| camel row: nested read `:first-name` of `:data` | 843.1 | 682.5   |
| kebab row: nested read `:first-name` of `:data` | 865.5 | 723.4   |
| kebab row: nested read via a live bean          | 550.7 | 494.5   |

Reading it against the shipped bean above:

- Dashless reads are free, as ADR 0018 §7 predicted: 25.5 vs 20.9 ns on a live
  bean, 225 vs 172 ns for construct-plus-read.
- Kebab and camel rows cost the same (723 vs 683 ns), so the fallback branch
  itself is cheap. What costs is reaching a nested object: every one allocates a
  fresh resolver closure plus a bean, which is why `:col-def` on a live bean
  goes 20 → 372 ns and the nested `:data` read goes 207 → 683 ns.
- So the open performance question for agd-01kygja77mxj is nested-bean
  construction, not the lookup law. ADR 0018 §8 already permits caching a nested
  bean, which is the lever to pull, and it must still be judged against the
  browser render and 100k-row sort/filter paths ADR 0018 requires.

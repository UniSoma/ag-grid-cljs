# 0003. Row data is JS by contract

- Status: accepted, 2026-07-22
- Origin: knot ticket agd-01ky0ed8d7k2 (tickets are ephemeral; this record is self-contained)

Consumers supply JS row objects directly; the wrapper never eagerly converts row collections. cljs-bean/js-interop ergonomics are provided at every callback boundary (inbound params as lazy kebab-keyed beans). A cljs.proxy outbound bridge was benchmarked and rejected as a row strategy.

## Context

Row data is the wrapper's hot path: AG Grid reads, enumerates, sorts, filters, and mutates row objects across ~100k-row datasets in its virtualized render loop. Three strategies were on the table:

- **JS by contract**: consumers supply `#js` row objects directly; no conversion ever happens on row collections.
- **Eager conversion**: consumers supply CLJS vectors of maps; the wrapper runs them through its `->js` conversion.
- **cljs.proxy outbound bridge**: rows stay kebab-keyed CLJS maps, wrapped in `cljs.proxy` (experimental namespace, ClojureScript 1.12.116+, available at our toolchain floor of shadow-cljs >= 3.3.0) so AG Grid reads them lazily as plain objects, paying only for fields actually touched. Its hypothesized win: avoid a 100k-row eager `->js` conversion while keeping rows idiomatic CLJS data.

The proxy question was settled by measurement, not argument.

### Benchmark setup

3 row strategies x 100k rows, ag-grid-community v36, shadow-cljs release build (advanced-optimized), headless Chromium. Prototype on branch `prototype/agd-01ky0ed8d7k2-cljs-proxy-bench` (commit 091d833): `src/dev/ag_grid_cljs/dev/bench.cljs` + `src/dev/public/bench.html` + `:bench` build.

- `:js` — `#js` array of `#js` objects (JS-by-contract baseline)
- `:proxy` — `#js` array of cljs.proxy-wrapped kebab CLJS maps (key-fn camel->kebab, cached)
- `:convert` — CLJS vector eagerly run through the lib's `->js` (the cost proxy would avoid)

### Findings

The decision driver was CORRECTNESS, not performance. Three independent AG Grid subsystems misread proxied rows:

- **Editable cell** (default valueSetter `data[field] = v`): THROWS `Cannot redefine property: salary`. cljs.proxy has no set trap and its `getOwnPropertyDescriptor` returns a value-less descriptor, so AG Grid cannot write the field.
- **Quick filter** (full scan): matched 0 rows vs 1 for js/convert — SILENT wrong result.
- **valueService.getCellValue(rowNode, col)**: returned null vs `"First12345"` — SILENT wrong result.

Direct field read via the get trap DOES work (getRowId ok; applyTransaction update applied=true; tx salary read-back=777). So the failures are AG Grid's enumeration / object-copy / defineProperty paths, which the proxy can't serve: `ownKeys` exposes kebab names while camelCase field `[[Get]]` expects camel, and descriptors carry no value. AG Grid does NOT treat rows as opaque `data[field]` accessors.

Performance (ms; representative run):

| metric             | js     | proxy  | convert | note |
|--------------------|--------|--------|---------|------|
| gen (build rows)   | 8.7    | 44.1   | 598.5   | proxy ~5x js (100k Proxy allocs); beats only eager-convert |
| mount              | 71.4   | 79.8   | 65.9    | parity |
| scroll 200 vp      | 2530   | 2412   | 2466    | PARITY — get-trap cost invisible vs AG Grid DOM work |
| sort               | 78.9   | 250.2  | 83.6    | proxy ~3x slower (comparator reads all rows via traps) |
| quick filter       | 85.9   | 126.8  | 87.6    | proxy ~1.5x slower AND wrong (0 matches) |
| applyTx 1k         | 14.3   | 13.8   | 14.4    | parity; proxy tx works (fresh proxied maps) |
| heap total (MB)    | ~11-48 | ~149   | ~0 (GC) | proxy ~3-13x heavier resident (Proxy objs + CLJS structs) |

Why proxy loses its own premise: its hypothesized win was lazy read avoiding a 100k `->js` conversion. But JS-by-contract means consumers supply JS rows directly (gen 8.7ms) — there is NO conversion to avoid. Proxy only beats the eager-convert strawman (599ms) already rejected, while delivering ZERO render-path benefit (scroll parity) and breaking edit/filter/value-read plus tripling-to-13x-ing memory.

## Decision

Row data is JS by contract:

- Consumers supply JS row objects directly.
- The wrapper never eagerly converts row collections.
- cljs-bean/js-interop ergonomics are provided at every callback boundary: inbound params arrive as lazy kebab-keyed beans (see ADR 0005 for the inbound bean contract).
- Do NOT ship cljs.proxy as default or opt-in row strategy.

## Consequences

- The row-data hot path pays zero wrapper overhead: generation, mount, scroll, sort, filter, and transactions all run at raw AG Grid speed.
- Consumers holding CLJS row data must convert it themselves at the edge — the wrapper deliberately does not hide that cost.
- cljs-bean/js-interop remain the INBOUND (callback-param) ergonomics answer, unchanged; cljs.proxy was only ever the inverse (outbound) candidate.
- Revisit only if a future CLJS proxy gains a set trap + ownKeys/key control + lower memory AND a concrete need to hold large row sets as CLJS maps emerges.

## Considered options

- **cljs.proxy outbound bridge** — REJECTED. Benchmarked head-to-head (setup and full evidence above): three correctness failures in AG Grid subsystems (editable cell throws `Cannot redefine property` for lack of a set trap and value-less descriptors; quick filter silently matches 0 rows; `valueService.getCellValue` silently returns null — because AG Grid enumerates, copies, and defineProperty's rows while the proxy exposes kebab `ownKeys` and value-less descriptors), ~3x slower sort, ~3-13x heavier resident memory, and no render-path benefit (scroll/mount parity). Its lazy-read premise is moot under JS-by-contract, since there is no eager conversion to avoid.
- **Eager conversion of CLJS row collections** — rejected: ~599ms to convert 100k rows vs 8.7ms supplying JS directly; a per-dataset tax the contract exists to avoid.

## References

- ADR 0005 — conversion boundary, including the inbound bean contract (callback params as lazy kebab-keyed beans)
- Evidence provenance: branch `prototype/agd-01ky0ed8d7k2-cljs-proxy-bench`, commit 091d833

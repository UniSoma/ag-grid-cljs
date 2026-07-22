---
id: agd-01ky0ed8d7k2
title: 'Walking skeleton: cljs.proxy outbound-bridge benchmark'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:26.150612799Z'
updated: '2026-07-22T01:47:07.640169683Z'
closed: '2026-07-22T01:47:07.640169683Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0ed83xww
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

Measure-and-decide: can cljs.proxy (ClojureScript 1.12.116+, experimental) serve as the outbound bridge so rows/options stay CLJS maps that AG Grid reads lazily? Benchmark against rows-as-JS in the virtualized cell-render hot path (~100k rows: initial render, fast scroll, sort/filter), and probe the mutation cases (editable cells, applyTransaction). Decision recorded: proxy viable as an opt-in/default row strategy, or not — the JS-by-contract decision stands either way as the safe default.

## Notes

**2026-07-20T19:01:31.612473154Z**

Context from charting-session research (recent ClojureScript releases):

- cljs.proxy landed as an EXPERIMENTAL namespace in ClojureScript 1.12.116 (2025-11-24; fixes in 1.12.134). ES Proxy-based lazy CLJS->JS bridging: JS reads CLJS maps as plain objects, paying only for fields actually touched. Usage: (require '[cljs.proxy :refer [builder]]). Sources: https://clojurescript.org/news/2025-11-24-release , https://github.com/clojure/clojurescript/blob/master/changes.md
- Caveats to probe here: read-oriented (mutation via editable cells / applyTransaction needs care); Proxy property access slower than raw JS field read — must benchmark in AG Grid's virtualized cell-render hot path; requires ES2016 target (default from 1.12.116).
- ^:async/await (1.12.145, 2026-05-07) — relevant to datasource callbacks, not this benchmark.
- cljs-bean dormant-but-stable (1.9.0, 2022); applied-science/js-interop actively maintained — both remain the INBOUND answer; cljs.proxy is the inverse (outbound).
- shadow-cljs >= 3.3.0 bundles CLJS 1.12.116, so the proxy is available at our toolchain floor.

**2026-07-22T01:47:00.858846311Z**

RESOLUTION — cljs.proxy REJECTED as row strategy. JS-by-contract stands as the sole default.

Benchmark: 3 row strategies x 100k rows, ag-grid-community v36, shadow-cljs release
(advanced-optimized), headless Chromium. Prototype on branch
prototype/agd-01ky0ed8d7k2-cljs-proxy-bench (commit 091d833): src/dev/ag_grid_cljs/dev/bench.cljs
+ src/dev/public/bench.html + :bench build. Playwright runner kept out of repo (scratchpad).
  :js      = #js array of #js objects (JS-by-contract baseline)
  :proxy   = #js array of cljs.proxy-wrapped kebab CLJS maps (key-fn camel->kebab, cached)
  :convert = CLJS vector eagerly run through the lib's ->js (the cost proxy would avoid)

DECISION DRIVER = CORRECTNESS, not perf. Three independent AG Grid subsystems misread proxied rows:
  - Editable cell (default valueSetter data[field]=v): THROWS "Cannot redefine property: salary".
    cljs.proxy has no set trap and its getOwnPropertyDescriptor returns a value-less descriptor,
    so AG Grid cannot write the field.
  - Quick filter (full scan): matched 0 rows vs 1 for js/convert — SILENT wrong result.
  - valueService.getCellValue(rowNode, col): returned null vs "First12345" — SILENT wrong result.
  Direct field read via the get trap DOES work (getRowId ok; applyTransaction update applied=true;
  tx salary read-back=777). So the failures are AG Grid's enumeration / object-copy / defineProperty
  paths, which the proxy can't serve: ownKeys exposes kebab names while camelCase field [[Get]] expects
  camel, and descriptors carry no value. AG Grid does NOT treat rows as opaque data[field] accessors.

PERFORMANCE (ms; representative run):
  metric              js      proxy    convert
  gen (build rows)    8.7     44.1     598.5     proxy ~5x js (100k Proxy allocs); beats only eager-convert
  mount               71.4    79.8     65.9      parity
  scroll 200 vp       2530    2412     2466      PARITY — get-trap cost invisible vs AG Grid DOM work
  sort                78.9    250.2    83.6      proxy ~3x slower (comparator reads all rows via traps)
  quick filter        85.9    126.8    87.6      proxy ~1.5x slower AND wrong (0 matches)
  applyTx 1k          14.3    13.8     14.4      parity; proxy tx works (fresh proxied maps)
  heap total (MB)     ~11-48  ~149     ~0(GC)    proxy ~3-13x heavier resident (Proxy objs + CLJS structs)

WHY IT LOSES ITS OWN PREMISE: proxy's hypothesized win was lazy read avoiding a 100k ->js conversion.
But JS-by-contract means consumers supply JS rows directly (gen 8.7ms) — there is NO conversion to
avoid. Proxy only beats the eager-convert strawman (599ms) we already rejected, while delivering ZERO
render-path benefit (scroll parity) and breaking edit/filter/value-read plus tripling-to-13x-ing memory.

OUTCOME: JS-by-contract remains the sole, safe row-data default. Do NOT ship cljs.proxy as default or
opt-in. cljs-bean/js-interop stay the INBOUND (callback-param) ergonomics answer, unchanged. Revisit
only if a future CLJS proxy gains a set trap + ownKeys/key control + lower memory AND a concrete need
to hold large row sets as CLJS maps emerges.

**2026-07-22T01:47:07.640169683Z**

cljs.proxy REJECTED as row strategy; JS-by-contract stays the sole default. Benchmark (100k rows, v36, release build, headless Chromium): proxy delivers scroll/mount PARITY (no render-path win — get-trap cost invisible vs AG Grid DOM work), the only lazy-read upside is moot (JS-by-contract means no eager conversion to avoid), and it fails three AG Grid subsystems — editable cell THROWS 'Cannot redefine property' (no set trap), quick-filter returns 0 matches, valueService.getCellValue returns null — plus ~3-13x memory. Prototype on branch prototype/agd-01ky0ed8d7k2-cljs-proxy-bench (091d833).

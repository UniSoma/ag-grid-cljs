---
id: agd-01ky0ed8d7k2
title: 'Walking skeleton: cljs.proxy outbound-bridge benchmark'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:26.150612799Z'
updated: '2026-07-20T19:01:31.612473154Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0ed83xww
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

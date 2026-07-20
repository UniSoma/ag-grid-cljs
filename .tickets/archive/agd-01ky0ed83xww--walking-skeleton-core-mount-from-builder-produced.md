---
id: agd-01ky0ed83xww
title: 'Walking skeleton: core mount from builder-produced EDN options'
status: closed
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:25.852612916Z'
updated: '2026-07-20T20:37:57.065694084Z'
closed: '2026-07-20T20:37:57.065694084Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0eck96vn
assignee: jonas
---

## Description

## Question

Prove the core design end to end at minimal scale: a shadow-cljs (>=3.3.0) app that mounts AG Grid Community via createGrid from an EDN options map produced by a small first cut of the builder API ((grid/options), with-columns, with-default-col-def), using the conversion rules decided in the conversion-boundary ticket. Functions must survive conversion; rows are JS by contract. This ticket also settles the library repo scaffold (deps.edn/package.json layout, dev app location). Asset: running prototype in this repo.

## Notes

**2026-07-20T20:24:16.447683339Z**

Skeleton built and headless-verified (commit a229630). Scaffold: deps.edn (src/main lib paths, cljs-bean 1.9.0 direct dep pending vendoring, :dev alias) + package.json (shadow-cljs 3.4.11, ag-grid-community ^36.0.1 devDeps) + shadow-cljs.edn (:dev-app browser build serving src/dev/public on :8080, :test node-test). Dev app at src/dev/ag_grid_cljs/dev/app.cljs; run with 'npm run dev', tests with 'npm test' (8 tests / 22 assertions green). Headless Chromium check: grid mounts from builder-produced EDN, 5 rows, keyword :field camelized into headers, :dom-layout :auto-height enum applied, CLJS value-formatter survived conversion with lazy kebab bean params (nested (-> p :col-def :field) works) and forward-converted return, (raw f) opt-out verified. Namespaces ag-grid-cljs.core / .impl.convert are provisional pending the namespace-layout decision. Awaiting HITL reaction before closing.

**2026-07-20T20:37:56.959936340Z**

# Resolution

Skeleton approved (HITL). Core design proven end to end: builder-produced EDN options -> conversion contract -> createGrid mount, verified by 8 node contract tests (22 assertions) plus a headless-Chromium mount check (5 rows, camelized keyword :field headers, :dom-layout :auto-height enum, CLJS value-formatter with lazy kebab bean params incl. nested (-> p :col-def :field), forward-converted return, (raw f) opt-out).

Decisions settled by this ticket:
- **Repo scaffold**: deps.edn (:paths ["src/main"], :dev alias adds src/dev + src/test and thheller/shadow-cljs) + package.json (shadow-cljs and ag-grid-community as devDeps of the dev harness; consumers own ag-grid-community as a peer) + shadow-cljs.edn (:deps {:aliases [:dev]}, :dev-app browser build served from src/dev/public on :8080, :test node-test). Commands: npm run dev / npm test.
- **Dev app location**: src/dev/ag_grid_cljs/dev/app.cljs + src/dev/public/index.html.
- **Builders are plain-map sugar**: (options) starts an EDN map; with-* are assoc; full options surface stays reachable via ordinary assoc. First cut: with-columns, with-default-col-def, plus with-row-data (added: the documented JS-by-contract entry point for rows).
- **cljs-bean rides as a direct dep in the skeleton**; vendoring the slice (per agd-01ky0eck96vn) is implementation work for the build-out effort, flagged in deps.edn.
- **Namespaces ag-grid-cljs.core / ag-grid-cljs.impl.convert are provisional** — naming is now a sharp question, graduated from the map's fog as its own ticket.

**2026-07-20T20:37:57.065694084Z**

Skeleton approved: EDN builders -> conversion contract -> createGrid proven (node contract tests + headless mount check). Scaffold settled: deps.edn/src/main library, src/dev dev app, shadow-cljs :dev-app+:test builds, npm run dev / npm test. Builders = plain-map sugar; cljs-bean direct dep pending vendoring; ag-grid-cljs.* namespaces provisional.

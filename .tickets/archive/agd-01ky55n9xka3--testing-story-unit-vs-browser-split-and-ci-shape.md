---
id: agd-01ky55n9xka3
title: 'Testing story: unit vs browser split and CI shape'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-22T15:03:44.819286204Z'
updated: '2026-07-22T17:36:17.784419760Z'
closed: '2026-07-22T17:36:17.784419760Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
assignee: jonas
---

## Description

## Question

Define the library's testing story. Data points from the skeleton: a node-test contract suite (14 tests, npm test, shadow-cljs :test build) plus per-ticket Playwright headless-Chromium checks whose scripts were kept OUT of the repo. Decide: what lives in the committed suite (conversion contract, builders, registry/validation) vs what needs a real browser (mount, renderers, Enterprise); whether browser tests are committed and if so with what harness; what CI runs (pure-Clojure per the key-registry decision — does headless Chromium fit?); and how the Enterprise license constraint (compile-time env macro, CI has no license) shapes the matrix.

## Notes

**2026-07-22T17:36:10.062773900Z**

RESOLUTION — Testing story (all four branches confirmed in grilling session, 2026-07-22):

1. THE SPLIT (rule, not file list): the committed node suite owns everything pure or fakeable — conversion contract, builders (plain-map sugar), registry/validation warnings, the update-grid! PATCH differ tested against a stubbed GridApi that records setGridOption calls, and renderer helpers' pure parts. The browser suite owns only assertions that depend on AG Grid's actual runtime behavior: mount, transactional updates preserving scroll/selection/focus, React cell renderers, Enterprise features, plus one "update-grid! actually applies" smoke. Dividing line: does the assertion depend on AG Grid's real runtime behavior, or only on our code's contract?

2. BROWSER SUITE: COMMITTED. Harness = shadow-cljs :browser-test build (assertions stay in cljs.test, inside the same runtime as the wrapper — direct CLJS API access, no window-hook plumbing) + a small committed Playwright driver script (~100-200 lines, cribbing re-frame's Sept-2025 day8.chrome-shadow-test-runner structure) that serves the compiled build, launches headless Chromium, reads results, and supplies real mouse primitives for the interaction tests (fill-handle drag, scrolling). Use AG Grid's first-party setupAgTestIds()/data-testid hooks for selectors. Playwright is the DRIVER, not the assertion language — only gestures are scripted from the Playwright side.
   Rejected: karma / shadow :karma target (karma deprecated upstream April 2023; reagent/fulcro legacy only); pure-JS Playwright specs (second assertion language, no in-runtime API access); etaoin (alive but clunkier WebDriver drag, black-box only); jsdom/node for anything touching the grid (AG Grid docs explicitly warn virtualization breaks without CSS layout); kaocha-cljs2 (dormant since 2023).
   Prior-art research (external agent, sources checked 2026-07-22): no existing CLJS grid wrapper has a suite to copy; the compile-:browser-test/serve/drive-headless/read-results pattern is established (re-frame 2025) but unpackaged — everyone writes ~100-200 lines of glue; AG Grid's own testing docs recommend real-browser e2e with Playwright and ship data-testid helpers for it. Key refs: github.com/day8/re-frame-test chrome_shadow_test_runner.clj; ag-grid.com/react-data-grid/testing; shadow-cljs User Guide :browser-test; karma-runner deprecation (2023).

3. CI SHAPE: one GitHub Actions workflow, one job, on every push/PR: checkout + JDK + Node setup, npm ci, then (a) npm test — node contract suite, fast fail-early; (b) npx playwright install --with-deps chromium && npm run test:browser — the smoke suite. Headless Chromium fits CI trivially via Playwright's managed install. No matrix for v1: single OS (ubuntu), single Node LTS, single pinned AG Grid version (the registry pin). Codegen stays out of CI per the key-registry decision (manual tool, committed output).

4. ENTERPRISE/LICENSE AXIS: Enterprise smoke tests live in the SAME committed browser suite and run UNLICENSED in CI. The :browser-test build reuses the compile-time AG_GRID_LICENSE macro; unset in CI means the real Enterprise feature paths are still exercised (skeleton agd-01ky0ed8fw7v proved identical behavior both states — only a console error + watermark). The Playwright driver allowlists the known license console errors and FAILS on any unexpected console error (regression tripwire). No licensed CI leg — a license secret can't safely reach forked-PR builds on a public repo; the licensed path is a documented local capability (AG_GRID_LICENSE=... npm run test:browser).

**2026-07-22T17:36:17.784419760Z**

Node suite owns pure/fakeable (conversion, builders, registry, differ vs stubbed GridApi); committed browser smoke suite = shadow-cljs :browser-test (cljs.test in-runtime) + ~150-line Playwright driver (re-frame 2025 pattern, real mouse drag, AG Grid data-testid hooks); CI = one GitHub Actions job on every push/PR (node tests, then Playwright-managed Chromium); Enterprise smoke runs unlicensed in CI with known license console errors allowlisted, no licensed CI leg — licensed path is a documented local AG_GRID_LICENSE run. Full resolution + prior-art research in the ticket's notes.

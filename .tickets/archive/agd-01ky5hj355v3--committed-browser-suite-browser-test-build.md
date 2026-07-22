---
id: agd-01ky5hj355v3
title: 'Committed browser suite: :browser-test build, Playwright driver, CI leg'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-22T18:31:42.495366113Z'
updated: '2026-07-22T21:31:17.429758759Z'
closed: '2026-07-22T21:31:17.429758759Z'
acceptance:
- title: npm run test:browser green locally in Playwright-managed headless Chromium
  done: true
- title: suite covers mount / state-preserving transactions / React renderer / unlicensed Enterprise / update-grid! smoke
  done: true
- title: unexpected console errors fail the run; known license errors allowlisted
  done: true
- title: 'CI job extended: node leg then browser leg, single workflow'
  done: true
deps:
- agd-01ky5hj2mbj5
- agd-01ky5hj32e7j
- agd-01ky5hj2wq7n
---

## Description

The committed browser suite per ADR 0015: a shadow-cljs :browser-test build (assertions stay in cljs.test inside the same runtime as the wrapper — direct CLJS API access) driven by a committed ~100-200-line Playwright script (cribbing the re-frame chrome-shadow-test-runner structure) that serves the compiled build, launches headless Chromium, reads results, and supplies real mouse gestures. Selectors via AG Grid first-party setupAgTestIds()/data-testid hooks. Suite covers: core mount, transactional updates preserving scroll/selection/focus, React cell renderers, the unlicensed Enterprise smoke (reusing the compile-time AG_GRID_LICENSE macro; known license console errors allowlisted), and one update-grid!-actually-applies smoke. Any unexpected console error fails the run (regression tripwire). Wire npm run test:browser, and extend the CI job with npx playwright install --with-deps chromium + the browser leg after the node leg. Document the licensed local run (AG_GRID_LICENSE=... npm run test:browser).

## Notes

**2026-07-22T21:27:45.434690171Z**

Implemented: :browser-test build (custom runner-ns stashing the cljs.test summary on window), src/browser-test suite (mount/React-renderer/update-grid! + state-preserving transaction with a real Playwright wheel gesture + unlicensed Enterprise mount), ~140-line Playwright driver (serves build, headless Chromium, gesture bridge, console-error tripwire with scoped AG-Grid-license allowlist), npm run test:browser, CI browser leg after node leg, README licensed-local-run doc. Selectors via first-party setupAgTestIds/agTestIdFor per ADR 0015 §2. Green: 5 tests/16 assertions, 0 failures; tripwire verified to fail on injected console.error; node suite still 44/124 green.

**2026-07-22T21:31:17.429758759Z**

Committed browser suite per ADR 0015 (commit 8a17056). :browser-test build with a custom runner-ns that runs ag-grid-cljs.browser.*-test and stashes the cljs.test summary on window; node :test scoped to src/test via :test-paths so the browser suite never runs headless in node. Suite: core mount, React cell renderer, update-grid! smoke, a fine-grained transaction preserving scroll/selection/focus (scroll = a real Playwright wheel gesture via a window gesture bridge), and the unlicensed Enterprise mount; selectors via first-party setupAgTestIds/agTestIdFor data-testid hooks. ~140-line Playwright driver (test/browser/run.mjs) serves the build, drives headless Chromium, services gestures, and fails on any console error outside a scoped AG-Grid-license allowlist. npm run test:browser green (5 tests/16 assertions, 0 failures); tripwire verified to exit 1 on an injected console.error. CI extended with playwright install + browser leg after the node leg; README documents the licensed local run.

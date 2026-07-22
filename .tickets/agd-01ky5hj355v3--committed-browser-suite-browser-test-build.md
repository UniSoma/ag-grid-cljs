---
id: agd-01ky5hj355v3
title: 'Committed browser suite: :browser-test build, Playwright driver, CI leg'
status: open
type: task
priority: 2
mode: afk
created: '2026-07-22T18:31:42.495366113Z'
updated: '2026-07-22T18:31:42.495366113Z'
acceptance:
- title: npm run test:browser green locally in Playwright-managed headless Chromium
  done: false
- title: suite covers mount / state-preserving transactions / React renderer / unlicensed Enterprise / update-grid! smoke
  done: false
- title: unexpected console errors fail the run; known license errors allowlisted
  done: false
- title: 'CI job extended: node leg then browser leg, single workflow'
  done: false
deps:
- agd-01ky5hj2mbj5
- agd-01ky5hj32e7j
- agd-01ky5hj2wq7n
---

## Description

The committed browser suite per ADR 0015: a shadow-cljs :browser-test build (assertions stay in cljs.test inside the same runtime as the wrapper — direct CLJS API access) driven by a committed ~100-200-line Playwright script (cribbing the re-frame chrome-shadow-test-runner structure) that serves the compiled build, launches headless Chromium, reads results, and supplies real mouse gestures. Selectors via AG Grid first-party setupAgTestIds()/data-testid hooks. Suite covers: core mount, transactional updates preserving scroll/selection/focus, React cell renderers, the unlicensed Enterprise smoke (reusing the compile-time AG_GRID_LICENSE macro; known license console errors allowlisted), and one update-grid!-actually-applies smoke. Any unexpected console error fails the run (regression tripwire). Wire npm run test:browser, and extend the CI job with npx playwright install --with-deps chromium + the browser leg after the node leg. Document the licensed local run (AG_GRID_LICENSE=... npm run test:browser).

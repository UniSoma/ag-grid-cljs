# 0015. Testing: node/browser split rule, committed browser suite, single CI job

- Status: accepted, 2026-07-22
- Origin: knot ticket agd-01ky55n9xka3 (tickets are ephemeral; this record is self-contained)

The node suite owns everything pure or fakeable; the browser suite owns only what depends on AG Grid's real runtime behavior, and it IS committed — a shadow-cljs `:browser-test` build (assertions in cljs.test, in-runtime API access) driven by a ~150-line Playwright script. CI is one GitHub Actions job on every push/PR: node tests, then Playwright-managed headless Chromium; the Enterprise smoke runs unlicensed with known license console errors allowlisted.

## Context

The walking skeleton left a node-test contract suite (14 tests, `npm test`, shadow-cljs `:test` build) plus per-ticket Playwright headless-Chromium checks whose scripts were kept OUT of the repo. The library needed a permanent testing story: what lives in the committed suite vs what needs a real browser; whether browser tests are committed and with what harness; what CI runs (and whether headless Chromium fits, given codegen stays pure-Clojure and manual per ADR 0007); and how the Enterprise license constraint (compile-time env macro, CI has no license) shapes the matrix.

## Decision

1. **The split is a rule, not a file list.** The committed node suite owns everything pure or fakeable — the conversion contract (ADR 0005), builders (plain-map sugar, ADR 0009), registry/validation warnings (ADR 0007), the `update-grid!` PATCH differ tested against a stubbed GridApi that records `setGridOption` calls, and renderer helpers' pure parts. The browser suite owns only assertions that depend on AG Grid's actual runtime behavior: mount, transactional updates preserving scroll/selection/focus, React cell renderers, Enterprise features, plus one "update-grid! actually applies" smoke. Dividing line: does the assertion depend on AG Grid's real runtime behavior, or only on our code's contract?

2. **The browser suite is committed.** Harness = a shadow-cljs `:browser-test` build (assertions stay in cljs.test, inside the same runtime as the wrapper — direct CLJS API access, no window-hook plumbing) + a small committed Playwright driver script (~100–200 lines, cribbing re-frame's Sept-2025 `day8.chrome-shadow-test-runner` structure) that serves the compiled build, launches headless Chromium, reads results, and supplies real mouse primitives for the interaction tests (fill-handle drag, scrolling). Selectors use AG Grid's first-party `setupAgTestIds()` / `data-testid` hooks. Playwright is the DRIVER, not the assertion language — only gestures are scripted from the Playwright side.

3. **CI shape: one GitHub Actions workflow, one job, on every push/PR.** Checkout + JDK + Node setup, `npm ci`, then (a) `npm test` — the node contract suite, fast fail-early; (b) `npx playwright install --with-deps chromium && npm run test:browser` — the smoke suite. Headless Chromium fits CI trivially via Playwright's managed install. No matrix for v1: single OS (ubuntu), single Node LTS, single pinned AG Grid version (the registry pin). Codegen stays out of CI per ADR 0007 (manual tool, committed output).

4. **Enterprise/license axis: the Enterprise smoke tests live in the same committed browser suite and run UNLICENSED in CI.** The `:browser-test` build reuses the compile-time `AG_GRID_LICENSE` env macro; unset in CI means the real Enterprise feature paths are still exercised (the skeleton proved behavior is identical in both states — only a console error + watermark differ). The Playwright driver allowlists the known license console errors and FAILS on any unexpected console error (a regression tripwire). No licensed CI leg — a license secret can't safely reach forked-PR builds on a public repo; the licensed path is a documented local capability (`AG_GRID_LICENSE=... npm run test:browser`).

## Consequences

- The unexpected-console-error failure rule doubles as a general regression tripwire for the whole browser suite, not just the license path.
- Prior-art note (external research, sources checked 2026-07-22): no existing CLJS grid wrapper has a suite to copy; the compile-`:browser-test`/serve/drive-headless/read-results pattern is established (re-frame, 2025) but unpackaged — everyone writes ~100–200 lines of glue, and so do we. AG Grid's own testing docs recommend real-browser e2e with Playwright and ship the `data-testid` helpers for it. Key refs: github.com/day8/re-frame-test `chrome_shadow_test_runner.clj`; ag-grid.com/react-data-grid/testing; shadow-cljs User Guide `:browser-test`; karma-runner deprecation (2023).

## Considered options

- **karma / shadow-cljs `:karma` target** — rejected: karma was deprecated upstream April 2023; reagent/fulcro usage is legacy only.
- **Pure-JS Playwright specs** — rejected: a second assertion language, and no in-runtime access to the wrapper's CLJS API.
- **etaoin** — rejected: alive but clunkier WebDriver drag, and black-box only.
- **jsdom/node for anything touching the grid** — rejected: AG Grid's docs explicitly warn that row virtualization breaks without real CSS layout.
- **kaocha-cljs2** — rejected: dormant since 2023.
- **A licensed CI leg** — rejected: a license secret can't safely reach forked-PR builds on a public repo; the licensed run is documented as a local capability instead.
- **A CI matrix (OS/Node/AG-Grid versions)** — rejected for v1: single ubuntu + single Node LTS + the single registry-pinned AG Grid version.

## References

- ADR 0005 — conversion boundary (the node suite's conversion contract)
- ADR 0007 — generated key registry (registry/validation tests; codegen out of CI)
- ADR 0009 — builder catalog v1 (builder tests in the node suite)

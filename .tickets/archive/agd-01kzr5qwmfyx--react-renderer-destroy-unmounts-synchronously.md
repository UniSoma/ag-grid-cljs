---
id: agd-01kzr5qwmfyx
title: react-renderer :destroy unmounts synchronously — React DEV error per live cell when destroy! runs inside a React commit
status: closed
type: bug
priority: 2
mode: afk
created: '2026-08-11T10:26:25.551126815Z'
updated: '2026-08-11T10:30:36.844450412Z'
closed: '2026-08-11T10:30:36.844450412Z'
acceptance:
- title: destroy-from-commit browser test passes with zero console.error
  done: true
- title: node + browser suites green
  done: true
---

## Description

Client report, verified against react-dom 19.2.7: react.cljs :destroy calls root.unmount() on the caller's stack. React 19's unmount warns (console.error, DEV only) when executionContext has RenderContext|CommitContext — and a React-hosted consumer typically destroys grids from a useEffect cleanup, which runs inside flushPassiveEffects (CommitContext). Result: one DEV error per live react-renderer cell. React defers the actual deletion in that case anyway, so the sync call buys nothing.

Agreed fix (grilling session): defer via js/queueMicrotask with a when-let guard on the captured root; applies unconditionally (churn-time destroys too — microtasks are free at 100-300/s). Accepted contract change: cell effect cleanups run a microtask after destroy for ALL callers, so cleanups must not touch params .api — documented in the namespace docstring. Verification: browser test that hosts a grid inside a React component (useEffect setup creates, cleanup destroys), captures console.error, asserts silence; same test doubles as a probe for whether :init's flushSync warns from a commit (separate ticket only if red). ADR 0011 gets a dated correction note ('explicit root.unmount() in destroy' — the root is still explicitly unmounted, only the tick moves; #26281 leak story unaffected, double-unmount is a no-op).

## Notes

**2026-08-11T10:30:20.892859555Z**

Probe result: :init's flushSync does NOT warn when the grid is created from a useEffect (browser test with probe as a temp assertion: 53 pass / 0 fail) — no follow-up ticket needed. Revert-check: with sync unmount restored, the new destroy-from-commit test fails (1 fail), so it catches the regression. Driver note: run.mjs already has an unexpected-console-error tripwire, but the test patches console.error before Playwright sees it, so the in-test capture is the real guard.

**2026-08-11T10:30:36.844450412Z**

react-renderer :destroy now defers root.unmount one microtask (queueMicrotask, when-let guard) so destroy! from a React commit (useEffect cleanup) prints no per-cell DEV error; contract change documented in the ns docstring (cell effect cleanups run after destroy — don't touch the grid api), ADR 0011 carries a dated correction, and a new react-host browser test (grid created/destroyed by a React component's useEffect) asserts console.error silence and proved red under the old sync unmount. Probe: :init's flushSync does not warn from a commit — no follow-up. Node 114/361 and browser 17/52 green.

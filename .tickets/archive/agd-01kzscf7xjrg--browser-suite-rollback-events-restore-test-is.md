---
id: agd-01kzscf7xjrg
title: 'Browser suite: rollback-events restore test is flaky — intermittently sees 0 cellValueChanged events'
status: closed
type: bug
priority: 2
mode: afk
created: '2026-08-11T21:43:16.658118257Z'
updated: '2026-08-11T21:50:35.153092148Z'
closed: '2026-08-11T21:50:35.153092148Z'
tags:
- browser-test
- flaky
acceptance:
- title: The deftest passes on 20 consecutive browser-suite runs
  done: true
- title: The fix does not weaken the assertions — still 3 events, sources, values, old-values, field, RowNode identity, and (:data node) nil
  done: true
- title: an-unsourced-write-does-not-look-like-the-sentinel uses the same wait strategy
  done: true
---

## Description

src/browser-test/ag_grid_cljs/browser/rollback_events_test.cljs, deftest a-restore-across-n-nodes-fires-n-events-carrying-the-sentinel.

Intermittently @seen is empty when the assertions run, failing all 7 assertions in the deftest (count 3 vs 0, sources #{} vs #{"restore"}, etc.). Frequency varies with machine load: observed 0/3 runs failing and 2/3 runs failing on the same checkout.

NOT caused by the AG Grid 36.0.2 -> 36.1.0 bump. Reproduced on 36.0.2 by reinstalling the old pin (npm install --no-save ag-grid-community@36.0.2 ag-grid-enterprise@36.0.2) and recompiling the browser-test build: same deftest, same 7 assertions, on the old version. The bump surfaced it, it did not introduce it.

The second deftest in the same ns (an-unsourced-write-does-not-look-like-the-sentinel) has not been observed failing, but it uses the same mount -> poll-testid -> next-frame -> setDataValue shape, so it is likely exposed to the same race with one write instead of three.

Reproduce: npx shadow-cljs compile browser-test && node test/browser/run.mjs  (run several times; slowing the driver down makes it more likely — forwarding browser console messages to stdout in the Playwright driver reproduced it readily).

Note the driver does not forward cljs.test output to stdout, so a failing run only prints the summary counts. Getting the failing assertion detail currently requires patching test/browser/run.mjs to log console messages.

## Design

Suspected cause: the test waits a single u/next-frame after u/poll-testid before calling .setDataValue on the three RowNodes. poll-testid only proves the first cell is in the DOM; one frame later the grid may not yet be in a state where setDataValue dispatches cellValueChanged, so the events either never fire or fire after the assertions have already run.

Directions worth checking:
- wait on the grid rather than on a frame count — e.g. resolve off firstDataRendered / a rowDataUpdated-style event before the restores, instead of u/next-frame;
- or poll for the expected event count with a deadline instead of asserting after a fixed delay;
- whatever the fix, apply the same treatment to an-unsourced-write-does-not-look-like-the-sentinel, which shares the shape.

Separately worth considering (own ticket if it grows): test/browser/run.mjs swallows cljs.test output, which is what made this slow to diagnose.

## Notes

**2026-08-11T21:50:35.153092148Z**

Root cause: AG Grid dispatches cellValueChanged asynchronously, in one batch — instrumentation showed @seen is still 0 immediately after all three .setDataValue calls on EVERY run, passing ones included. The single u/next-frame was a coin flip on whether that batch flush landed first, which is why the failure was all-or-nothing (0 events, never 1 or 2) rather than a partial count.

Fix: new u/poll-until in browser/util.cljs — polls a predicate every 25ms to a 2s deadline, then resolves anyway so a genuine shortfall fails as an assertion instead of hanging the suite. Both deftests now wait on the event count (>=3 and >=1) instead of a frame. Assertions unchanged: still = 3, sources, values, old-values, field, RowNode identity, (:data node) nil.

Verified: 3/10 failures before, 20/20 clean after.

Also fixed the diagnosis blocker noted in the ticket: test/browser/run.mjs now forwards page console messages to stdout, so a failing run prints cljs.test's per-assertion detail instead of only the summary counts. Kept as a one-liner, so no separate ticket.

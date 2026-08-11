---
id: agd-01kzq9x25fez
title: run-live-check! can fire against a destroyed grid and print AG Grid's destroyed-api warning
status: closed
type: bug
priority: 2
mode: afk
created: '2026-08-11T02:19:54.927009772Z'
updated: '2026-08-11T02:22:43.791179008Z'
closed: '2026-08-11T02:22:43.791179008Z'
---

## Description

modelUpdated/newColumnsLoaded are async-dispatched (not in ALWAYS_SYNC_GLOBAL_EVENTS); flushAsyncQueue copies the queue, so removing a listener does not dequeue an already-queued closure. If an event is dispatched in the same macrotask as destroy! (e.g. an async datasource successCallback landing just before teardown), the queued run-live-check! closure runs after destroy, calls .getColumns on the dead api, and AG Grid prints the ungated _warn(26) 'cannot be called as the grid has been destroyed' — the exact class of console warning the check exists to prevent. Client-reported. Fix: guard run-live-check! with isDestroyed (in defaultFns, safe after destroy), covering both the field check and the ref-data check. Node test for the short-circuit; browser test pinning the window (dispatch + destroy same macrotask, no destroyed-warn). Docstring carries the why; one sentence appended to ADR 0017 consequences.

## Notes

**2026-08-11T02:22:43.791179008Z**

run-live-check! now opens with an isDestroyed guard — the one api call left alive after destroy — closing the async-dispatch window where a modelUpdated/newColumnsLoaded queued in the same macrotask as destroy! ran the check against the dead api and printed AG Grid's ungated destroyed-grid warning. One guard at the shared entry covers the field check and the ref-data check. Node test proves the short-circuit (fake api gained isDestroyed); browser test reproduces the exact race (transact! + destroy! in one macrotask) and fails with the guard reverted. ADR 0017 consequences note the window. Commit 538e1d4.

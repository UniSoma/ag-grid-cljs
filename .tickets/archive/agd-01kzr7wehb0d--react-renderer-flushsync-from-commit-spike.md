---
id: agd-01kzr7wehb0d
title: 'react-renderer flushSync-from-commit: spike batched microtask flush vs portal host, then ADR'
status: closed
type: task
priority: 2
mode: afk
created: '2026-08-11T11:03:52.107556432Z'
updated: '2026-08-11T11:20:19.665085199Z'
closed: '2026-08-11T11:20:19.665085199Z'
tags:
- react
- renderer
- adr
acceptance:
- title: 'Browser harness drives create-grid! + refresh from inside a real useEffect and counts React DEV console.errors on the status quo (expected: ~1 per live renderer cell per create and per refresh)'
  done: true
- title: 'Variant (b) prototyped: per-cell roots kept, renders queued per macrotask, one flushSync issued from queueMicrotask; harness reports zero DEV errors and zero visible empty-cell paint'
  done: true
- title: 'Measurement cases exercised under (b): autoHeight rows and column auto-sizing; a failure there is recorded as a caveat/escape-hatch decision, not as grounds to build (c)'
  done: true
- title: Portal-host prototype (c) is built ONLY if (b) fails the error or flash criteria
  done: true
- title: 'New ADR (next number) ''React renderer render scheduling: batched microtask flush vs portal host'' written from the spike''s numbers, three-way (status quo / b / c); correction note appended to ADR 0011'
  done: true
- title: 'If (b) ships: contract line documented in ag-grid-cljs.react ns docstring (cell content lands before paint, not on the call stack) and browser tests gain an await-microtask helper'
  done: true
---

## Description

Origin: standalone submission from the main client asking to reopen ADR 0011's deferred portal variant (one host root + createPortal per cell, ag-grid-react's architecture). Submission cites sha 6548da8a, ag-grid-community 35.3.1, react-dom 19.2.7; written after the :destroy microtask deferral (agd-01kzr5qwmfyx) and the run-live-check! isDestroyed guard (agd-01kzq9x25fez), assuming both.

Its central claim (point 1): a React-hosted consumer drives the grid from useEffect bodies; React runs passive effects with CommitContext set, so every per-cell flushSync in :init/:refresh (react.cljs) prints React's DEV "flushSync was called from inside a lifecycle method" error — ~1 per live renderer cell per create and per data push. Claimed unfixable per-cell because deferring flushSync re-opens the empty-cell flash.

Grilling outcome (2026-08-11) — scope agreed with the maintainer:

1. REPRO GATE FIRST. The claim is argued from react-dom source citations, not a repro. Build a browser harness (real useEffect-driven create/refresh) and count errors before any ADR text.

2. THREE-WAY COMPARISON, not the submission's two-way. Option (b), unconsidered by the submission: keep per-cell roots, queue cell renders per macrotask, issue ONE flushSync from queueMicrotask. Microtasks queued during a commit run after CommitContext clears but before paint — so (b) plausibly kills the whole error class and collapses N flushSync calls to one, with no host, no registry, no ADR 0021 interplay, no grid-destroy hook. Note: the submission's claim that a portal-host batch flush is "schedulable outside any consumer commit" is wrong — create-grid! from useEffect puts AG Grid's init stack inside the commit; (b) and (c) escape the commit the same way (microtask) or neither does.

3. KILL RULE (staged spike): (b) wins and (c) is closed if the harness shows zero DEV errors and zero visible empty-cell paint with (b) applied. (c) is built only if (b) fails one of those. If (b) wins, (c)'s residual advantages (one commit per batch instead of N; per-cell #26281 audit shrinks to one host root) are acknowledged in the ADR as real but not worth the architecture.

4. MEASUREMENT CASES in scope but not deciders: autoHeight rows and column auto-sizing may read an empty cell before the microtask fills it. A portal host has the same deferred first paint, so a failure here cannot decide (b) vs (c) — it only shapes a caveat (documented limitation, or sync-flush opt-in for those grids).

5. CONTEXT TRANSPARENCY (submission point 4) is OUT of the motivation: portal children inherit the context of the host tree, but the proposed host root is library-created — cells would see an empty library-owned tree, exactly as opaque as today. Delivering context flow requires new consumer-facing API (host wrapper), contradicting the submission's zero-API-change claim. May become its own future ADR that a portal host merely enables.

6. UNMOUNT ACCOUNTING corrected: portals shrink the #26281 discipline from N cells to 1 host root; they do not delete it. The host is still a nested createRoot needing explicit unmount on grid destroy (e.g. gridPreDestroyed listener), wired from the react ns since core never requires it.

7. (b)'s CONTRACT CHANGE accepted in principle: :init returns a span that fills at end-of-microtask; :refresh returns true before the re-render lands. Content lands before paint, not on the call stack — same class of change as the destroy deferral. Sibling docstring paragraph + await-microtask test helper.

8. The shipped teardown fixes (deferred unmount, isDestroyed guard) stand regardless of outcome.

9. Fulcro note: ADR 0011's original deferral criterion (transact!-from-a-cell ergonomics) was met, not failed (ADR 0012: "the Fulcro skeleton required zero adapter"). The reopening rests solely on the new evidence; the ADR must not frame it as the deferral condition triggering. No consumer is hurting today — this is anticipatory; normal priority.

References: ADR 0011 (tiers + both correction notes), ADR 0012 (no adapters), ADR 0021 §4 (deferred construction; react ns isolation is load-bearing), src/main/ag_grid_cljs/react.cljs, React issue #26281.

## Notes

**2026-08-11T11:20:19.560465218Z**

Spike numbers (8 renderer cells, React 19.2.7 DEV, headless Chromium): flushSync-in-commit DEV errors by leg driven from useEffect — create: status quo 0 / batched 0; refreshCells force: status quo 8 (1 per cell) / batched 0; set-rows!: status quo 0 / batched 0. The error class is real but NARROWER than submitted: AG Grid builds rows and applies row data async, so only synchronous per-cell refresh APIs (refreshCells) repro it. Empty-cell frames identical under both schedulings ({create 1, set-rows 1} — AG Grid's own async cell build). autoHeight settles to content height under batched flush (62px for 60px content). Caveat: refreshCells + autoSizeAllColumns on the same stack measures stale content under batched flush (88px vs 326px; correct one microtask later; status quo measured fresh). Kill rule fired for (b): portal host (c) NOT built, closed in ADR 0023 with residual advantages acknowledged.

**2026-08-11T11:20:19.665085199Z**

react-renderer now batches all cell renders into ONE flushSync per microtask (module-level queue, destroyed-cell guard): zero React DEV flushSync-from-commit errors on create/refreshCells/set-rows! driven from useEffect (was 1 per live cell on refreshCells), N render passes collapsed to 1, empty-cell paint unchanged vs baseline. Contract change: content lands before paint, not on the call stack (ns docstring + u/await-microtask helper + react_commit_flush_test contract guard). Portal host closed, not built — kill rule fired for the batched flush; ADR 0023 records the three-way decision from the spike's numbers, correction note appended to ADR 0011. Caveat recorded: same-stack refreshCells+auto-size measures stale content; escape is measuring one microtask later.

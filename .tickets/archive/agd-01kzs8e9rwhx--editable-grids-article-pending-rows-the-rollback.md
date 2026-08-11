---
id: agd-01kzs8e9rwhx
title: 'Editable-grids article: pending rows, the rollback loop, and server round-trip write-back'
status: closed
type: task
priority: 2
mode: afk
created: '2026-08-11T20:32:51.484692193Z'
updated: '2026-08-11T21:32:55.320413195Z'
closed: '2026-08-11T21:32:55.320413195Z'
tags:
- docs
- recipes
acceptance:
- title: docs/editable-grids.md exists with all three sections
  done: true
- title: The rollback section covers sentinel, guard, and the N-event batch case
  done: true
- title: The write-back section shows the ghost-key failure under both row recipes
  done: true
- title: Wired into the docs index and cljdoc article list
  done: true
---

## Description

ADR 0009 names pending-rows as a recipe and (2026-08-11 amendment) sends the rollback loop and the server round-trip write-back the same way, after rejecting set-data-value! and set-row-data! as functions. Nothing is written yet.

One article, not three pages — a reader building an editable grid hits all three in sequence.

Sections:

1. PENDING ROWS — the recipe ADR 0009 already named: optimistic pinned-top rows keyed by a temp row id, edit routing update-vs-persist.

2. THE ROLLBACK LOOP — three parts, not two:
   a. the sentinel write on the failure arm: (.setDataValue node col old-value "restore")
   b. the handler-side guard: (when-not (= "restore" source) ...) — without it, restore fires cellValueChanged, which writes, which fails, which restores
   c. THE BATCH CASE: an all-or-nothing server rollback restores N cells across N nodes and fires N cellValueChanged events, each hitting the same guard. Without the sentinel that is not a cycle but N spurious writes — a different failure mode from the single-cell one, and the reason a reader may think the guard is optional for batches.
   Note in passing that (:node params) is a raw RowNode in an ordinary handler (see agd-01kzs8dexkb1) — no unwrap needed, and (:data node) does NOT work.

3. SERVER ROUND-TRIP WRITE-BACK — the .setData replace trap: .setData REPLACES, so writing one key drops the rest of the row. The merge is (Object.assign #js {} (.-data node) new-data) then .setData. And the load-bearing rule, the reason no library function ships: A WRITE INTO A ROW MUST USE THE CONVERTER THE ROW WAS BUILT WITH. A camelizing merge against literal-kebab rows lands rowId beside the live "row-id"; the row-id getter reads the ghost and rows silently lose identity after an edit. Show it under both row recipes.

## Design

The rollback section is written by the proposal requester, who has the working loop and found the batch failure mode. Anyone else would be transcribing.

Placement: docs/editable-grids.md, wired into the docs index and the cljdoc article list (ADR 0014).

Cross-references it must carry: ADR 0009 node-operations section (why these are prose and not functions), ADR 0003 (JS-by-contract rows), CONTEXT.md row recipe (the pairing outlives the datasource), docs/options-and-conversion.md (the two recipes).

## Notes

**2026-08-11T20:45:52.251430165Z**

Reassigned (2026-08-11): WE draft all three sections, including the rollback loop. The earlier note assigning section 2 to the proposal requester is withdrawn.

Reason: nothing in section 2 is unknown to us. The requester stated all three parts in their reply — the sentinel write on the failure arm, the handler-side guard, and the batch case (an all-or-nothing rollback restores N cells and fires N cellValueChanged events, so without the sentinel it is N spurious writes rather than a cycle). A recipe should be minimal anyway, not lifted from one app.

The requester REVIEWS section 2 against their working loop. That is minutes of their time instead of an article, and it is the only part they can give us that we cannot produce.

Added scope: verify the batch claim ourselves rather than citing it. It is a claim about AG Grid (how many cellValueChanged events a multi-cell restore fires), not a claim about us, and ADR 0019 §5/Consequences set the precedent — one browser assertion covering the premise a doc rests on. Put it in the browser suite (ADR 0015). If the count differs from the report, the doc follows the test.

**2026-08-11T21:31:45.365261682Z**

Drafted docs/editable-grids.md with all three sections. Pending-rows MOVED out of docs/updating-data.md (not duplicated); the range-fill batch-flush recipe stays there and the two cross-link. ADR 0014 amended for the seventh article. options-and-conversion.md § row recipes now points at the write-back rule.

Batch claim verified rather than cited, per the added scope: new browser test ag-grid-cljs.browser.rollback-events-test pins three premises against a real grid — three .setDataValue restores across three nodes fire exactly 3 cellValueChanged events (not 1), the fourth argument arrives as :source, and (:node params) is the raw RowNode ((.-data node) is the row, (:data node) nil). Report matches: the doc did not have to follow a different count. Node 114/363 green, browser 21 tests/69 assertions green, clj-kondo at baseline.

Still open: the requester reviews the rollback section against their working loop.

**2026-08-11T21:32:55.320413195Z**

docs/editable-grids.md ships all three sections. Pending-rows MOVED out of docs/updating-data.md (batch-flush stays there; the two cross-link), rollback loop written with both arms plus the batch case as a distinct failure mode (no cycle, N spurious writes), write-back with the .setData replace trap and the ghost-key failure shown in BOTH directions — a bare clj->js merge landing "row-id" beside camel rows, a camelizing merge landing "rowId" beside kebab rows. Preamble states the two bean facts the article rests on: (:node params) is the raw RowNode ((.-data node), not (:data node)) and (:field (:col-def params)) is the emitted string the row is keyed at.

Added scope met — the batch claim is VERIFIED, not cited: new browser test ag-grid-cljs.browser.rollback-events-test (2 tests, 9 assertions) pins three .setDataValue restores across three nodes at exactly 3 cellValueChanged events, the fourth argument arriving as :source, and the raw-RowNode reads. Count matched the report, so the doc stood.

Wired: doc/cljdoc.edn gains 'Editable grids'; options-and-conversion.md § row recipes now says the pairing outlives the datasource and links the converter rule; ADR 0014 amended for the seventh article and the pending-rows move.

Node 114/363 green, browser 21/69 green, clj-kondo at baseline. Open: the requester still reviews the rollback section against their working loop.

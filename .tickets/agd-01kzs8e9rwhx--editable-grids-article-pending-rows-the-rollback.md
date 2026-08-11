---
id: agd-01kzs8e9rwhx
title: 'Editable-grids article: pending rows, the rollback loop, and server round-trip write-back'
status: open
type: task
priority: 2
mode: hitl
created: '2026-08-11T20:32:51.484692193Z'
updated: '2026-08-11T20:32:51.484692193Z'
tags:
- docs
- recipes
acceptance:
- title: docs/editable-grids.md exists with all three sections
  done: false
- title: The rollback section covers sentinel, guard, and the N-event batch case
  done: false
- title: The write-back section shows the ghost-key failure under both row recipes
  done: false
- title: Wired into the docs index and cljdoc article list
  done: false
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

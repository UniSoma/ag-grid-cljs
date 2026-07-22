---
id: agd-01ky5hj32e7j
title: update-grid! PATCH differ
status: closed
type: feature
priority: 2
mode: afk
created: '2026-07-22T18:31:42.410482006Z'
updated: '2026-07-22T19:50:08.631777686Z'
closed: '2026-07-22T19:50:08.631777686Z'
acceptance:
- title: changed updatable keys produce exactly one setGridOption each; unchanged and absent keys produce none
  done: false
- title: initial-only change warns once per key and is ignored; :row-data warns and is ignored; unclassified applies and warns
  done: false
- title: :column-defs passes whole-value; stash reflects applied state across successive updates
  done: false
- title: node suite green against the stubbed GridApi; docstring contract in place
  done: false
deps:
- agd-01ky5hj2mbj5
- agd-01ky5hj2t2m9
---

## Description

Implement update-grid! [handle new-opts] per ADR 0008: iterate keys PRESENT in new-opts, compare by = uniformly (no function special-case), call setGridOption (key camelized, value through the conversion boundary) only where changed; absent keys emit nothing; :column-defs ships the whole new value (AG Grid owns column-level diffing). Registry :initial? is the sole updatable-vs-initial-only classifier: initial-only changes dev-warn once per key (dedup set on the handle) and are ignored; :row-data is ignored with a dev-warn (the data channel owns it); unclassified changed keys apply optimistically with a dev-warn. The stash merges present new keys after an update so successive diffs stay accurate and minimal; update-grid! returns the handle with the updated stash. Node-tested against a stubbed GridApi that records setGridOption calls (ADR 0015). Docstring contract per ADR 0014. Demo the channel in the dev app.

## Notes

**2026-07-22T19:50:08.631777686Z**

update-grid! [handle new-opts] PATCH differ per ADR 0008. Iterates keys present in new-opts, compares by = uniformly; changed updatable keys emit one setGridOption each (value routed through map->js so update converts identically to create-grid!), absent/unchanged keys emit none. Registry :initial? is the sole classifier: initial-only changes dev-warn once per key (dedup atom on the handle) and are ignored; :row-data warns and is ignored (data channel owns it); unclassified changed keys apply optimistically with a warn. :column-defs ships whole. Classification is goog.DEBUG-gated so the registry stays DCE'd in prod (every key unclassified there -> applied optimistically). GridHandle gained an internal warned atom. Stash merges present new keys; returns handle with updated stash. Node suite: 38 tests/103 assertions green against the stubbed GridApi. Dev app demos the channel via a quick-filter box. Commit 13d0d30.

---
id: agd-01ky5hj32e7j
title: update-grid! PATCH differ
status: open
type: feature
priority: 2
mode: afk
created: '2026-07-22T18:31:42.410482006Z'
updated: '2026-07-22T18:31:42.410482006Z'
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

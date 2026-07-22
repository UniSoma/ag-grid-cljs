---
id: agd-01ky5hj2mbj5
title: 'GridHandle: create-grid! rename, grid-api accessor, retargeted channels'
status: open
type: feature
priority: 2
mode: afk
created: '2026-07-22T18:31:41.963462822Z'
updated: '2026-07-22T18:31:41.963462822Z'
acceptance:
- title: create-grid! returns a GridHandle stashing the last-applied EDN opts
  done: false
- title: grid-api returns the raw GridApi from a handle
  done: false
- title: set-rows!/transact!/destroy! take the handle; node tests updated and green
  done: false
- title: all three dev apps compile and work against the new API
  done: false
- title: docstring contracts on all touched public vars
  done: false
---

## Description

Rename create-grid to create-grid! (ADR 0006) and make it return a GridHandle {:api <GridApi> :opts <last-applied-EDN>} instead of the raw GridApi (ADR 0008). Add the grid-api accessor exposing the raw GridApi (the escape-hatch channel, ADR 0004). Retarget set-rows!, transact! and destroy! to take the handle. Update the three dev harness apps (dev, fulcro, enterprise) to the new entry point. Every touched public var gets its docstring contract per ADR 0014 (what it does, EDN shape, minimal example, constraints).

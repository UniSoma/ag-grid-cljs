---
id: agd-01ky0edx8dzc
title: 'Options-diffing semantics: what is updatable and how changes apply'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:47.500211676Z'
updated: '2026-07-21T02:36:45.137279860Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
deps:
- agd-01ky0ed8766f
---

## Description

## Question

Define the declarative update contract: which grid options the differ applies via setGridOption vs which are initial-only (and what happens when an initial-only option changes — warn? recreate grid?); diff granularity for nested structures (columnDefs identity); and how the explicit data channel and the differ stay out of each other's way (row-data key ignored by differ?). Informed by the Fulcro skeleton's update behavior.

## Notes

**2026-07-21T02:24:04.180508079Z**

Inputs from the Fulcro skeleton (agd-01ky0ed8766f, closed): (1) set-rows! full swap with :get-row-id is state-preserving (scroll/selection/focus verified headless) — AG Grid already does row-data diffing internally, so the wrapper's diffing question is mainly about non-data OPTIONS; (2) dual bookkeeping in consumers is eliminable via set-rows!-from-source-of-truth; a generic auto-diff watcher (normalized-table watch -> transactions) was judged Fulcro-flavored and deferred HERE as a possible optional adapter; (3) the guide's D3 pattern shows where a props-driven declarative layer would hook in (sCU as props-changed callback) — we bypassed it with the explicit channel.

**2026-07-21T02:36:45.137279860Z**

Side research relevant to this ticket: docs/research/ag-grid-react-wrapper.md §1.2 (prop→grid bridge). Sourced findings on what ag-grid-react's re-apply-every-prop bridge breaks: new rowData/columnDefs references per render trigger extra grid renders and can reset grid state (selection, column order/width) — with the nuance that column state *may* survive when colId/field still match (docs hedge with 'may', not a guaranteed full reset). Also §4: hold gridOptions/callbacks stable across host re-renders or the grid resets internal state; stale-closure hazard on callbacks. Direct evidence for deciding what is updatable via setGridOption diffing vs what must go through the transactional channel.

---
id: agd-01ky0edx8dzc
title: 'Options-diffing semantics: what is updatable and how changes apply'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:47.500211676Z'
updated: '2026-07-21T02:24:04.180508079Z'
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

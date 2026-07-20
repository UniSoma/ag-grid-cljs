---
id: agd-01ky0m0btmrp
title: Namespace layout and public API naming
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-20T20:38:15.124044420Z'
updated: '2026-07-20T20:38:15.124044420Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
---

## Description

## Question

Lock the library's namespace layout and public-API naming conventions. The walking skeleton (agd-01ky0ed83xww) established provisional names to react against: ag-grid-cljs.core (raw, register!, options, with-*, create-grid) and ag-grid-cljs.impl.convert (+ the future vendored ag-grid-cljs.impl.bean). Decide: root segment (ag-grid-cljs vs unisoma.ag-grid vs other), core vs split public namespaces (e.g. separate enterprise ns per the module-registry decision), impl.* convention, and naming rules for builders/API fns (with-* prefix? bang conventions for register!/set-license-key!?).

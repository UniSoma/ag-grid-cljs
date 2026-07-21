---
id: agd-01ky0ed8766f
title: 'Walking skeleton: Fulcro integration with transactional data updates'
status: open
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:25.958172419Z'
updated: '2026-07-21T01:38:51.912881747Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0ed83xww
---

## Description

## Question

Prove the Fulcro bar: the skeleton grid living inside a Fulcro component (mount-point pattern, no React diffing inside the grid), with row updates flowing through the explicit data channel (set-rows! / transact! mapped to AG Grid transactions) without losing grid state (scroll, selection, focus). Outcome informs the options-diffing grilling ticket.

## Notes

**2026-07-21T01:38:51.912881747Z**

From the cell-renderer resolution (agd-01ky0ed8adbf): react/react-renderer is committed with a per-cell local React root — cells are detached from the host tree (no React context, no Fulcro app context; Fulcro db->UI refresh never reaches them; grid refresh drives cell re-render). This skeleton must prove transact!-from-a-cell with an EXPLICIT app reference (comp/transact! app [...]) is ergonomically acceptable. Only if that fails, specify a portal variant (one host root + createPortal per cell, ag-grid-react's architecture): cells join the host tree (context flows, batched renders) at the cost of host cooperation (portal-manager component mounted in the Fulcro root) and sync machinery (empty-cell flash). Perf context: local roots are fine for sparse interactive columns, wrong for React-everywhere grids (scroll churn ~100-300 root create/destroy/s, flushSync defeats batching).

---
id: agd-01ky0ed8766f
title: 'Walking skeleton: Fulcro integration with transactional data updates'
status: closed
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:25.958172419Z'
updated: '2026-07-21T02:23:54.318612692Z'
closed: '2026-07-21T02:23:54.318612692Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0ed83xww
assignee: jonas
---

## Description

## Question

Prove the Fulcro bar: the skeleton grid living inside a Fulcro component (mount-point pattern, no React diffing inside the grid), with row updates flowing through the explicit data channel (set-rows! / transact! mapped to AG Grid transactions) without losing grid state (scroll, selection, focus). Outcome informs the options-diffing grilling ticket.

## Notes

**2026-07-21T01:38:51.912881747Z**

From the cell-renderer resolution (agd-01ky0ed8adbf): react/react-renderer is committed with a per-cell local React root — cells are detached from the host tree (no React context, no Fulcro app context; Fulcro db->UI refresh never reaches them; grid refresh drives cell re-render). This skeleton must prove transact!-from-a-cell with an EXPLICIT app reference (comp/transact! app [...]) is ergonomically acceptable. Only if that fails, specify a portal variant (one host root + createPortal per cell, ag-grid-react's architecture): cells join the host tree (context flows, batched renders) at the cost of host cooperation (portal-manager component mounted in the Fulcro root) and sync machinery (empty-cell flash). Perf context: local roots are fine for sparse interactive columns, wrong for React-everywhere grids (scroll churn ~100-300 root create/destroy/s, flushSync defeats batching).

**2026-07-21T02:10:01.108997313Z**

Skeleton built and headless-verified (commit e4dba7d). Fulcro 3.9.5 (dev-harness dep only; with-react18 is a deprecated no-op — React 19.2 works out of the box) as reference consumer at /fulcro.html (:fulcro-app build; npm run dev now watches both pages).

What it proves, all with ZERO library support for Fulcro:
- Mount-point pattern: GridHost defsc renders a stable ref div, shouldComponentUpdate false, createGrid in componentDidMount, destroy! in componentWillUnmount — ~15 lines of consumer code. Fulcro db->UI refreshes re-render the page around the grid (summary <p> updates) without touching grid DOM (verified by element identity).
- Explicit data channel (new library fns set-rows!/transact!/destroy! in core): mutations swap the normalized db, then push the same delta through grid/transact! ({:update [(person->row person)]} etc., matched by :get-row-id). Headless Chromium check: scroll position, single-row selection, and cell focus all survive an :add transaction, an :update transaction, and a set-rows! full swap (AG Grid diffs by row id); grid never remounts; 200->201 rows and payroll stay in sync with the Fulcro summary in both directions.
- transact!-from-a-cell: Actions column is a react-renderer (detached local root, no context); onClick is one line — (comp/transact! APP [(give-raise {:person/id id})]) — with APP the defonce'd app. Ergonomics look acceptable; no portal variant needed on this evidence.

Friction found (data points for the options-diffing ticket agd-01ky0edx8dzc): dual bookkeeping is real but small — each mutation states its delta twice (db swap + grid transaction) and needs an entity->row fn; row-selection new API needs :enable-click-selection true (checkbox default); AG Grid v36 renamed scroll DOM classes (.ag-grid-viewport) — API unaffected. Check script kept out of the repo (scratchpad, playwright-driven; 5 assertions green).

Awaiting HITL reaction before closing.

**2026-07-21T02:22:28.290422159Z**

Grounding check (research subagent, Fulcro repo + developers guide, 2026-07): the class-based GridHost is CONFIRMED as the recommended pattern, not legacy. defsc class lifecycle is fully supported (no deprecations in 3.9.x/3.10.0-RC3); the defsc docstring explicitly blesses sCU-forced-false 'so that other libraries can control the sub-dom'; the guide's only documented imperative-widget pattern ('Taking control of the sub-DOM (D3, etc)') is class-based and structurally identical to GridHost. Hooks variant (:use-hooks? + use-ref + use-lifecycle + comp/memo or :use-hooks? :pure) exists but only approximates never-re-render via props-equality memoization — weaker than sCU false. Explicit-app transact! from a detached root is the book's own sanctioned pattern (rc/transact! against a defonce'd app in 'Fulcro Hook Components from Vanilla React'); hooks/use-component noted for cells that ever need live db subscriptions. 3.9's breaking change was the render root (createRoot default, React <=17 dropped, with-react18 no-op) — already reflected in the skeleton. Guide's D3 example uses sCU as a props-changed callback to the widget; we intentionally don't (explicit channel instead) — data point for the options-diffing ticket.

**2026-07-21T02:23:54.229863605Z**

# Resolution

Skeleton approved (HITL). The Fulcro bar is proven with zero library support for Fulcro:

- **Mount-point pattern**: class-based GridHost defsc (ref div, componentDidMount createGrid, componentWillUnmount destroy!, shouldComponentUpdate false) — ~15 lines of consumer code, confirmed by grounding research as the guide's canonical 'library controls the sub-DOM' pattern (not legacy; hooks variant is weaker — memo only approximates never-re-render). Fulcro db->UI refreshes flow around the grid without touching it.
- **Explicit data channel** (library additions: set-rows!, transact!, destroy!): headless-verified that scroll, selection, and focus survive :add and :update transactions AND a set-rows! full swap (AG Grid diffs by :get-row-id); grid DOM never remounts; grid and Fulcro summary stay in sync both directions.
- **transact!-from-a-cell**: react-renderer cell in a detached local root calls (comp/transact! APP [(give-raise ...)]) — one line with an explicit defonce'd app reference; sanctioned by the book's own detached-root example. Ergonomics acceptable — NO portal variant needed.
- **Dual bookkeeping verdict**: eliminable. Default consumer pattern = mutations swap only the db, then set-rows! with rows derived from the db (single bookkeeping, zero new API, state-preserving as proven). Manual transact! stays as the fine-grained/perf channel. A generic auto-diff watcher (normalized-table watch -> grid transactions) is possible and cheap over normalized tables, but is Fulcro-flavored machinery — deferred to the options-diffing ticket (agd-01ky0edx8dzc) as a possible optional adapter; core stays framework-agnostic.
- Fulcro 3.9.5 + React 19.2 work out of the box (with-react18 is a deprecated no-op; 3.9 moved the render root to createRoot).

Asset: src/dev/ag_grid_cljs/dev/fulcro_app.cljs at /fulcro.html (commit e4dba7d).

**2026-07-21T02:23:54.318612692Z**

Fulcro bar proven, zero library support needed: class-based mount-point host (canonical per guide), set-rows!/transact! channel preserves scroll/selection/focus across transactions and full swaps, cell transact! with explicit app ref acceptable (no portal variant). Dual bookkeeping eliminable via set-rows!-from-db; auto-diff watcher deferred to options-diffing. Library gained set-rows!/transact!/destroy!.

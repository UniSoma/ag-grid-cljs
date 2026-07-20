---
id: agd-01ky0ed8adbf
title: 'Walking skeleton: CLJS custom cell renderer helper'
status: in_progress
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:26.061235644Z'
updated: '2026-07-20T20:53:32.121182618Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0ed83xww
assignee: jonas
---

## Description

## Question

Prove the cell-renderer ergonomics risk: a helper that lets users define custom cell renderers in ClojureScript against AG Grid's vanilla renderer interface (init/getGui/refresh). Explore at least: plain DOM/hiccup->DOM helper, and mounting a React/Fulcro component into the cell (portal or local root) for React-hosted apps. Decide which helper(s) the library commits to.

## Notes

**2026-07-20T20:53:32.121182618Z**

Skeleton built and headless-verified. Three renderer styles proven against AG Grid v36 vanilla interface (init/getGui/refresh/destroy):

1. **Plain fn shorthand** (ICellRendererFunc) — zero helper needed: the converter's existing fn auto-wrap gives kebab bean params and a string/DOM return works as-is. Baseline for simple cells.
2. **ag-grid-cljs.render/dom-renderer** — (fn [params-bean] hiccup|Node|string) -> component class. Minimal hiccup->DOM (~40 lines, :style maps, on-* event fns) vendored in the library ns; content lives in a <span> container so refresh swaps in place (returns true). Built on render/renderer, the low-level lifecycle-map helper ({:init :get-gui :refresh :destroy}, per-instance state atom instead of this).
3. **ag-grid-cljs.react/react-renderer** — optional ns (consumers without React never require it; react-dom is not a core dep): per-cell local root, createRoot in init + flushSync render (async default would flash empty cells), refresh re-renders into the same root (React local state survives — verified by clicking a useState counter then refreshCells force), unmount in destroy.

Interop hazards found and locked into the helpers + contract tests:
- AG Grid detects a component class via prototype && 'getGui' in prototype — the converter's fn auto-wrap would strip that, so helpers return (raw class). 
- A JS constructor returning an object hijacks new — ctor must return nil.
- AG Grid .then's any non-null init return (deferred-init protocol) — the init wrapper swallows the user fn's return.

Verified: 13 node tests / 36 assertions green; headless Chromium 11/11 (5 rows, all three styles render, hiccup style map applied, React counter interactive, state + DOM content survive refreshCells force). Decision pending HITL: which helper(s) does the library commit to?

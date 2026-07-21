---
id: agd-01ky0ed8adbf
title: 'Walking skeleton: CLJS custom cell renderer helper'
status: closed
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:26.061235644Z'
updated: '2026-07-21T02:37:00.804149476Z'
closed: '2026-07-21T01:38:41.238567937Z'
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

**2026-07-21T01:38:41.138818988Z**

# Resolution

Grilled and decided (HITL). The library commits to three renderer tiers, none owning a DOM-building engine:

1. **Bare fn shorthand** = the vanilla escape hatch. Officially documented AG Grid behavior: a function renderer's string return is injected via innerHTML (_loadTemplate). We keep vanilla semantics (the wrapper never silently rewrites AG Grid's own contract) but dev-warn when a fn in a *CellRenderer position returns a string containing '<' (XSS trap; ag-grid-react precedent: React's escaping means strings-as-text there, HTML needs dangerouslySetInnerHTML). A fn returning a DOM Node also works here untouched — this is where BYO hiccup->DOM engines plug in, by plain composition, no config API.
2. **render/renderer** — lifecycle map ({:init :get-gui :refresh :destroy}, per-instance state atom) -> component class (raw-wrapped). The tier for refresh-in-place / destroy hooks / instance state.
3. **render/dom-renderer** — engine-free sugar over renderer: (fn [params-bean] Node|string), string ALWAYS means text (createTextNode), refresh swaps content in a <span> container. The mini hiccup engine was cut: dialect trap vs real hiccup, maintenance treadmill, and BYO engines compose at the fn level (pluggable-by-composition beats a global config slot).
4. **react/react-renderer** — committed with per-cell local root (createRoot + flushSync; refresh re-renders same root so React local state survives). Right for sparse interactive columns; documented as wrong for React-everywhere grids (scroll churn ~100-300 root create/destroy per second; flushSync defeats batching). The portal variant (one host root + createPortal per cell, ag-grid-react's architecture) is deferred to the Fulcro skeleton ticket: prove transact!-from-a-cell with an explicit app reference; specify a portal variant only if that fails ergonomically.

Also settled en route: no wrapper-level typed-renderer catalog (Mantine DataTable precedent: none, pure composition; AG Grid built-ins reachable by name string, e.g. "agCheckboxCellRenderer"; AG Grid Cell Data Types v31+ auto-wires boolean/date/number columns and dataTypeDefinitions is plain options data our conversion contract already handles — a with-data-type-definitions builder noted on the builder-catalog ticket).

Verified: 14 node tests / 38 assertions; headless Chromium 11/11 (all three styles, React state survives refreshCells force).

**2026-07-21T01:38:41.238567937Z**

Cell-renderer ergonomics decided: bare fn = vanilla escape hatch (innerHTML semantics kept, dev-warn on HTML-looking string returns); render/renderer lifecycle-map + engine-free dom-renderer (string=text, BYO DOM via composition — hiccup engine cut); react/react-renderer committed with per-cell local root, portal variant deferred to Fulcro skeleton. No typed-renderer catalog (AG Grid cell data types + named built-ins cover it). 14 tests + 11/11 headless green.

**2026-07-21T02:37:00.804149476Z**

Post-resolution corroboration: docs/research/ag-grid-react-wrapper.md §3 independently confirms the tier design — plain-DOM renderer (option c) is the safe default; per-cell React root (option b, our react-renderer) works but is leak-prone: React issue #26281 shows nested createRoot() roots are NOT unmounted when the parent root unmounts and their effect cleanups don't run, so react-renderer must wire explicit root.unmount() to cell-destroy (virtualization destroys off-screen cells constantly). Reproducing ReactUI-style direct mounting (option a) is not viable — AG Grid's React tree is a private root with no injection point.

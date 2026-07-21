---
id: agd-01ky157wpbpv
title: 'Reagent/UIx adapters: in scope or out?'
status: open
type: task
priority: 3
mode: hitl
created: '2026-07-21T01:39:27.563335742Z'
updated: '2026-07-21T02:36:50.776373118Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
---

## Description

## Question

react/react-renderer already accepts any React element, so Reagent (r/as-element) and UIx ($) components mount today via one line of user composition. Decide: does the library ship thin adapter sugar (e.g. reagent-renderer / uix-renderer namespaces with their own optional deps), document the composition recipe only, or rule adapters out of scope? Context: cell-renderer resolution on agd-01ky0ed8adbf (per-cell local root committed; portal question parked on the Fulcro skeleton).

## Notes

**2026-07-21T02:36:50.776373118Z**

Side research relevant to this ticket: docs/research/ag-grid-react-wrapper.md §2–§3 and §5. Key facts for scoping React-facing adapters: (1) ReactUI mounts all custom components inside AG Grid's own *private* React root (agGridReactUi.tsx renders one <GridComp>; no createRoot per cell, no public injection point) — so an adapter cannot reproduce ReactUI-style direct mounting. (2) React issue #26281: a createRoot() root nested inside another root's tree is NOT unmounted with the parent and its effect cleanups don't run — each nested root needs explicit root.unmount(); amplified by cell virtualization churn. (3) §5: the proven Reagent/re-frame embedding is the two-component split with a static mount div (Day8 'Using Stateful JS Components'), which is what a Reagent adapter would package. Caveat: analysis pinned to ag-grid-react ~v31–32 source (current through 35.x).

---
id: agd-01ky157wpbpv
title: 'Reagent/UIx adapters: in scope or out?'
status: closed
type: task
priority: 3
mode: hitl
created: '2026-07-21T01:39:27.563335742Z'
updated: '2026-07-22T01:27:38.601400720Z'
closed: '2026-07-22T01:27:38.601400720Z'
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

**2026-07-21T22:47:53.737638375Z**

From options-diffing resolution (agd-01ky0edx8dzc, closed): `update-grid!` in core is deliberately a PATCH op (absent key = leave-as-is, never revert-to-default). TRUE declarative full-state — where the opts map is the complete desired state and dropped keys revert to default — was rejected as a core default (fragile registry :default dependency across AG Grid versions; punishes partial/builder opts maps). If a reactive adapter wants full-state declarative semantics, that opt-in layer is built HERE on top of update-grid!, not in core. Also relevant: core owns the diff (GridHandle stash), so an adapter wraps update-grid! rather than re-implementing diffing.

**2026-07-22T01:27:38.601400720Z**

Reagent/UIx adapters ruled OUT of scope for V1 — ship no framework adapters (neither Reagent/UIx cell-renderer sugar nor a reactive/full-state-declarative layer); core stays framework-agnostic. (1) Door already open, zero forward-compat work: a future adapter effort wraps three already-public seams — update-grid! (PATCH differ) + GridHandle {:api :opts} for a reactive layer, react/react-renderer (per-cell local root) for React-flavored cell sugar, raw/grid-api as escape hatch. This resolves the full-state-declarative layer punt from options-diffing (agd-01ky0edx8dzc). (2) Docs deliverable: ONE prose recipe page hung off the cell-renderer docs (not a code namespace) — Reagent r/as-element + UIx $ one-liner into react-renderer; the static-mount-div two-component split for stateful re-frame/Reagent components (docs/research/ag-grid-react-wrapper.md §5); the nested-createRoot unmount caveat (React #26281, amplified by cell virtualization churn); a light pointer naming the reactive seams. (3) Fulcro-is-the-bar stays true — the bar is a proof target, not shipped code; Fulcro required zero adapter (agd-01ky0ed8766f).

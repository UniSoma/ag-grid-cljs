---
id: agd-01ky0m0btmrp
title: Namespace layout and public API naming
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-20T20:38:15.124044420Z'
updated: '2026-07-21T16:47:00.569713649Z'
closed: '2026-07-21T16:47:00.569713649Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

Lock the library's namespace layout and public-API naming conventions. The walking skeleton (agd-01ky0ed83xww) established provisional names to react against: ag-grid-cljs.core (raw, register!, options, with-*, create-grid) and ag-grid-cljs.impl.convert (+ the future vendored ag-grid-cljs.impl.bean). Decide: root segment (ag-grid-cljs vs unisoma.ag-grid vs other), core vs split public namespaces (e.g. separate enterprise ns per the module-registry decision), impl.* convention, and naming rules for builders/API fns (with-* prefix? bang conventions for register!/set-license-key!?).

## Notes

**2026-07-21T16:46:56.087448199Z**

**2026-07-21T16:47:00.569713649Z**

Namespace layout + API naming locked: root ag-grid-cljs; fat core + render/react/enterprise satellites; internals under .impl; !-marks-side-effects (create-grid->create-grid!); builders with-* opts-first-returns-opts. Full detail in resolution note.

## Resolution

**Root segment:** `ag-grid-cljs` (matches artifact; idiomatic short root; always aliased so low collision risk). Org-scoped (`unisoma.ag-grid`) and Maven-matched roots rejected as buying little over an aliased short root.

**Public namespaces — fat core + satellites (rejected: split builders into own ns, and single-facade re-export):**
- `ag-grid-cljs.core` — setup + builders + runtime API: register!, raw, options, with-*, create-grid!, destroy!, set-rows!, transact!
- `ag-grid-cljs.render` — renderer, dom-renderer
- `ag-grid-cljs.react` — react-renderer (isolates the React dep from grid-only users)
- `ag-grid-cljs.enterprise` — opt-in; set-license-key! + enterprise-only helpers (name mirrors AG Grid Community/Enterprise vocabulary; per the module-registry decision agd-01ky0eck3myz)

**Internals:** everything under `ag-grid-cljs.impl.*` (impl.convert, future impl.bean). `.impl` is the committed 'private, may change' marker; nothing outside .impl is off-limits.

**Naming rules:**
- `!` marks any side-effecting fn (DOM mutation, global registration, live-grid mutation): create-grid!, register!, destroy!, set-rows!, transact!, set-license-key!. Pure map-builders/accessors stay bang-free: options, raw, all with-*.
- Builders: every curated builder is `with-<thing>`, takes opts first, returns opts, ->-threadable. (Catalog contents = separate ticket agd-01ky0edx5mfs.)

**Implied code drift for the implementation effort (planning-first, not applied here):** rename skeleton `create-grid` -> `create-grid!` in ag-grid-cljs.core.

No new tickets surfaced; no fog graduated.

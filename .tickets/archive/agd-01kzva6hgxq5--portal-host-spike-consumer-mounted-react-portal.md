---
id: agd-01kzva6hgxq5
title: 'Portal host spike: consumer-mounted react/portal-host + portal-renderer (tier beside per-cell roots)'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-08-12T15:42:03.288489320Z'
updated: '2026-08-12T16:35:18.968196418Z'
closed: '2026-08-12T16:35:18.968196418Z'
tags:
- react
- spike
- adr
acceptance:
- title: 'Spike harness in the browser suite exercises create-grid!, refreshCells force, set-rows! from useEffect with portal cells: zero DEV errors, zero empty-cell-paint frames'
  done: true
- title: Provider-based cell content (raw createContext + an error boundary) renders under consumer providers and boundaries with zero bridging
  done: true
- title: Missing-host and second-host dev warnings fire per design; never a per-cell-root fallback
  done: true
- title: ADR 0024 records the decision evidence-first, incl. the machinery-moves-not-dies deflation and the beside-not-replacing intent; 0023 gains the pointer note; CONTEXT.md updated
  done: true
links:
- agd-01kzva5kq1j5
---

## Description

Reopens the question ADR 0023 explicitly severed ('context flow is a separate, consumer-facing API question this ADR deliberately does not open') — legitimately open because 0023's kill rule never measured context inheritance and the design it rejected was a LIBRARY-created host, not this one. Origin: field-consumer submission wip/ask.md (Agilis/Mantine, silent empty cells from provider-based content). Decision from the grilling session: spike proceeds NOW on architectural grounds — per-cell provider wrapping makes cells manage what they should only consume; elegance is the criterion, measurement is telemetry, not gate. Design: the CONSUMER mounts react/portal-host once under their providers; cells createPortal into AG Grid cell DOM from that host, so providers, theme, locale, stores AND error boundaries flow natively (error boundaries are the thing no value-bridge can deliver into a separate root).

## Design

Agreed shape (grilled, all confirmed): (1) BESIDE, not replacing — new react/portal-renderer + react/portal-host; per-cell react-renderer untouched; ADR to declare intent that per-cell roots may retire in a later major only after the portal has production hours. Auto-switch on host presence ruled out hard (silent architecture change = new silent failure mode). (2) Host discovery: module-level registry — host registers on mount, portal cells look it up; dev-warn on second host mount; precedent is react.cljs's module-level render queue ('one flush covers every cell of every grid on the page'). Keeps live object refs out of the options map (ADR 0021); portal-renderer stays a deferred, rebuild-stable value. (3) Missing host: queue cell registrations, drain when a host mounts; dev-warn if the queue is non-empty after a macrotask ('portal cells waiting — mount react/portal-host under your providers'). Never fall back to a per-cell root. (4) Known deflation, record honestly in the ADR: the scheduling machinery MOVES INTO THE HOST rather than dying — host commits must land before paint, so cell registrations drain in one flushSync per microtask (ADR 0023: a portal host escapes a React commit the same way, a microtask, or not at all). What dies: per-cell createRoot/unmount lifecycle, the deferred-unmount correction, the destroyed-guard race, the #26281 per-cell audit (N to 1). KILL RULE (correctness gates): (a) no DEV error class, (b) no empty-cell paint, (c) provider-based cell content renders under the consumer's own providers AND error boundaries with zero consumer-side bridging. Scroll-churn/provider-cost measurement rides along as telemetry only. Close-out work: ADR 0024 written evidence-first with the spike table (0023's shape) covering the whole context-flow answer including the wrap-pattern and diagnostic decisions from agd-01kzva5kq1j5; pointer note on 0023's 'deliberately does not open' line; CONTEXT.md gains a portal-host term and the Renderer tiers entry is amended; docs use the existing tier numbering (no 4a/4b).

## Notes

**2026-08-12T16:35:18.968196418Z**

Portal tier shipped: react/portal-host + react/portal-renderer beside per-cell roots, all four acceptance criteria met. Kill rule passed on all three gates in the committed browser harness (react_portal_test.cljs): zero DEV errors on create/refreshCells-force/set-rows! from useEffect; empty-cell-paint {:create 1, :refresh 0, :set-rows 1} = ADR 0023's all-tier baseline (the residual frame is AG Grid's own async cell build), refresh leg zero; raw-createContext provider value AND a class error boundary reach portal cells with zero bridging. Host discovery via module-level registry (render-queue precedent, no live refs in options, portal-renderer deferred+rebuild-stable); hostless cells queue and drain into a late host, warning once per hostless episode (per-episode latch, ADR 0022 live-relationship period — reviewers flagged once-per-process as too silent); second host warns once, inert, takes over on first's unmount (tested). Machinery-moves-not-dies recorded: host commits drain on the SAME one-flushSync-per-microtask queue; per-cell createRoot/unmount, deferred-unmount trade-off, destroyed guard, and the #26281 audit all gone from this tier. ADR 0024 written evidence-first with beside-not-replacing intent (retirement earliest in a later major, only with production hours); 0023 pointer note, 0011 correction note, CONTEXT.md Portal host term + 4-level Renderer tiers, cell-rendering.md Tier 4, framework-composition wrap-vs-portal. Scroll-churn/provider-cost telemetry NOT collected (recorded honestly in ADR); empty-frame deltas logged by the harness each run. Node 114/364, browser 28/95, both green across repeated runs. Commit be997f9.

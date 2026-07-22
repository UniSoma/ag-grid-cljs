---
id: agd-01ky55nrwypz
title: 'Spec assembly: format and home of the final design spec'
status: closed
type: task
priority: 3
mode: hitl
created: '2026-07-22T15:04:00.154370413Z'
updated: '2026-07-22T18:12:13.219253063Z'
closed: '2026-07-22T18:12:13.219253063Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
deps:
- agd-01ky55n5xn5c
- agd-01ky55n9xka3
- agd-01ky55neshd2
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

All design decisions live in closed ticket resolutions. Decide the shape of the final deliverable: format and home of the design spec (docs/adr/ per-decision records? single spec document? both?); how ticket resolutions translate into it (verbatim lift vs re-synthesis); what the walking-skeleton code's status is in the handoff (reference implementation? scaffold to keep?, noting the known drift: create-grid → create-grid!); and the definition of done that lets implementation start as a fresh effort with no open questions.

Blocked by the three remaining decisions (theming, testing, docs) — the spec assembles after the last decision lands.

## Notes

**2026-07-22T18:12:13.219253063Z**

Spec assembled and committed. Home: CONTEXT.md (pure glossary per domain-modeling convention) + docs/spec.md (index: architecture overview, ADR table, skeleton status + drift list, scope boundary, research pointers) + docs/adr/0001..0015 — one self-contained ADR per locked decision (charting decisions included), full substance lifted from ticket resolutions (light re-edit into Context/Decision/Consequences/Considered-options) because tickets are ephemeral; ticket ids kept as provenance only. Research tickets extracted verbatim to docs/research/ag-grid-module-registry-esm-enterprise.md and ag-grid-options-surface-extraction.md. Skeleton status: scaffold to keep, evolved in place; drift enumerated in spec.md (create-grid→create-grid!, GridHandle+update-grid!, catalog gaps, registry/codegen, cljs-bean vendoring, browser-test+CI, docs tree). Definition of done met: every decision traces to an ADR or spec.md; implementation can start as a fresh effort with no open questions.

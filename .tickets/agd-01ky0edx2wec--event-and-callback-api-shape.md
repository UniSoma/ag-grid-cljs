---
id: agd-01ky0edx2wec
title: Event and callback API shape
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:47.323230923Z'
updated: '2026-07-20T19:00:47.323230923Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
deps:
- agd-01ky0ed83xww
---

## Description

## Question

Decide the event/callback surface: naming ((grid/on-event :row-selected ...) vs per-event builders vs :on-* option keys); exactly what handlers receive (cljs-bean-wrapped event? raw JS event plus helpers? both?); how handler identity/re-registration works with the declarative options-diffing model; and how AG Grid's ~150 event names are exposed/validated (ties into the key registry). Informed by friction found in the core-mount skeleton.

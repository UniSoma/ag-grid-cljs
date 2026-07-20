---
id: agd-01ky0ed83xww
title: 'Walking skeleton: core mount from builder-produced EDN options'
status: in_progress
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:25.852612916Z'
updated: '2026-07-20T20:14:49.696397107Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0eck96vn
assignee: jonas
---

## Description

## Question

Prove the core design end to end at minimal scale: a shadow-cljs (>=3.3.0) app that mounts AG Grid Community via createGrid from an EDN options map produced by a small first cut of the builder API ((grid/options), with-columns, with-default-col-def), using the conversion rules decided in the conversion-boundary ticket. Functions must survive conversion; rows are JS by contract. This ticket also settles the library repo scaffold (deps.edn/package.json layout, dev app location). Asset: running prototype in this repo.

---
id: agd-01ky0edx8dzc
title: 'Options-diffing semantics: what is updatable and how changes apply'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:47.500211676Z'
updated: '2026-07-20T19:00:47.500211676Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
deps:
- agd-01ky0ed8766f
---

## Description

## Question

Define the declarative update contract: which grid options the differ applies via setGridOption vs which are initial-only (and what happens when an initial-only option changes — warn? recreate grid?); diff granularity for nested structures (columnDefs identity); and how the explicit data channel and the differ stay out of each other's way (row-data key ignored by differ?). Informed by the Fulcro skeleton's update behavior.

---
id: agd-01ky0ed8766f
title: 'Walking skeleton: Fulcro integration with transactional data updates'
status: open
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:25.958172419Z'
updated: '2026-07-20T19:00:25.958172419Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0ed83xww
---

## Description

## Question

Prove the Fulcro bar: the skeleton grid living inside a Fulcro component (mount-point pattern, no React diffing inside the grid), with row updates flowing through the explicit data channel (set-rows! / transact! mapped to AG Grid transactions) without losing grid state (scroll, selection, focus). Outcome informs the options-diffing grilling ticket.

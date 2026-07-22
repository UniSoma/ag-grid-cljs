---
id: agd-01ky55n9xka3
title: 'Testing story: unit vs browser split and CI shape'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-22T15:03:44.819286204Z'
updated: '2026-07-22T15:03:44.819286204Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
---

## Description

## Question

Define the library's testing story. Data points from the skeleton: a node-test contract suite (14 tests, npm test, shadow-cljs :test build) plus per-ticket Playwright headless-Chromium checks whose scripts were kept OUT of the repo. Decide: what lives in the committed suite (conversion contract, builders, registry/validation) vs what needs a real browser (mount, renderers, Enterprise); whether browser tests are committed and if so with what harness; what CI runs (pure-Clojure per the key-registry decision — does headless Chromium fit?); and how the Enterprise license constraint (compile-time env macro, CI has no license) shapes the matrix.

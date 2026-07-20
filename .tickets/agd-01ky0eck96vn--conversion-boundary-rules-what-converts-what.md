---
id: agd-01ky0eck96vn
title: 'Conversion boundary rules: what converts, what passes through untouched'
status: open
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:04.518255534Z'
updated: '2026-07-20T19:00:04.518255534Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
---

## Description

## Question

Define the exact rules for the EDN->JS options conversion: which keys get kebab->camel renaming and at what nesting depths; which values must pass through untouched (row data, functions, JS class instances, datasources); how string enum values (e.g. :multi-row vs "multiRow") are handled; how users force raw pass-through for a subtree; and how the same rules run in reverse for what callbacks receive (cljs-bean boundaries). Output: a decision on the conversion contract precise enough to implement in the walking skeleton.

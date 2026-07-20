---
id: agd-01ky0evm9nap
title: 'Key registry: codegen pipeline and dev-warning design'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:08:17.077520809Z'
updated: '2026-07-20T19:08:17.077520809Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
---

## Description

## Question

Given the extraction research (see closed ticket agd-01ky0eck6erk: GridOptions/event names are runtime constants in ag-grid-community; only ColDef needs a small ts-morph pass; recommended output is per-version EDN keyed by kebab keyword with :camel/:type/:deprecated/:doc plus an events block), decide the registry design: where the codegen script lives and when it runs (per AG Grid release); the exact EDN artifact shape shipped in the library; dev-mode warning behavior (unknown key -> warn with did-you-mean, never reject; deprecation warnings?); how warnings are elided from production builds; and how the kebab<->camel reference table is rendered for docs.

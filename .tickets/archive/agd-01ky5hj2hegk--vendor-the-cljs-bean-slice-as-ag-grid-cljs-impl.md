---
id: agd-01ky5hj2hegk
title: Vendor the cljs-bean slice as ag-grid-cljs.impl.bean
status: closed
type: chore
priority: 2
mode: afk
created: '2026-07-22T18:31:41.870413290Z'
updated: '2026-07-22T18:40:44.055071385Z'
closed: '2026-07-22T18:40:44.055071385Z'
acceptance:
- title: impl.bean vendored with EPL headers retained; THIRD-PARTY.md documents the slice, its origin and license
  done: false
- title: conversion boundary requires the vendored namespace; cljs-bean removed as a direct dep
  done: false
- title: npm test green; dev apps still compile
  done: false
---

## Description

Vendor the cljs-bean.core essentials (bean with :prop->key/:key->prop support) under the internal namespace ag-grid-cljs.impl.bean, EPL file headers retained, plus a THIRD-PARTY.md notice (ADR 0005 §8). Retarget the conversion boundary to the vendored slice and drop cljs-bean from deps.edn (removing the TODO there), so the shipped library has zero runtime dependencies beyond the AG Grid peer packages. Prefactor: no blockers, unblocks nothing structurally, but should land before the conversion boundary grows.

## Notes

**2026-07-22T18:40:44.055071385Z**

Vendored cljs-bean.core (+ its from.cljs.core helper) verbatim as ag-grid-cljs.impl.bean/.from-core; transit not vendored. Conversion boundary (impl.convert) retargeted to the vendored ns; cljs-bean dropped from deps.edn (:deps now empty), walking-skeleton TODO removed. EPL-1.0 notices carried on both files (from-core retains upstream header; bean.cljs got one added since upstream core has none); THIRD-PARTY.md documents origin/license/scope. npm test green (0 failures/errors); all three dev apps compile. Committed ecf60f4.

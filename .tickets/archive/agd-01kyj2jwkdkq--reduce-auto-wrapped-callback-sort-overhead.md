---
id: agd-01kyj2jwkdkq
title: Reduce auto-wrapped callback sort overhead
status: closed
type: task
priority: 1
mode: hitl
created: '2026-07-27T15:20:07.533478405Z'
updated: '2026-07-27T16:48:13.383634521Z'
closed: '2026-07-27T16:48:13.383634521Z'
acceptance:
- title: Bench-local variants isolate wrap-fn apply/map, bean/bean option parsing, and scalar return conversion costs
  done: true
- title: Any shipped optimization preserves callback argument bean semantics, return conversion, raw opt-out, and variadic callbacks
  done: true
- title: Node tests and full browser tests pass
  done: true
- title: Same-run before/after 100k-row browser measurements are recorded
  done: true
- title: Vendored cljs-bean files remain unchanged
  done: true
---

## Description

Investigate and reduce the per-comparison overhead of auto-wrapped callbacks on the 100k-row sort benchmark. The shipped fallback path measures ~2.13s versus ~0.15s raw; evidence points to wrap-fn argument conversion and callback-bean construction rather than the literal-key fallback law. Prototype candidates bench-locally before production changes. Do not change vendored cljs-bean bodies or callback semantics. Direct coupling to bean/->Bean requires an explicit user decision.

## Notes

**2026-07-27T15:27:07.820507564Z**

Same-run prototype results (release): Node one-object-arg callback 475.9 ns for baseline variadic map/apply, 338.1 ns for fixed arities with shipped params-bean, and 131.1 ns with fixed arity + direct bean/->Bean; scalar ->js accounts for ~55 ns. Browser pass 2 (100k sort): raw 144.8 ms, baseline fallback wrapper 1985.8 ms, shipped fixed arities 1575.2 ms, direct-constructor prototype 754.3 ms, value-cache 268.9 ms. Fixed 0-3 arities are implemented and node-tested. Direct constructor remains bench-only because it bypasses ADR 0018's public-options implementation and couples to vendored Bean field order; user decision required before shipping.

**2026-07-27T16:48:03.210824052Z**

Decision: ship fixed 0-3 callback arities only. Reject direct bean/->Bean construction despite its measured 0.75s sort because :value-cache true (0.27s) and raw (0.14s) are faster explicit hot paths, while the private constructor would add field-order coupling to every upstream cljs-bean sync. Review found no Standards or Spec issues. Verification: 74 node tests / 229 assertions and 9 browser tests / 31 assertions pass; git diff --check passes; vendored diff is empty.

**2026-07-27T16:48:13.383634521Z**

Shipped fixed 0-3 arities in wrap-fn, avoiding rest/map/apply allocation for common AG Grid callbacks while preserving the variadic fallback and conversion semantics. Same-run browser sort improved 1985.8ms to 1575.2ms (21%); Node callback cost improved 475.9ns to 338.1ns (29%). Recorded and rejected direct bean/->Bean construction: although it measured 754.3ms, value-cache and raw are faster explicit hot paths and do not couple production code to vendored field order. Node and browser suites pass; vendored cljs-bean unchanged.

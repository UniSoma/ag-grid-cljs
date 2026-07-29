---
id: agd-01kyqy3hd3fw
title: Consolidate the remaining emitters onto impl.warn; GridHandle becomes [api opts]
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-29T21:57:16.827002665Z'
updated: '2026-07-29T22:26:31.588599245Z'
closed: '2026-07-29T22:26:31.588599245Z'
tags:
- dev-warnings
- adr-0022
acceptance:
- title: validate's and update-grid!'s emitters route through impl.warn
  done: true
- title: check-fields! keeps its resolved-fields atom and uses warn!, not warn-once!
  done: true
- title: GridHandle is [api opts]; all three ->GridHandle call sites updated
  done: true
- title: One capture helper in src/test/ag_grid_cljs/test_support.cljs, resetting on entry
  done: true
- title: Browser suite still patches console.warn directly and stays green
  done: true
- title: npm test green; npm run test:browser green
  done: true
deps:
- agd-01kyqy2y8c7m
---

## Description

Ticket A landed impl.warn and fixed the two unbounded warnings. This is the consolidation: move validate's and update-grid!'s emitters onto the module, drop GridHandle's dev-only field, and collapse five test capture blocks into one.

Carries the public shape change and the trade-off ADR 0022 §5 took with eyes open (hot reload stops re-firing initial-only warnings), so it reads differently from A and is reviewed separately.

## Design

ADR 0022 §5, §7.

validate.cljs
  delete: defonce warned, private warn-once!, reset-warnings! (all move to impl.warn)
  line ~143 unknown key   -> (warn/warn-once! ::unknown-key [object-name k] ...)
  line ~194 class rule    -> (warn/warn-once! ::class-rule-key [option k] ...)
  check-fields! js/console.warn -> (warn/warn! ...)   ; NOT warn-once! — see below
  rewrite the ADR 0019 §5 collision comment: passing the option keyword into the
  first slot 'so the shared set cannot collide' stops being load-bearing once the
  key space is [site discriminator].

  The field check KEEPS its own atom. It is not a dedup set: it holds *resolved*
  fields, conj'd unconditionally including for fields found present, because it is
  also the short-circuit that stops run-field-check! walking forEachNode on every
  modelUpdated/newColumnsLoaded — and newColumnsLoaded fires on sort and resize.
  A warn-once! owning it would conj only on warn and regress sort/resize (ADR 0017 §9).

core.cljs
  (defrecord GridHandle [api opts])        ; matches CONTEXT.md, which always said {:api :opts}
  (->GridHandle api opts) at core.cljs:289
  delete private warn-once! (:349-357)
  4 update-grid! sites -> warn/warn-once! with ::row-data-ignored / ::initial-only / ::unclassified, disc k
  drop the 'also carries an internal per-handle set' paragraph from create-grid!'s docstring
  promote reset-warnings! from 'test helper' to documented dev affordance (^:dev/after-load)

NEW src/test/ag_grid_cljs/test_support.cljs
  One capture helper that calls reset-warnings! ON ENTRY. Load-bearing, not tidiness:
  cljs.test runs the whole node suite in one process, so process-wide dedup makes
  warning-count assertions order-dependent (ADR 0022 §7). shadow's :test build is
  :test-paths ["src/test"] with the default -test$ discovery, so a support ns is
  required but never run as tests (browser/util.cljs is the precedent).

Tests: 5 capture blocks in 4 namespaces collapse to 1 — core_test:34, validate_test:12,
  field_check_test:13, convert_test:178 AND :204. ->GridHandle arity at core_test:28,45.
  validate_test's fixture repoints v/reset-warnings! -> warn/reset-warnings!.
  The 3 browser namespaces KEEP patching js/console.warn: initial_only_test and
  validation_module_test assert AG Grid's OWN output, which no seam inside impl.warn sees.

## Notes

**2026-07-29T22:06:08.168777347Z**

Mode corrected hitl -> afk. Every decision here is settled and recorded (ADR 0022 §5, §7); nothing in this ticket needs a human to choose anything. The GridHandle change is a public record-shape change, but ADR 0016 §3 puts the library on 0.1.0-SNAPSHOT explicitly to iterate on the public API before the first immutable cut.

Design correction after ticket A shipped: impl.warn ALREADY has reset-warnings! (and warn!, warn-once!, fired). B does not create it — B deletes validate's copy along with validate's defonce warned atom and private warn-once!, and repoints validate_test's fixture from v/reset-warnings! to warn/reset-warnings!.

Also already done in A, do not redo: core_test's capture and convert_test's two inline capture blocks call warn/reset-warnings!. B's job there is to factor those into src/test/ag_grid_cljs/test_support.cljs together with validate_test's and field_check_test's, not to add resets from scratch.

**2026-07-29T22:26:31.588599245Z**

Every emitter routes through impl.warn: validate's set/warn-once!/reset-warnings! deleted, check-fields! on warn! with its own resolved-fields atom, GridHandle down to [api opts] (so a handle is =), core/reset-dev-warnings! as the public hot-reload affordance, and the node suite's capture blocks collapsed into ag-grid-cljs.test-support with a reset on entry. npm test 95/308 green; browser suite 14/45 green; :advanced release build carries no [ag-grid-cljs] warning strings at all.

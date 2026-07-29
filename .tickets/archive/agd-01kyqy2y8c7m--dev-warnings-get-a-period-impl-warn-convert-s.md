---
id: agd-01kyqy2y8c7m
title: 'Dev warnings get a period: impl.warn + convert''s unbounded warnings'
status: closed
type: bug
priority: 1
mode: afk
created: '2026-07-29T21:56:57.227695541Z'
updated: '2026-07-29T22:02:10.936426075Z'
closed: '2026-07-29T22:02:10.936426075Z'
tags:
- dev-warnings
- adr-0022
acceptance:
- title: impl.warn exists with warn!/warn-once!/reset-warnings!, requires neither registry nor validate
  done: true
- title: convert's 7 warn sites route through warn-once! with the site keywords in the design
  done: true
- title: with-pagination's conflict warning fires once per process
  done: true
- title: 'Regression test: N cell renders of an HTML-returning renderer produce exactly one warning'
  done: true
- title: 'Regression test: N with-pagination calls with both keys produce exactly one warning'
  done: true
- title: npm test green; npm run test:browser green
  done: true
---

## Description

Two dev warnings in this library are unbounded. convert's renderer-XSS nudge fires on EVERY cell render of a renderer returning an HTML-looking string. with-pagination's :auto-page-size/:page-size conflict warning fires on EVERY builder call — and ADR 0021 exists to make rebuilding the whole options map per render a supported shape, so that is per render, forever, from a public builder.

Neither is a choice: nothing owned dev warnings, so five emitters each re-decided the prefix, the goog.DEBUG gate and the dedup policy, and two of them answered 'never dedup' by omission. ADR 0022 sets the rule and this ticket lands it where something is actively broken.

Ticket B (the consolidation: validate, update-grid!, GridHandle, test consolidation) follows.

## Design

ADR 0022 §1-4, §6.

NEW src/main/ag_grid_cljs/impl/warn.cljs
  (defonce ^:private warned (when ^boolean goog.DEBUG (atom #{})))  ; registry.cljs's own pattern
  (warn! & msg)                        ; prefix + gate, no dedup
  (warn-once! site discriminator & msg) ; key is [site discriminator]
  (reset-warnings!)                    ; clears the set
  read-only view of the set (test seam)
  Both variadic so (apply str ...) happens INSIDE the gate.
  HARD CONSTRAINT: never requires impl.registry or impl.validate (ADR 0022 §6).
  Dep edge is warn <- convert <- validate.

convert.cljs — delete private warn, require impl.warn, 7 sites:
  key->prop namespaced kw     -> ::namespaced-keyword, disc k
  ->js keyword branch (same message, same site+disc — dedups across both paths)
  non-keyword map key         -> ::non-keyword-key, disc (pr-str k)   [k may be unhashable]
  warn-cljs-collection        -> ::cljs-collection, disc prop
  set passthrough             -> ::set-passthrough, disc nil
  wrap-renderer-fn XSS        -> ::renderer-html, disc nil (ADR 0022 §4)
  The render-time (string? ret)+includes? test STAYS.

core.cljs — with-pagination inline js/console.warn -> (warn/warn-once! ::pagination-conflict nil ...).
  core's private warn-once! and the update-grid! sites are ticket B, NOT here.

Tests: regression test rendering N cells asserting ONE warning; ditto N with-pagination calls.
Note: process-wide dedup makes the node suite order-dependent — any new count assertion needs reset-warnings!. The shared test_support helper lands in ticket B; until then reset inline.

## Notes

**2026-07-29T22:02:10.936426075Z**

impl.warn owns the prefix, the goog.DEBUG gate and the period; convert's 7 sites and with-pagination now route through it (ADR 0022 §1-4, §6).

Both unbounded warnings are fixed. The renderer-XSS nudge fired on every cell render; with-pagination's conflict fired on every builder call, which under ADR 0021's rebuild-per-render shape is every render. Both now fire once per process with a nil discriminator — nothing varies between firings, and the alternatives were all wrong: the renderer fn is a fresh closure per render, the return string would grow the set with row data, and ADR 0019 §5 rejects naming the column.

New src/main/ag_grid_cljs/impl/warn.cljs: warn! (prefix+gate only), warn-once! (key is [site discriminator]), reset-warnings!, fired. Both entry points variadic so str runs inside the gate. Set is (defonce warned (when ^boolean goog.DEBUG (atom #{}))) — impl/registry.cljs's own pattern, needed because this is the first dev-diagnostic ns reachable from production code (impl.convert is the live conversion boundary). It requires neither impl.registry nor impl.validate; the dep edge is warn <- convert <- validate.

Sites: ::cljs-collection (disc prop), ::renderer-html (nil), ::namespaced-keyword (disc the keyword — shared by key->prop and the ->js value branch, so a keyword in both positions warns once), ::non-keyword-key (disc the PRINTED key, since a JS object or fn would dedup by identity and so never dedup across a rebuilt map), ::set-passthrough (nil), ::pagination-conflict (nil).

Tests: three regressions — 500 renders of one renderer, 20 rebuilds with fresh closures, 200 builder calls, all one warning; plus 50 conversions giving one rowData and one context line. reset-warnings! added to core_test's capture and convert_test's two inline blocks: process-wide dedup makes count assertions order-dependent because cljs.test runs the suite in one process. The shared test_support helper is ticket B.

Verified: npm test 93 tests / 304 assertions / 0 failures; npm run test:browser 14 tests / 45 pass / 0 fail; and an :advanced release build contains zero occurrences of the [ag-grid-cljs] prefix or any warning message text, so ADR 0007 §1's elision discipline holds with a production-reachable warn namespace.

core's own warn-once!, validate, GridHandle and the test consolidation are ticket B (agd-01kyqy3hd3fw).

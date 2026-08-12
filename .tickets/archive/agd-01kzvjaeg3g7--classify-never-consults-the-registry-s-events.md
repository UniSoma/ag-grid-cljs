---
id: agd-01kzvjaeg3g7
title: '`classify` never consults the registry''s :events block — every event-handler key update-grid! pushes warns "not in the key registry"'
status: closed
type: bug
priority: 3
mode: afk
created: '2026-08-12T18:03:59.852976441Z'
updated: '2026-08-12T18:30:41.334031343Z'
closed: '2026-08-12T18:30:41.334031343Z'
tags:
- dev-warnings
- update-channel
- dx
acceptance:
- title: Node suite gains a test asserting a handler key applies silently through update-grid!
  done: true
- title: update-grid! pushing :on-row-data-updated, :on-first-data-rendered, or :on-cell-key-down applies the handler via setGridOption and emits no warning
  done: true
- title: :on-cell-value-changed also applies silently — it is a :col-def entry, absent from :grid-options, so it warns today like every other handler
  done: true
- title: 'Initial-only and genuinely-unknown keys are unaffected: update-grid!-initial-only-* and update-grid!-unclassified-applies-and-warns pass unchanged'
  done: true
- title: The :events-derived handler index is defined once and shared between classify and impl.validate's grid-spec, not duplicated, and is goog.DEBUG-gated so the :advanced build still eliminates the registry literal
  done: true
- title: 'Browser suite proves a live handler SWAP: create with :on-cell-clicked A, update-grid! to B, click a cell, B fires and A does not'
  done: true
links:
- agd-01kymx8m23sj
- agd-01ky5hj32e7j
- agd-01ky5hj2t2m9
---

## What to build

`update-grid!` pushing an event-handler key applies it and stays silent. Handler keys are spelled by their handler property (`:on-cell-key-down`, `:on-row-data-updated`), but the registry catalogs events under their event name with the handler in the value, so `classify`'s `:grid-options` lookup misses all ~112 of them and each one falsely warns "not in the key registry; update-grid! applied it optimistically". The apply was always correct — only the claim is wrong.

A key that resolves through the registry's `:events` block by its handler property classifies `:updatable`. The fallback is unconditional: handler entries carry no `:initial?` flag (the codegen emits only `:event`/`:handler`), so there is no initial-only handler to preserve. A key in neither block still warns, unchanged.

Note the four `:on-*` keys that look like an exception — `:on-cell-clicked`, `:on-cell-context-menu`, `:on-cell-double-clicked`, `:on-cell-value-changed` — sit in the registry's `:col-def` block, not `:grid-options`. `:grid-options` holds zero handler-spelled keys, so no handler escapes the false warning today.

`impl.validate` already derives this exact handler index for its did-you-mean check — which is why unknown-key validation does *not* flag `:on-cell-key-down`. That derivation becomes the single shared source rather than being written twice; the registry file is generated and must not be hand-edited. The index stays behind `^boolean goog.DEBUG` so the registry literal keeps being eliminated in `:advanced` (ADR 0007 §1).

Consumer symptom worth reproducing mentally: every shadow-cljs hot reload of a namespace defining handler defns redefines those vars, so the differ genuinely has handler keys to push and each warns. Normal re-renders stay quiet because ns-level defns keep the rebuilt map `=` at those keys.

## Blocked by

None - can start immediately.

## Notes

**2026-08-12T18:23:13.840758339Z**

Grilling session settled the open design points:

- Shared index home: hoisted inside impl.validate (drop ^:private, grid-spec consumes it, core/classify requires it). core already requires validate, so no new dependency direction and no new namespace for one def.
- Docs: ADR 0008 amended in place (Classification section gains a dated amendment restating the classifier as two ordered lookups: :grid-options -> :initial?, else :events -> unconditionally updatable). Not a superseding ADR — the rule widens to a key class it never contemplated, it does not reverse. Callbacks branch bullet refined; a Consequences bullet records the hot-reload symptom. CONTEXT.md gains the term 'Handler key'.
- Browser coverage is IN SCOPE for this ticket, not a follow-up: the node test proves setGridOption was called on a stub, which does not verify the ticket's central claim that the apply was always correct. Sixth acceptance criterion added.
- Browser test shape: SWAP, not add — create with :on-cell-clicked = handler A, update-grid! to handler B, click a cell, assert B ran AND A did not (guards against AG Grid stacking listeners rather than replacing). :on-cell-clicked chosen over :on-cell-key-down because a click on a rendered cell is less brittle headless than focus-then-keydown; both resolve through :events identically. :on-cell-key-down stays covered by the node test.

**2026-08-12T18:30:41.334031343Z**

classify now falls back to an :events-derived handler index (hoisted out of validate/grid-spec, shared by both, goog.DEBUG-gated — verified absent from a release build). All ~112 handler keys apply via setGridOption in silence; initial-only and genuinely-unknown keys unchanged. ADR 0008's sole-classifier rule amended in place, CONTEXT.md gains 'Handler key'. Browser test pins the upstream half: setGridOption REPLACES a live handler rather than stacking one. Both suites green (115 node / 29 browser). Commit 18e79f0.

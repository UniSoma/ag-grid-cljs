---
id: agd-01kzs8dexkb1
title: 'ADR 0018 §2 overclaims: RowNode arguments are never beaned (class instances bypass the object? gate)'
status: closed
type: bug
priority: 2
mode: afk
created: '2026-08-11T20:32:23.987219117Z'
updated: '2026-08-11T20:54:55.353120989Z'
closed: '2026-08-11T20:54:55.353120989Z'
tags:
- callbacks
- docs
- adr
acceptance:
- title: ADR 0018 §2 names the object? predicate and states class instances are raw
  done: true
- title: row-node-argument-data asserts the shipped behaviour against a class-instance fake
  done: true
- title: fake-node is a class instance
  done: true
- title: docs reach node data via (.-data node), not (:data node)
  done: true
---

## Description

ADR 0018 §2 states the callback-bean lookup law covers "params.data, direct row arguments, RowNode arguments". It does not cover RowNode arguments.

All three wrap points gate on cljs.core/object?, i.e. (identical? (.-constructor x) js/Object):

- impl/convert.cljs wrap-arg: (if (object? a) (params-bean a) a)
- impl/convert.cljs bean-transform: (when (object? x) ...)
- impl/bean.cljs:40 (vendored recursive branch): (object? x) (Bean. ...)

RowNode is a class (node_modules/ag-grid-community/dist/package/main.cjs.js:4259, `var RowNode = class {`), so .-constructor is not js/Object and the node is handed back untouched. bean.cljs ->val returns x unchanged when the transform yields nil, which is the path a class instance takes.

User-visible consequence in a non-raw handler:

- (:data node) is nil in isRowSelectable / doesExternalFilterPass — keyword lookup on a raw JS object
- (-> params :node :data) is nil for the same reason
- (.setDataValue (:node params) ...) works — the node is raw

The suite cannot see this because every AG Grid object in it is a plain object literal, which is object?-true:

- src/test/ag_grid_cljs/impl/callback_bean_test.cljs:67 — #js {:node #js {:rowIndex 7 :data ...}}
- src/test/ag_grid_cljs/impl/callback_bean_test.cljs:79 — row-node-argument-data, #js {:id "0" :data ...}
- src/test/ag_grid_cljs/test_support.cljs fake-node — #js {:data data :group group?}

row-node-argument-data asserts "RowNode.data follows the callback-bean lookup law" and passes only because its node is not a RowNode.

## Design

Decided in the set-data-value!/set-row-data! grilling (2026-08-11): this is a DOCUMENTATION defect, not a code one. Class instances stay raw by design.

Rejected alternative — make RowNode follow the law. Two forms, both bad:

1. Bean all class instances. Non-starter: Bean is a deftype with none of AG Grid's methods, so (.getValue (:column params)) and (.getRowNode (:api params)) break. AgColumn is a class too (main.cjs.js:2411, extends BeanStub). The object? gate is the ONLY reason method calls through beaned params work today.
2. Bean RowNode specifically. Makes the conversion boundary position-aware for the first time — the trade ADR 0019's Considered options rejected for consumer-keyed options ("a universal law users can hold in their head" over special cases).

Accepted cost of leaving it: (:data node) returns nil silently and there is no hook to warn on it — the node is not a bean, so nothing of ours is on that lookup path. Docs are the whole mitigation.

Work:

1. Amend ADR 0018 §2 — state the object? predicate, the reason (objects whose methods consumers call must stay callable), and that RowNode/Column/GridApi arguments are raw. The rule to state: BEANS COVER DATA, NOT AG GRID OBJECTS.
2. Docs — params.node.data is a documented dead end; reach it via (.-data node). Check docs/options-and-conversion.md (the ~line 242 "unwrap the backing JS object" passage) and the event/callback article for the same overclaim.
3. Invert row-node-argument-data: assert (:data node) is nil and (.-data node) is the row, against a class-instance fake. A test asserting the opposite of shipped behaviour is worse than no test.
4. Make callback_bean_test.cljs:67's nested node and test_support/fake-node class instances.

## Notes

**2026-08-11T20:34:22.727616766Z**

Decided (grilling, 2026-08-11): NO public unwrap ships. With class instances raw by design, the only beans a consumer holds are over plain data objects, and the surviving use case — mutating params.data in place — is one the write-back recipe (agd-01kzs8e9rwhx) tells you not to do; use .setData. The docs promise was made on a false premise (it read as if RowNode needed unwrapping, the one case that never did). If a real call site turns up, unwrap is five bean?-guarded lines and can ship then.

DONE ALREADY, out of this ticket: docs/options-and-conversion.md line ~240 rewritten — the "unwrap the backing JS object" sentence is gone, replaced by the rule "beans cover data, not AG Grid objects", (:node params) is a real RowNode you call methods on, and node rows are read via (.-data node), not (:data node).

Remaining docs work here is the ADR 0018 §2 amendment plus a sweep of the event/callback article for the same overclaim.

**2026-08-11T20:54:55.353120989Z**

ADR 0018 §2 now names the cljs.core/object? gate and the rule BEANS COVER DATA, NOT AG GRID OBJECTS: RowNode/Column/GridApi arguments are class instances, never beaned, read with interop ((.-data node), not (:data node)). Context, §9, Consequences, Verification and one Considered option de-overclaimed; amendment marker added; same correction in ADR 0009/0010, CONTEXT.md and the wrap-fn docstring. No src/main behaviour change. test_support/fake-node is now a RowNodeFake deftype (a plain #js literal is object?-true and modelled the wrong side of the gate), and row-node-argument-data — which asserted the OPPOSITE of shipped behaviour and passed only because its node was a plain literal — is inverted and renamed row-node-argument-is-not-beaned. Characterization failure observed before inverting. Node suite 114/363 green, browser suite 19 tests/60 pass. Commit 84aa794.

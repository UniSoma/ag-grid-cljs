---
id: agd-01kzs8dtn4f7
title: 'Audit: which node-suite fakes are plain objects standing in for AG Grid class instances, and is any test passing because of it?'
status: open
type: task
priority: 3
mode: afk
created: '2026-08-11T20:32:35.989518042Z'
updated: '2026-08-11T20:32:35.989518042Z'
tags:
- testing
- callbacks
acceptance:
- title: 'Each of fake-col / fake-api / fake-node is classified: fidelity gap load-bearing for a passing test, or not'
  done: false
- title: Any real hole found is written up as its own ticket
  done: false
deps:
- agd-01kzs8dexkb1
---

## Description

agd-01kzs8dexkb1 found that ADR 0018 §2 overclaims RowNode beaning, and that the reason nobody caught it is test fidelity: the node suite models every AG Grid object as a plain #js literal, which is cljs.core/object?-true where the real class instance is object?-false. The two behave differently at exactly the boundary the suite exists to test.

Same shape, unaudited: test_support fake-col, fake-api, fake-node. These feed the always-on live-grid checks (field check ADR 0017, ref-data check ADR 0019 §9) and the callback-bean suite.

## Design

Question to answer, not a change to make. For each fake, ask: does any passing assertion depend on the fake being object?-true where the real object is object?-false?

Highest-suspicion sites: anything that reads a fake THROUGH a bean, or asserts a keyword lookup resolves on something a real grid hands over as a class instance.

Report the answer on this ticket. Widen to a fix ticket only if a real hole turns up — replacing every fake with a class instance for its own sake is not the goal, and fakes are deliberately cheap.

Out of scope: the ADR 0018 amendment and the row-node-argument-data inversion, which are agd-01kzs8dexkb1.

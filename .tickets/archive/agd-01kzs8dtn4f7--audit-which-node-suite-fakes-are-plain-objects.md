---
id: agd-01kzs8dtn4f7
title: 'Audit: which node-suite fakes are plain objects standing in for AG Grid class instances, and is any test passing because of it?'
status: closed
type: task
priority: 3
mode: afk
created: '2026-08-11T20:32:35.989518042Z'
updated: '2026-08-11T20:55:07.670093990Z'
closed: '2026-08-11T20:55:07.670093990Z'
tags:
- testing
- callbacks
acceptance:
- title: 'Each of fake-col / fake-api / fake-node is classified: fidelity gap load-bearing for a passing test, or not'
  done: true
- title: Any real hole found is written up as its own ticket
  done: true
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

## Notes

**2026-08-11T20:55:07.367049420Z**

Scratch test written, run, and deleted; tree confirmed clean of it (`git status` shows only the pre-existing modifications from the prior session).

---

**2026-08-11T20:55:07.670093990Z**

Answered, no fix needed: no fake's object?-trueness is load-bearing for any passing assertion. fake-col and fake-api are read exclusively through interop (validate.cljs field-targets/ref-data-targets, isDestroyed/getColumns/forEachNode/addEventListener) and never enter the callback path where object? is consulted (convert.cljs:215,238 and bean.cljs:40); the validation path's own js-object? (validate.cljs:234) is goog/typeOf-based, true for class instances and plain objects alike, so it cannot diverge across the gate by construction. fake-node's gap was real and is already closed by agd-01kzs8dexkb1. Confirmed empirically, not argued: a scratch suite with deftype ColFake/ApiFake ran both live checks end-to-end — 115 tests, 377 assertions, 0 failures, not one assertion flipped (scratch file deleted). GridApi is a class (main.cjs.js:13037 var GridApiClass = class {}), so it too arrives raw, consistent with the ADR wording. Inverse risk checked and clear: params objects are plain literals mutated in place (addCommon, main.cjs.js:27189-27193), so the plain-object side of the gate is safe. No new ticket raised — the one gap is coverage, not correctness: the object? gate is pinned for :node only, not :column or :api, and nothing passes today that a faithful fake would fail. Fakes stay cheap.

## Verdict

**No fake's `object?`-trueness is load-bearing for any passing assertion.** All three verdicts are negative, and the negative is empirically confirmed, not argued.

- **`fake-col`: gap NOT load-bearing.** `fake-col` is a `#js` literal (`/home/jonasrodrigues/projects/ag-grid/src/test/ag_grid_cljs/test_support.cljs:43`) where the real `AgColumn` is a class. Every read of it in production is dot-notation: `.getColDef` / `.-field` / `.-refData` / `.getColId` / `.isFieldContainsDots` at `/home/jonasrodrigues/projects/ag-grid/src/main/ag_grid_cljs/impl/validate.cljs:259` (`field-targets`) and `:402-413` (`ref-data-targets`). No column ever reaches `params-bean` or `wrap-arg`. Consumers: `field_check_test.cljs:106-124,162,174,198,212`, `ref_data_check_test.cljs:114-125,156,179-183,208,220,236` — all dot-notation or downstream of it.
- **`fake-api`: gap NOT load-bearing.** `#js` literal at `test_support.cljs:73` (and a second local one at `core_test.cljs:14`) where the real `GridApi` is a class. Production reads it only via `.isDestroyed` / `.getColumns` / `.forEachNode` / `.addEventListener` (`validate.cljs:294,348,349` and `install-field-check!`/`install-ref-data-check!`). `core_test`'s copy is read via `unchecked-get`, `identical?`, and record-field access on `GridHandle` — nothing beans it.
- **`fake-node`: no gap remains.** Already a `deftype` (`test_support.cljs:49`, landed by agd-01kzs8dexkb1), so it is object?-false like the real `RowNode`. Read only as `(.-group node)` / `(.-data node)` at `validate.cljs:296-298`.

### The decisive structural fact

`cljs.core/object?` is consulted in exactly three production sites — `convert.cljs:215`, `convert.cljs:238`, `bean.cljs:40` (plus `bean.cljs:80`, an internal snapshot-compatibility test) — **all of them on the callback-bean path.** No fake from `test_support.cljs` is ever handed to a wrapped callback. The validation path uses its own `js-object?` (`validate.cljs:234`), which is `(identical? "object" (goog/typeOf x))` — **true for class instances and plain objects alike**, so it cannot diverge across the gate by construction. The fidelity gap agd-01kzs8dexkb1 found lives only where the two paths meet, and the fakes never enter the callback path.

### GridApi: a class

`GridApi` is a **class instance**, so `object?`-false, exactly like `RowNode` and `AgColumn`.

```
main.cjs.js:13037  var GridApiClass = class {};
main.cjs.js:13039  Reflect.defineProperty(GridApiClass, "name", { value: "GridApi" });
main.cjs.js:13044  this.api = new GridApiClass();          // in ApiFunctionService's ctor
```

The methods are assigned as own properties onto that instance (`api[key] = this.makeApi(key)[key]`, main.cjs.js:13051), but the constructor is `GridApiClass`, not `js/Object` — `(identical? (.-constructor api) js/Object)` is false. So `(:api params)` hands back the raw `GridApi`, consistent with the rule as landed. This confirms rather than complicates the ADR wording.

### Empirical confirmation (done, then deleted)

I wrote `src/test/ag_grid_cljs/impl/scratch_fidelity_test.cljs` defining `ColFake` and `ApiFake` as `deftype`s exposing the same methods via `Object`, asserted `(false? (object? …))` on each, and re-ran a representative slice of both live checks against them: `field-targets` (plain + dotted), `ref-data-targets`, `first-row` (group-skipping), `install-field-check!` end-to-end (listener registration, `get-columns` count, the `"fristName"` warning), and `install-ref-data-check!` end-to-end.

```
Ran 115 tests containing 377 assertions.
0 failures, 0 errors.
```

Not one assertion flipped — the opposite of the characterization failure the node fake produced. The file has been deleted; `git status` shows only the seven pre-existing entries from the prior session plus `wip/`.

### Real holes found

**None that produce a false pass.** One genuine *coverage* absence, weaker than a defect and probably not ticket-worthy on its own: the node suite exercises the `object?` gate only for `:node` (`callback_bean_test.cljs:68-85` and `row-node-argument-is-not-beaned`). There is no equivalent assertion pinning `:column` or `:api` as raw-through-a-bean. Nothing passes today that would fail against a faithful fake — the rule is simply unpinned for two of the three classes it names, so a future change to the gate would be caught by one test rather than three.

I also checked the inverse risk, which would have been the serious one: if AG Grid ever constructed a *params* object as a class instance, the whole bean law would silently return nil for that callback. It does not. Params objects are plain literals mutated in place — `_addGridCommonParams` (main.cjs.js:1434) delegates to `addCommon` (main.cjs.js:27189-27193), which does `params.api = this.api; params.context = …; return params` on the caller's object literal (e.g. main.cjs.js:2686). The plain-object side of the gate is safe.

### What the negative verdict means

The fakes stay cheap. `fake-col` and `fake-api` model AG Grid objects that production reads exclusively through interop, and the one check that inspects their type (`validate.cljs:234`) is deliberately typeof-based precisely so it cannot care. Converting them to `deftype`s would cost readability and buy nothing measurable — the run above is the proof. `fake-node` needed the conversion because it is the one fake whose value crosses into the callback path in the tests that exist; that conversion has landed, and it stops there.

# 0017. Always-on field check: emitted column fields vs the sampled row

- Status: accepted, 2026-07-26
- Origin: knot ticket agd-01kyd3sefe7e (tickets are ephemeral; this record is self-contained)

A column whose `field` names a key the row data does not have renders blank with nothing in the console. The **field check** compares each column's emitted field string against the keys of one sampled row and warns once per field with a did-you-mean. Unlike the **dev validations** it sits beside, it is ON by default in `goog.DEBUG` builds, it is driven by AG Grid events rather than by wrapper call sites, and it reads the emitted JS rather than the authored EDN.

## Context

`ag-grid-cljs.impl.validate` already holds position-aware unknown-key and deprecation warnings over the EDN options map (ADR 0007 §4-5), gated off by default behind `enable-dev-validations!`. Those checks are heuristic: they test consumer keys against a registry pinned to one AG Grid version, so a consumer on a newer AG Grid gets false "unknown option" warnings from version drift. The opt-in gate is the price of that heuristic.

A field-vs-row-shape check is a different animal. It compares two things the consumer supplied to *each other* — their column definitions against their own row data. No registry, no pin, no drift, therefore no reason to make the developer opt in to it. But it needs data the EDN options map does not durably hold: rows leave the options map at creation (`:row-data` is owned by the data channel per ADR 0004 and explicitly refused by `update-grid!`), so any check driven from the EDN can only ever run once, at creation.

The check also cannot live in `impl.convert`: it needs the `levenshtein`/`suggest` helpers in `impl.validate`, and `validate` already requires `convert`.

## Decision

1. **Always on in `goog.DEBUG`, no opt-in.** The field check is not gated by `enable-dev-validations!`. It is registry-free, so the drift argument that justifies the gate does not apply. The namespace docstring is rewritten to scope the gate to the dev validations rather than to the file.

2. **Event-driven, not call-site-driven.** `create-grid!` calls `validate/install-field-check!` once, behind a `goog.DEBUG` guard, immediately after `createGrid` returns. It registers two listeners via `api.addEventListener` — `modelUpdated` (any data arrival, whatever the route) and `newColumnsLoaded` (`columnDefs` replaced) — and then runs the check once. `set-rows!`, `transact!`, and `update-grid!` are untouched.

   The immediate run is load-bearing, not belt-and-braces: `addEventListener` is only reachable after `createGrid` returns, by which point both events have already fired for the initial columns and rows. Without it a grid that is never subsequently modified — the common case, and all three dev apps — is never checked. Attaching earlier would mean routing `onModelUpdated` through the options map, which would clobber a consumer's own handler.

3. **Read the emitted JS, not the authored EDN.** A listener cannot reach the handle's stashed `:opts`: it is a plain value on the `GridHandle` record and `update-grid!` returns a *new* handle, so any closure holds a stale one. Warnings therefore name camel strings (`"fristName"`, not `:frist-name`) and suggest camel row keys. No `camel->kebab` back-translation — the emitted string is what AG Grid is actually looking up and failing to find, which is the fact the developer needs; the kebab source is one mechanical transform away and the developer already knows the transform.

4. **Columns from the flat `Column` list.** `api.getColumns()` plus `Column.getColDef()` per column. Flat, so group `:children` need no recursion and no mutual-recursion pair; and `getColDef()` returns the *merged* def, so a `valueGetter` supplied via `defaultColDef` correctly suppresses the check. `api.getColumnDefs()` is rejected: it reconstructs a nested tree from `originalParent` pointers that we would immediately re-flatten. Consequence: AG-Grid-synthesized columns (auto-group, selection) are in the list and are checked if they carry a field — accepted, since a field is a field.

5. **Row sample from `api.forEachNode`, first node with `data` and without `group`.** Every row model, not client-side only: `forEachNode` delegates to `beans.rowModel` with no client-side guard, so an infinite or server-side grid is checked once a block lands. `forEachLeafNode` was the alternative and is client-side-only by construction (`_getClientSideRowModel(beans)?.…`), which would have silently exempted every non-client grid from a check whose value is identical there.

   Skipping group nodes is required, not tidiness: a CSRM group node has no `data` at all, but an **SSRM group row does** — it carries only the grouping field, and sampling one would report every other column's field as missing.

6. **Skip per field, not per ColDef.** `valueGetter` drops `field` (the getter supersedes it). Nothing drops `tooltipField`: v36's cell tooltip resolver reads `data[tooltipField]` *before* consulting `tooltipValueGetter`, so the tooltip field stays live regardless of either getter. The blunter "a ColDef carrying `valueGetter` is skipped" would miss a computed column with a mistyped tooltip field — a legitimate configuration whose only symptom is an empty tooltip.

7. **Dot notation gated on `Column.isFieldContainsDots()`, first segment only.** The grid option `suppressFieldDotNotation` turns dots into literal characters in a key name; `Column.initDotNotation` already resolves this once at column-build time, so we ask rather than re-derive and cannot drift from AG Grid. Only the first segment is checked: nested objects are legitimately sparse (`{address: null}` on one row, populated on another), so walking deeper would trade a rare catch for a common false positive.

8. **Presence via the `in` operator; suggestions from own enumerable keys.** Deliberately asymmetric. `js-in` walks the prototype chain, so a consumer passing class instances with prototype getters does not get spurious warnings; `js-keys` does not, so `"toString"` is never suggested. For a diagnostic the developer cannot switch off, a false positive is the only failure that matters, and the asymmetry biases both halves toward silence. The check bails silently unless the sample is a JS object — `in` throws on primitives, and a diagnostic must never crash the grid.

9. **Per-grid state in the listener closure.** One atom holding the set of *resolved* field strings, where resolved means a verdict was reached — warned or found present. Warning fires on the transition into the set; the short-circuit is "every current field already resolved, return before touching the row model". One set doing both jobs. Not `validate`'s module-global `warned` atom, for three reasons: the "present in this grid's rows" half is inherently per-grid; a global set silences a real bug on a second grid whose rows have a different shape; and `defonce` survives hot reload, so re-introducing a typo you just fixed would be met with silence.

10. **`impl.validate` owns the api.** The first instinct was to keep `validate` free of AG Grid and have `core` marshal everything into plain data, but the check touches five api methods and that would relocate the knowledge without reducing it. `validate` only calls methods on an object handed to it — no `"ag-grid-community"` require is added, so ADR 0007 §1's dead-code-elimination story is unchanged.

## Consequences

- The namespace now holds two mechanisms with opposite defaults. The glossary (`CONTEXT.md`) names them **dev validations** (opt-in, registry-backed) and **field check** (always-on, registry-free) so the distinction has words; "field validation" is explicitly avoided, since *validation* now denotes the gated family.
- A field that resolves as *present* is never re-checked. A later `set-rows!` with a differently-shaped row will not warn. This is criterion "once per field, not per update" doing what it says, and it is what keeps the steady-state cost at a set-membership test.
- `newColumnsLoaded` also fires on column state changes such as sort and resize, so the check is *invoked* far more often than it does work. The short-circuit absorbs this; without it the `forEachNode` traversal (which has no early exit in AG Grid's loop) would run per sort on large grids.
- Warnings are `console.warn`, and the browser suite's tripwire (`test/browser/run.mjs`) only fails on `console.error` and `pageerror`. The suite therefore does not police the wrapper's own warnings. Widening it would make the field check self-enforcing in CI but requires auditing every warning the suite intentionally provokes; deferred.
- Testing splits on ADR 0015's rule. Node owns the pure core, the ColDef→target mapping, the row-sampling skip logic, and listener registration against a fake api. The browser suite owns the two assertions that are about AG Grid rather than about us: that `getColumns()` flattens group children, and that the events fire where we claim. Dead-code elimination under `{goog.DEBUG false}` is verified by grepping the `:advanced` build.

## Considered options

- **Gate it behind `enable-dev-validations!` like everything else in the namespace** — rejected: uniformity for its own sake. The gate exists to contain registry version drift, and this check has no registry.
- **Hook the wrapper's call sites (`set-rows!`, `transact!`, `update-grid!`)** — rejected: it would let the check name fields in the consumer's own kebab vocabulary, which is better DX, but it leaks. It misses the `grid-api` escape hatch, misses datasource blocks arriving asynchronously, and needs a new hook for every future data path. Coverage was judged worth more than kebab spelling.
- **`getGridOption("rowData")` as the row source** — rejected: `applyTransaction` mutates the row model without touching the `rowData` option, so transaction coverage would have been fake.
- **`getDisplayedRowAtIndex(0)`** — rejected: O(1), but `.data` is `undefined` on a group row and the call returns `undefined` when a filter hides everything, so the check would silently skip in two ordinary configurations.
- **`forEachLeafNode`** — rejected: client-side-only by construction, which would have limited the check to CSRM for no reason other than the shape of the api.
- **Walking the row's dotted path and reporting the first missing hop** — rejected: false-positives on legitimately sparse nested data.
- **Listing the row's actual keys when no close match is found** — rejected for v1: the single most useful thing to print, but unbounded; revisit with a cap if the bare message proves too thin.

## References

- ADR 0004 — update model (the data channel owns rows, which is why the EDN cannot supply them post-creation)
- ADR 0005 — conversion boundary (why the check sees camel strings)
- ADR 0007 §1, §4-5 — key registry: dead-code elimination, and the opt-in validations this sits beside
- ADR 0008 — options diffing (`update-grid!` returns a new handle, which is why a listener cannot hold the stash)
- ADR 0015 — testing split (node vs browser)

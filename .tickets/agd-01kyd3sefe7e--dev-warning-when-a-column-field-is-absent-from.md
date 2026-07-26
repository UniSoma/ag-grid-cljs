---
id: agd-01kyd3sefe7e
title: Dev warning when a column field is absent from the row data
status: open
type: feature
priority: 1
mode: afk
created: '2026-07-25T17:04:58.862565757Z'
updated: '2026-07-26T21:39:57.431768429Z'
acceptance:
- title: :tooltip-field is checked the same way
  done: false
- title: A field that is present is silent; a key whose value is nil counts as present
  done: false
- title: Columns nested in column-group :children are checked
  done: false
- title: Warns once per field, not per row and not per update
  done: false
- title: Fires without calling enable-dev-validations!
  done: false
- title: Runs at grid creation and when columns or rows change
  done: false
- title: Nothing survives a goog.DEBUG false build
  done: false
- title: 'Silent while no leaf data row has loaded: no rows, empty rows, or a datasource that has not delivered yet'
  done: false
- title: A ColDef carrying :value-getter has its :field skipped; its :tooltip-field is still checked
  done: false
- title: A dot-notation field checks only its first segment; with :suppress-field-dot-notation on, the whole field is checked as a literal key
  done: false
- title: The dev apps and the browser suite produce no new warnings, since both are correct under today's rule
  done: false
- title: A :field absent from the sampled row warns once, naming the emitted field string and suggesting the closest key the row actually has
  done: false
---

## Description

A column whose `:field` (or `:tooltip-field`) names a key absent from the row data renders blank with nothing in the console — the only clue is an empty column. Add an always-on dev diagnostic (the **field check**, `CONTEXT.md`) that compares each column's emitted field string against the keys of one sampled row and warns once per field with a did-you-mean.

Lives in `ag-grid-cljs.impl.validate`: it needs the `levenshtein`/`suggest` helpers already there, and `impl.convert` would be a circular require. It is NOT gated by `enable-dev-validations!` — that gate exists to contain the registry's AG Grid version drift, and this check has no registry: it compares two consumer-supplied things. Update the namespace docstring so the gate is not mistaken for covering the whole file.

Design rationale: ADR 0017.

## Design

One entry point, `validate/install-field-check!`, called from `create-grid!` behind a `goog.DEBUG` guard immediately after `createGrid` returns. `set-rows!`, `transact!`, and `update-grid!` are untouched.

```clojure
;; core/create-grid!
(let [api (createGrid el (convert/->js opts))]
  (when ^boolean goog.DEBUG (validate/install-field-check! api))
  (->GridHandle api opts (atom #{})))
```

### Wiring

Register `modelUpdated` (data arriving by any route — `set-rows!`, `transact!`, the `grid-api` escape hatch, a datasource block) and `newColumnsLoaded` (`:column-defs` replaced), then **run the check once immediately**. The immediate run is load-bearing: `addEventListener` is only reachable after `createGrid` returns, by which point both events have already fired, so a grid that is never subsequently modified would otherwise never be checked.

### Columns

`(.getColumns api)` → `(.getColDef col)` per column. Flat, so group `:children` need no recursion; `defaultColDef` is merged in, so a default `:value-getter` correctly suppresses the check. Returns `null` until `colModel.ready` — nil-guard. Synthesized columns (auto-group, selection) are in the list and are checked if they carry a field.

### Row sample

```clojure
(defn- first-row
  "The first loaded leaf data row, or nil. Group rows are skipped: a CSRM group
  node carries no data at all, and an SSRM group row carries only its grouping
  field — sampling one would read as every other field being absent."
  [^js api]
  (let [v (volatile! nil)]
    (.forEachNode api (fn [^js node]
                        (when (and (nil? @v)
                                   (not (.-group node))
                                   (some? (.-data node)))
                          (vreset! v (.-data node)))))
    @v))
```

`forEachNode` delegates to `beans.rowModel` with no client-side guard, so infinite/SSRM grids are checked once a block lands. Nil sample → silent.

### What is checked

| Field string | Checked when | Looked-up key |
|---|---|---|
| `field` | no `valueGetter` on the merged def | `(.isFieldContainsDots col)` → first dot segment; else the whole string |
| `tooltipField` | always, if present | `(.isTooltipFieldContainsDots col)` → same rule |

`tooltipField` is never skipped: v36's cell tooltip resolver reads `data[tooltipField]` *before* consulting `tooltipValueGetter`, so it stays live regardless of either getter. Dot handling asks the `Column` rather than splitting blindly, so `:suppress-field-dot-notation` is honoured for free.

Presence is `js-in` (prototype chain — permissive, so class instances with prototype getters stay quiet). Suggestions come from `js-keys` (own enumerable only, so `"toString"` is never suggested). Bail silently unless the sample is a JS object — `in` throws on primitives.

### State and dedup

One atom per grid, in the listener closure, holding the set of **resolved** field strings (verdict reached: warned or found present). Warning fires on the transition into the set. Short-circuit: every current field already resolved → return before touching the row model, so the steady state is a set-membership test rather than an untruncated `forEachNode` traversal.

Not the module-global `warned` atom: the "present in this grid's rows" half is inherently per-grid, a global set would silence a real bug on a second grid with a different row shape, and `defonce` survives hot reload.

### Messages

```
[ag-grid-cljs] column field "fristName" is not a key in the row data — did you mean "firstName"?
[ag-grid-cljs] column tooltip field "sallary" is not a key in the row data — did you mean "salary"?
[ag-grid-cljs] column field "xyz" is not a key in the row data
```

Camel strings both sides — no `camel->kebab` back-translation. The emitted string is what AG Grid is looking up and failing to find. No list of the row's actual keys on the no-suggestion path (unbounded); revisit with a cap if the bare message proves thin.

Extract a string-level `closest [input candidates]` from the current `suggest`, and let `suggest` delegate — same signature, no caller moves, one copy of the `(max 2 (quot (count input) 3))` threshold.

### Tests (ADR 0015 split)

Node — our contract, given AG Grid's answers:

- `check-fields! [state targets row]` (pure): absent warns with suggestion; no near match → bare message; present silent; `nil` value counts present; tooltip wording; non-object row bails; two runs → one warning.
- `field-targets` over fake `Column`s: `valueGetter` drops `field` but keeps `tooltipField`; `isFieldContainsDots` true → first segment, false → whole string.
- `first-row` over a fake api: skips group nodes, skips nil `data`, nil when nothing yields.
- `install-field-check!` over a fake api: exactly two listeners registered, one immediate run, no `enable-dev-validations!` needed.

Browser — assertions about AG Grid, not about us:

- A column group with a mistyped child field warns (proves `getColumns()` flattens `children`).
- Warnings appear at creation, after `update-grid! {:column-defs …}`, and after a `transact!` into an initially-empty grid.

AC 12 is manual: grep the `:advanced` build for `is not a key in the row data`.

### Accepted costs

- A field resolved as *present* is never re-checked; a later `set-rows!` with a differently-shaped row will not warn.
- `newColumnsLoaded` fires on sort and resize too; absorbed by the short-circuit.
- Synthesized columns are checked when they carry a field.
- The browser tripwire only fails on `console.error`/`pageerror`, so it does not police these warnings. Widening it is a separate ticket.
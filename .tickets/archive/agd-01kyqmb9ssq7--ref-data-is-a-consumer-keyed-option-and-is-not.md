---
id: agd-01kyqmb9ssq7
title: '`:ref-data` is a consumer-keyed option and is in none of the documented sets — a keyword key blanks the cell'
status: closed
type: bug
priority: 2
mode: afk
created: '2026-07-29T19:06:45.433122000Z'
updated: '2026-07-29T23:31:50.648909873Z'
closed: '2026-07-29T23:31:50.648909873Z'
tags:
- conversion
- dx
- docs
acceptance:
- title: A dev grid with a :ref-data column whose keys camelize away from the row's value spelling warns once, naming the property AG Grid looked up and suggesting the near-matching key the consumer should have written; the same grid under the camel row recipe warns nothing
  done: true
- title: 'The check is silent in every false-positive case: no near-match among the :ref-data keys, a :value-formatter on the column, a :value-getter, a field the field check already reported absent, and no rows loaded'
  done: true
- title: Warning period is once per grid, not once per process (ADR 0022 §1) — the relationship is between this column's :ref-data and this grid's rows, so a second grid with differently-spelled rows still warns
  done: true
- title: :ref-data is a member of the consumer-keyed family in all three enumerations — ADR 0019's table, the table in docs/options-and-conversion.md, and CONTEXT.md's glossary — with row-data values named as its citation site and its prescription stated recipe-relative rather than "strings on both sides"; the docs dev-warnings list gains the nudge and its always-on, registry-free standing
  done: true
- title: 'ADR 0019 is amended where this finding contradicts it: §4''s reachable/unreachable split gains the third case (unreachable from the options map, reachable from the live grid), and Considered options'' rejection of keyword-value detection is narrowed to AG Grid-vocabulary positions'
  done: true
- title: :values (:cell-editor-params, :filter-params) is recorded as docs-only — one line in docs/options-and-conversion.md, reason recorded, no check shipped
  done: true
- title: 'Tests follow ADR 0015: node owns the condition, each silence case, the per-grid period and the suggestion; one browser assertion covers the AG Grid premise that an unmatched :ref-data key renders the cell blank. Dead-code elimination verified in the :advanced build'
  done: true
---

## What to build

`:ref-data` (ColDef) satisfies both clauses of ADR 0019 §1's test and appears in
none of the three places that enumerate the consumer-keyed family. It is a
seventh member and the one with the worst failure mode: `:ref-data
{:in-progress "In Progress"}` emits the property `inProgress`, and a row holding
`"in-progress"` — the spelling bare `clj->js` produces, and the pairing the
literal-kebab row recipe commits a consumer to — misses the lookup. AG Grid
resolves `refData[value] || ""`, so the cell renders **blank**. Class rules lose
their styling; this loses the content. `:ref-data` is registry-known, so nothing
else flags it: it converts silently and validates clean.

Ship the whole path for this one option: a dev diagnostic, the vocabulary, the
record, the consumer docs, and tests.

**The diagnostic is a cross-reference, not the keyword-key heuristic.** ADR 0019
§4 splits by whether the citation is reachable, and rates cross-reference
strictly better where it is. `:ref-data`'s citation is the row value —
unreachable from the options map (rows leave it at creation, ADR 0004), but
*reachable from the live grid*, which is exactly what the field check already
does: it holds the live columns and samples a loaded row. So the check compares
this column's sampled value against the keys of its own `:ref-data` map, and
warns only on a **near-match** — ADR 0019 §6's rule, which is what keeps it
registry-free and free of the false positive the class-rules check had to accept:

| Row value | `:ref-data` keys | AG Grid looks up | Cell | Warn? |
| --- | --- | --- | --- | --- |
| `"inProgress"` (camel recipe) | `{:in-progress …}` | `inProgress` | renders | no |
| `"in-progress"` (kebab recipe) | `{:in-progress …}` | `inProgress` | **blank** | yes, suggest `"in-progress"` |
| `"in-progress"` (kebab recipe) | `{"in-progress" …}` | `in-progress` | renders | no |
| anything with no near-match | sparse by intent | — | blank | no |

This refutes rather than accepts the camel-row-recipe false positive this ticket
was filed to adjudicate: correct code under either documented recipe stays
silent, with no need for a signal distinguishing which recipe is in force.

Two supersession rules keep it honest, both mirroring skips the field check
already makes: a `:value-formatter` on the column means AG Grid never consults
`:ref-data` at all, and a `:value-getter` means the emitted field is not where
the value comes from.

**The prescription is recipe-relative, and that is new.** ADR 0019 §8 prescribes
strings on both sides for the reference-cited members. That is not the fix here —
`:ref-data` keys must match the exact spelling the *rows* carry, which under the
camel recipe is camel. The family's "Cited from" column gains a third kind of
citation site: row-data values.

**`:values` is decided as docs-only.** `{:cell-editor-params {:values [:pending
:in-progress]}}` and the Set Filter equivalent camelize keyword elements the same
way, so a select editor writes `"inProgress"` into a column whose rows hold
`"in-progress"`. ADR 0019's Considered options rejected detecting keyword values
as undecidable on evidence drawn entirely from positions holding **AG Grid
vocabulary** (`:cell-data-type :date-string`, `:type :right-aligned`,
`:cell-renderer :ag-group-cell-renderer`); `:values` holds nothing but the
consumer's own cell values, so the rejection reads broader than its evidence
supports. Narrow it in the record, state the rule in docs, ship no second check —
the ref-data check's shape is the template if consumer evidence ever asks for one.

**Not requested: a builder.** `:ref-data` makes the label mapping plain data, so
a `valueFormatter` closure is unnecessary — and would fail ADR 0021 anyway (a
fresh fn per call inside `:column-defs`, churning `columnDefs` on every rebuild).
What remained of the `as-select` shape this was found judging was `mapv name`
over `:values` plus stringifying the label keys: coercion whose only purpose is
dodging the camelization above. It also bundled a Community editor
(`agSelectCellEditor`) with an Enterprise Set Filter (`agSetColumnFilter`) under
one unannotated name, and had zero call sites, against ADR 0009's post-v1
deferral of per-column builders. If a coercing `col`-level builder is ever
wanted, it belongs with the coercion question ADR 0019 §7 parked, not here.

## Blocked by

None - can start immediately.

## Notes

**2026-07-29T23:31:50.648909873Z**

A :ref-data column's sampled row value is cross-referenced against that column's own emitted refData keys, warning only on a near-match — registry-free, always on, once per grid per column, sharing the field check's plumbing with its own state keyed on col id. Prescribes a spelling only where the nearest key is the value's camelization; a plain typo could sit on either side. :values recorded docs-only; ADR 0019 gains the family's seventh member, §4's third case, §9-10 and the narrowed keyword-value rejection; ADR 0017 §7 scoped to presence tests; ADR 0022 §1's roster corrected.

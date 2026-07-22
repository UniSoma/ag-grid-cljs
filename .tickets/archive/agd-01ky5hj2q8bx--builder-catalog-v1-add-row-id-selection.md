---
id: agd-01ky5hj2q8bx
title: 'Builder catalog v1: add row-id/selection/pagination/infinite-datasource, drop default-col-def'
status: closed
type: feature
priority: 2
mode: afk
created: '2026-07-22T18:31:42.056744351Z'
updated: '2026-07-22T20:31:17.831855439Z'
closed: '2026-07-22T20:31:17.831855439Z'
acceptance:
- title: with-default-col-def removed and any usages updated
  done: false
- title: four new builders implemented as pure opts->opts fns with node tests
  done: false
- title: docstrings state coerce/bundle rationale, EDN shape, a threaded example, AG Grid keys written, version constraints
  done: false
- title: npm test green
  done: false
---

## Description

Complete the locked 8-entry curated builder catalog (ADR 0009). Remove with-default-col-def (fails the coerce-or-bundle admission bar). Add: with-row-id (keyword-or-fn -> getRowId callback, string-coerced; load-bearing for set-rows!/transact! diffing), with-selection (friendly shape -> v32.2+ rowSelection object: mode/checkboxes/header-checkbox/enable-click-selection), with-pagination (pagination + page-size + page-size-selector, encoding the auto-page-size x page-size mutual exclusion), with-infinite-datasource (Community Infinite Row Model: row-model-type + cache sizing + datasource whose getRows marshalling follows the ADR 0010 callback contract). All pure opts->opts, opts-first, ->-threadable, bang-free. Docstring contracts per ADR 0014 stating what each coerces/bundles, keys written, version/Enterprise constraints.

## Notes

**2026-07-22T20:31:17.831855439Z**

Dropped with-default-col-def (updated 3 dev-app usages to plain assoc; :default-col-def key stays valid). Added 4 pure opts->opts builders per ADR 0009 with ADR-0014 docstrings: with-row-id (keyword-or-fn -> string-coerced getRowId; keyword path raw for the per-row hot path), with-selection (friendly shape -> v32.2 rowSelection object, mode coerced), with-pagination (bundles pagination + page sizing, encodes auto-page-size x page-size mutual exclusion with dev-warn), with-infinite-datasource (bundles row-model-type + datasource + cache; getRows follows ADR 0010 callback contract). 7 node tests added; npm test green (44 tests / 124 assertions, 0 failures). Two-axis code-review clean. Commit aed5401.

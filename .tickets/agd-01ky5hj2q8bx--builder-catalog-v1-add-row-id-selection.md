---
id: agd-01ky5hj2q8bx
title: 'Builder catalog v1: add row-id/selection/pagination/infinite-datasource, drop default-col-def'
status: open
type: feature
priority: 2
mode: afk
created: '2026-07-22T18:31:42.056744351Z'
updated: '2026-07-22T18:31:42.056744351Z'
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

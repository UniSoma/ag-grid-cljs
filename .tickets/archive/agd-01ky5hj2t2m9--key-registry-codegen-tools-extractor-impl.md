---
id: agd-01ky5hj2t2m9
title: 'Key registry codegen: tools/ extractor, impl.registry literal, generated options reference'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-22T18:31:42.146536103Z'
updated: '2026-07-22T19:11:39.925049996Z'
closed: '2026-07-22T19:11:39.925049996Z'
acceptance:
- title: tools/ codegen runs against the installed ag-grid-community pin and is deterministic/idempotent
  done: true
- title: impl.registry committed with the full rich shape including :initial? and :ag-grid-version
  done: true
- title: docs/reference/ag-grid-options.md committed with the generated header and ADR 0007 §6 layout
  done: true
- title: registry def referenced only from goog.DEBUG-guarded code (DCE precondition)
  done: true
---

## Description

Build the manual Node codegen tool under tools/ (ADR 0007): GridOptions keys and event names extracted from ag-grid-community runtime constants; ColDef via a small ts-morph pass (pin typescript@5.x). It emits two committed artifacts: (a) ag-grid-cljs.impl.registry — a goog.DEBUG-guarded CLJS source literal with the full rich per-key shape {:camel :type :default :initial? :deprecated :doc}, blocks :grid-options/:col-def/:col-group-def/:events, top-level :ag-grid-version stamp; (b) docs/reference/ag-grid-options.md — H2 sections (Grid Options / Column Definitions / Column Groups / Events), columns kebab|camelCase|type|init-only?|deprecated->replacement|description, with a machine-generated header (version stamp, edit-the-tool notice, AG Grid docs pointer). Run on AG Grid dependency bump only; codegen stays out of CI. The registry is dev-warnings + docs authority only — runtime conversion never consults it.

## Notes

**2026-07-22T19:11:39.925049996Z**

Manual Node codegen under tools/codegen (extract.mjs): GridOptions keys + events + handler map from ag-grid-community runtime constants; ColDef/ColGroupDef keys + per-key type/JSDoc metadata via a ts-morph pass (ts-morph 25.0.1 pinned, embeds TS 5.7.3). Emits two committed artifacts for ag-grid-community 36.0.2: (a) ag-grid-cljs.impl.registry — a (when ^boolean goog.DEBUG {...}) literal, blocks :grid-options(351)/:col-def(153)/:col-group-def(27)/:events(111), rich per-key shape {:camel :type :default :initial? :deprecated :doc}, top-level :ag-grid-version; (b) docs/reference/ag-grid-options.md with generated header + ADR 0007 §6 layout. Sorted by locale-independent code-unit order — deterministic/idempotent. DCE precondition verified on a throwaway release build: 417-byte bundle, zero registry data. Codegen out of CI. All 18 tests green.

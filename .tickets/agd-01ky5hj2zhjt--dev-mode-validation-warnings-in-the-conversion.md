---
id: agd-01ky5hj2zhjt
title: Dev-mode validation warnings in the conversion walk
status: open
type: feature
priority: 2
mode: afk
created: '2026-07-22T18:31:42.319420874Z'
updated: '2026-07-22T18:31:42.319420874Z'
acceptance:
- title: typo'd top-level and ColDef keys warn with did-you-mean in dev; opaque positions never warn
  done: false
- title: deprecation warnings carry the replacement; warnings dedup per [object-name key]
  done: false
- title: validation never rejects or alters conversion output; node tests cover all branches
  done: false
- title: 'production DCE verified: registry data + validation code absent from an :advanced goog.DEBUG=false build'
  done: false
deps:
- agd-01ky5hj2t2m9
---

## Description

Wire registry-powered dev validation into the conversion boundary (ADR 0007 §4-5). Position-aware: top-level options validate against :grid-options; known ColDef-bearing positions (:column-defs items, :default-col-def, :auto-group-column-def) against :col-def; everything else is opaque and never validated. Unknown key -> console.warn with kebab did-you-mean; deprecated key -> warn with replacement note; warn-never-reject (ADR 0002). Dedup once per [object-name key] via a dev-only warned-set atom. Ship the enable-dev-validations! helper and document registering AG Grid ValidationModule in dev for type/dependency/row-model checks. Then retire the DCE risk named by ADR 0007 §1: verify with a :pseudo-names / bundle-size check that :advanced + goog.DEBUG false eliminates both the registry literal and the validation code.

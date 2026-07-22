---
id: agd-01ky5hj2zhjt
title: Dev-mode validation warnings in the conversion walk
status: closed
type: feature
priority: 2
mode: afk
created: '2026-07-22T18:31:42.319420874Z'
updated: '2026-07-22T19:34:34.860925792Z'
closed: '2026-07-22T19:34:34.860925792Z'
acceptance:
- title: typo'd top-level and ColDef keys warn with did-you-mean in dev; opaque positions never warn
  done: true
- title: deprecation warnings carry the replacement; warnings dedup per [object-name key]
  done: true
- title: validation never rejects or alters conversion output; node tests cover all branches
  done: true
- title: 'production DCE verified: registry data + validation code absent from an :advanced goog.DEBUG=false build'
  done: true
deps:
- agd-01ky5hj2t2m9
---

## Description

Wire registry-powered dev validation into the conversion boundary (ADR 0007 §4-5). Position-aware: top-level options validate against :grid-options; known ColDef-bearing positions (:column-defs items, :default-col-def, :auto-group-column-def) against :col-def; everything else is opaque and never validated. Unknown key -> console.warn with kebab did-you-mean; deprecated key -> warn with replacement note; warn-never-reject (ADR 0002). Dedup once per [object-name key] via a dev-only warned-set atom. Ship the enable-dev-validations! helper and document registering AG Grid ValidationModule in dev for type/dependency/row-model checks. Then retire the DCE risk named by ADR 0007 §1: verify with a :pseudo-names / bundle-size check that :advanced + goog.DEBUG false eliminates both the registry literal and the validation code.

## Notes

**2026-07-22T19:20:14.627724978Z**

DCE criterion recipe (verified against the committed registry, agd-01ky5hj2t2m9): add a throwaway :browser build with {:closure-defines {goog.DEBUG false}} whose init-fn references reg/registry ONLY inside a (when ^boolean goog.DEBUG ...) guard, 'shadow-cljs release' it, then grep the output bundle for a unique registry string (e.g. a docstring fragment) and the :ag-grid-version stamp — both must be absent. Baseline already measured: with just the registry reference (no validation code yet) the release bundle was 417 bytes with zero registry data, while a dev compile contained it — so the guard collapses as ADR 0007 §1 intends. This ticket extends that to also assert the validation code is eliminated. Note: the check needs a JVM/Closure release build, heavier than the pure-Node CI (ADR 0015), so it's a documented manual/local verification, not a CI gate.

**2026-07-22T19:34:34.860925792Z**

Position-aware dev-only validation in the conversion boundary (ADR 0007 §4-5). impl.validate: top-level -> :grid-options + event handlers; :column-defs/:default-col-def/:auto-group-column-def -> :col-def; groups (:children) -> :col-group-def and recurse; all else opaque. Unknown key -> console.warn with kebab did-you-mean (length-scaled Levenshtein); deprecated -> warn with replacement; deduped per [object-name key] via dev-only warned-set atom; camel-normalized so kebab and already-camel match. core/enable-dev-validations! (off by default) + documents AG Grid ValidationModule for type/dep/row-model checks. Every registry ref under ^boolean goog.DEBUG; create-grid! call site guarded (load-bearing for DCE). Production DCE verified via throwaway :advanced {goog.DEBUG false} release build; probe removed (manual/local, not CI per ADR 0015). 18 node tests cover all branches. /code-review found+fixed the event-handler did-you-mean bug and strengthened the AC3 test to deep equality.

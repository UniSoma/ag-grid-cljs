---
id: agd-01ky0edx8dzc
title: 'Options-diffing semantics: what is updatable and how changes apply'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:47.500211676Z'
updated: '2026-07-21T22:47:49.805472234Z'
closed: '2026-07-21T22:47:49.805472234Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
deps:
- agd-01ky0ed8766f
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

Define the declarative update contract: which grid options the differ applies via setGridOption vs which are initial-only (and what happens when an initial-only option changes — warn? recreate grid?); diff granularity for nested structures (columnDefs identity); and how the explicit data channel and the differ stay out of each other's way (row-data key ignored by differ?). Informed by the Fulcro skeleton's update behavior.

## Notes

**2026-07-21T02:24:04.180508079Z**

Inputs from the Fulcro skeleton (agd-01ky0ed8766f, closed): (1) set-rows! full swap with :get-row-id is state-preserving (scroll/selection/focus verified headless) — AG Grid already does row-data diffing internally, so the wrapper's diffing question is mainly about non-data OPTIONS; (2) dual bookkeeping in consumers is eliminable via set-rows!-from-source-of-truth; a generic auto-diff watcher (normalized-table watch -> transactions) was judged Fulcro-flavored and deferred HERE as a possible optional adapter; (3) the guide's D3 pattern shows where a props-driven declarative layer would hook in (sCU as props-changed callback) — we bypassed it with the explicit channel.

**2026-07-21T02:36:45.137279860Z**

Side research relevant to this ticket: docs/research/ag-grid-react-wrapper.md §1.2 (prop→grid bridge). Sourced findings on what ag-grid-react's re-apply-every-prop bridge breaks: new rowData/columnDefs references per render trigger extra grid renders and can reset grid state (selection, column order/width) — with the nuance that column state *may* survive when colId/field still match (docs hedge with 'may', not a guaranteed full reset). Also §4: hold gridOptions/callbacks stable across host re-renders or the grid resets internal state; stale-closure hazard on callbacks. Direct evidence for deciding what is updatable via setGridOption diffing vs what must go through the transactional channel.

**2026-07-21T22:47:43.010367386Z**

**2026-07-21T22:47:49.805472234Z**

Options-diffing contract locked: create-grid! returns a GridHandle {:api :opts}; new update-grid! [handle new-opts] is a PATCH/MERGE differ compared by = uniformly (no function special-case), per top-level key, applying setGridOption only for present+changed keys (absent = leave-as-is, stash accumulates via merge). Registry :initial? is the sole classifier; initial-only change = warn-once+ignore (recreate deferred); :row-data ignored (owned by explicit channel); unclassified changed keys applied optimistically+warn; :column-defs whole-value handoff, AG Grid owns column diffing. Full-state + Reagent/UIx declarative layer punted to agd-01ky157wpbpv.

## Resolution: Options-diffing contract

The declarative half of the locked update model (differ + explicit data channel + raw API). Lives in **core**. Grilled through in one HITL session; contract confirmed.

### Entry point & state
- `create-grid!` returns a **`GridHandle`** record `{:api <GridApi> :opts <last-applied-EDN>}` (was: bare GridApi — implied implementation drift from the skeleton).
- All wrapper fns take the handle (`set-rows!`/`transact!`/`destroy!`/`update-grid!`); **`grid-api`** accessor exposes the raw GridApi (raw-API escape hatch).
- New fn `update-grid! [handle new-opts]`: diffs `new-opts` vs stashed `:opts`, applies changes, returns handle with updated stash.

### Classification
- Registry `:initial?` is the SOLE, trusted classifier: `false` -> updatable via setGridOption; `true` -> initial-only. No hand-maintained override list (rejected: re-introduces the maintenance treadmill). Mis-annotations get fixed in codegen, not the differ.

### Diff mechanism — PATCH/MERGE, per top-level key, compared by `=` uniformly (no function special-case)
1. iterate keys PRESENT in new-opts;
2. `setGridOption(camelKey, newVal)` where `(not= old new)`;
3. ABSENT keys -> no call, grid keeps current value ("leave as-is" = differ emits no setGridOption; AG Grid never re-consumes a whole opts map after creation);
4. `:column-defs` ships the WHOLE new value; AG Grid owns column-level diffing.
- Stash after update = MERGE(old stash, present new keys) -> always reflects grid's actual applied state; keeps later diffs accurate/minimal.

### Behavior branches
- Initial-only key changed: dev-warn ONCE PER KEY (dedup set on handle, mirrors registry validation dedup) + ignore. Opt-in recreate (`{:on-initial-change :recreate}`) DEFERRED, not built — recreate is destructive of scroll/selection/focus/column state, contradicting the state-preservation premise.
- Data keys: differ IGNORES `:row-data` (owned by set-rows!/transact!); dev-warn if it changed. `:column-defs` stays with the differ (config, not data).
- Unclassified changed key (not in registry :grid-options — newer than pin, or typo): apply optimistically via setGridOption + dev-warn; never a hard block (consistent with "warn, never reject"; real typos already caught by conversion-time did-you-mean).
- Callbacks: updatable where `:initial? false`; latest closure wins via `=`. The React "mount-once" stale-closure trap does NOT reproduce here — consumer re-supplies opts (with fresh closures) each update, and `=` detects the new fn object and applies it.

### Why `=` uniformly beat "always re-apply function options"
Enumerated every consumer pattern (fresh inline lambda / stable ref / reused map): under both policies the grid ends up holding the SAME function object in every case. "Always re-apply" buys zero correctness, only redundant setGridOption calls. So: one comparison rule for the whole differ, no `fn?`-detection branch.

### Documented consequences (guidance, not differ logic)
- Column-state preservation across a `:column-defs` update is AG Grid's "may, not guaranteed" -> pin `colId` (mirrors `:get-row-id` for rows).
- Inline function values NESTED inside `:column-defs` make its structural `=` always-dirty -> full re-apply -> possible column-state reset. Use name-registered renderers / stable refs (ties to the renderer ticket's name-registration tier).
- Memoized-stale callbacks (stable ref over changed captured state) are unfixable by any differ policy -> consumer reads live state at call time.
- `update-grid!` is a PATCH op. True declarative full-state (absent = revert-to-default) is a separate opt-in built on top; belongs to the Reagent/UIx adapters ticket (agd-01ky157wpbpv), not here. Full-state was rejected as the default: needs a trustworthy registry `:default` for every key (drifts across AG Grid versions -> silent wrong reverts), and punishes partial/builder-produced opts maps.

### Inputs consumed
- Fulcro skeleton (agd-01ky0ed8766f): AG Grid diffs row-data internally -> differ is about non-data OPTIONS; explicit channel already preserves state.
- Key registry (agd-01ky0evm9nap): `:initial?` per-key flag, built to feed exactly this.
- Side research docs/research/ag-grid-react-wrapper.md §1.2 (prop->grid bridge resets state) and §4 (callback stability / stale-closure) — motivated the patch-not-full-state and `=`-not-always-reapply calls.

No new tickets, no fog graduation: this ticket consumed inputs rather than producing them.

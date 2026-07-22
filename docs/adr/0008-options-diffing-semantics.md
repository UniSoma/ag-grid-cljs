# 0008. Options-diffing semantics: what is updatable and how changes apply

- Status: accepted, 2026-07-21
- Origin: knot ticket agd-01ky0edx8dzc (tickets are ephemeral; this record is self-contained)

`create-grid!` returns a `GridHandle` `{:api :opts}`, and a new `update-grid! [handle new-opts]` is a PATCH/MERGE differ: per top-level key present in `new-opts`, compared by `=` uniformly, applying `setGridOption` only for changed keys. The registry's `:initial?` flag (ADR 0007) is the sole classifier of what is updatable; `:row-data` belongs to the explicit data channel; `:column-defs` is handed to AG Grid whole.

## Context

This is the declarative half of the locked update model (ADR 0004: differ + explicit data channel + raw API). It lives in **core**. The question: which grid options the differ applies via `setGridOption` vs which are initial-only (and what happens when an initial-only option changes — warn? recreate grid?); diff granularity for nested structures (columnDefs identity); and how the explicit data channel and the differ stay out of each other's way.

Inputs consumed:

- **Fulcro walking skeleton** (ticket agd-01ky0ed8766f): `set-rows!` full swap with `:get-row-id` is state-preserving (scroll/selection/focus verified headless) — AG Grid already does row-data diffing internally, so the wrapper's diffing question is about non-data OPTIONS. Dual bookkeeping in consumers is eliminable via set-rows!-from-source-of-truth; a generic auto-diff watcher (normalized-table watch -> transactions) was judged Fulcro-flavored and deferred.
- **Key registry** (ADR 0007): the `:initial?` per-key flag, built to feed exactly this decision.
- **Side research** docs/research/ag-grid-react-wrapper.md §1.2 (prop->grid bridge): ag-grid-react's re-apply-every-prop bridge means new rowData/columnDefs references per render trigger extra grid renders and can reset grid state (selection, column order/width) — with the nuance that column state *may* survive when colId/field still match (the docs hedge with "may", not a guaranteed full reset). And §4: gridOptions/callbacks must be held stable across host re-renders or the grid resets internal state; stale-closure hazard on callbacks. Together these motivated the patch-not-full-state and `=`-not-always-reapply calls.

## Decision

### Entry point and state

- `create-grid!` returns a **`GridHandle`** record `{:api <GridApi> :opts <last-applied-EDN>}` (was: bare GridApi — implied implementation drift from the skeleton).
- All wrapper fns take the handle (`set-rows!` / `transact!` / `destroy!` / `update-grid!`); a **`grid-api`** accessor exposes the raw GridApi (the raw-API escape hatch, ADR 0004).
- New fn `update-grid! [handle new-opts]`: diffs `new-opts` vs the stashed `:opts`, applies changes, returns the handle with updated stash.

### Classification

Registry `:initial?` (ADR 0007) is the SOLE, trusted classifier: `false` -> updatable via `setGridOption`; `true` -> initial-only. No hand-maintained override list (rejected: re-introduces the maintenance treadmill). Mis-annotations get fixed in codegen, not in the differ.

### Diff mechanism — PATCH/MERGE, per top-level key, compared by `=` uniformly (no function special-case)

1. Iterate keys PRESENT in `new-opts`.
2. `setGridOption(camelKey, newVal)` where `(not= old new)`.
3. ABSENT keys -> no call; the grid keeps its current value ("leave as-is" = the differ emits no `setGridOption`; AG Grid never re-consumes a whole opts map after creation).
4. `:column-defs` ships the WHOLE new value; AG Grid owns column-level diffing.

The stash after an update is MERGE(old stash, present new keys) -> it always reflects the grid's actual applied state, keeping later diffs accurate and minimal.

### Behavior branches

- **Initial-only key changed**: dev-warn ONCE PER KEY (dedup set on the handle, mirroring the registry validation dedup of ADR 0007) + ignore. An opt-in recreate (`{:on-initial-change :recreate}`) was DEFERRED, not built — recreate is destructive of scroll/selection/focus/column state, contradicting the state-preservation premise.
- **Data keys**: the differ IGNORES `:row-data` (owned by `set-rows!`/`transact!`, ADR 0004); dev-warn if it changed. `:column-defs` stays with the differ (config, not data).
- **Unclassified changed key** (not in registry `:grid-options` — newer than the pin, or a typo): apply optimistically via `setGridOption` + dev-warn; never a hard block (consistent with "warn, never reject", ADR 0002; real typos are already caught by conversion-time did-you-mean, ADR 0007).
- **Callbacks**: updatable where `:initial? false`; latest closure wins via `=`. The React "mount-once" stale-closure trap does NOT reproduce here — the consumer re-supplies opts (with fresh closures) each update, and `=` detects the new fn object and applies it. (Event-callback shape is ADR 0010.)

### Why `=` uniformly beat "always re-apply function options"

Enumerating every consumer pattern (fresh inline lambda / stable ref / reused map): under both policies the grid ends up holding the SAME function object in every case. "Always re-apply" buys zero correctness, only redundant `setGridOption` calls. So: one comparison rule for the whole differ, no `fn?`-detection branch.

## Consequences

Documented as guidance, not differ logic:

- Column-state preservation across a `:column-defs` update is AG Grid's "may, not guaranteed" -> pin `colId` (mirrors `:get-row-id` for rows).
- Inline function values NESTED inside `:column-defs` make its structural `=` always-dirty -> full re-apply -> possible column-state reset. Use name-registered renderers / stable refs (ties to the renderer name-registration tier).
- Memoized-stale callbacks (a stable fn ref closing over changed captured state) are unfixable by any differ policy -> the consumer must read live state at call time.
- `update-grid!` is a PATCH op. True declarative full-state (absent = revert-to-default) would be a separate opt-in built on top; it was punted to the Reagent/UIx adapters question, and ADR 0012 subsequently ruled framework adapters out of v1 entirely.

## Considered options

- **Full-state declarative semantics as the default** (absent key = revert-to-default) — rejected: needs a trustworthy registry `:default` for every key (drifts across AG Grid versions -> silent wrong reverts), and punishes partial/builder-produced opts maps.
- **"Always re-apply function options" instead of `=` comparison** — rejected: under every consumer pattern the grid holds the same function object either way; the policy buys zero correctness at the cost of redundant `setGridOption` calls and a special-case branch.
- **Hand-maintained updatable/initial-only override list** — rejected: re-introduces the maintenance treadmill the generated registry exists to avoid.
- **Opt-in grid recreate on initial-only change** — deferred, not built: destructive of scroll/selection/focus/column state.
- **Generic auto-diff watcher** (normalized-table watch -> transactions) — judged Fulcro-flavored and deferred as a possible optional adapter.

## References

- ADR 0002 — layered API shape (warn-never-reject policy)
- ADR 0004 — update model (differ + explicit data channel + raw API)
- ADR 0005 — conversion boundary (kebab->camel of keys/values the differ passes)
- ADR 0007 — key registry (`:initial?` classifier, warning dedup pattern)
- ADR 0010 — event-callback shape
- ADR 0012 — no framework adapters in v1 (home the full-state layer was punted to, then ruled out)
- docs/research/ag-grid-react-wrapper.md §1.2, §4 — evidence against re-apply-every-prop and for callback-stability guidance

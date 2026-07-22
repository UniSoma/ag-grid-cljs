# 0012. No framework adapters in v1: Reagent/UIx compose through public seams

- Status: accepted, 2026-07-22
- Origin: knot ticket agd-01ky157wpbpv (tickets are ephemeral; this record is self-contained)

Framework adapters are ruled out of scope for v1 — no Reagent/UIx cell-renderer sugar and no reactive/full-state-declarative layer; the core stays framework-agnostic. A future adapter effort wraps three already-public seams, so leaving the door open costs zero forward-compat work. The v1 deliverable is one prose recipe page, not a code namespace.

## Context

`react/react-renderer` (ADR 0011) already accepts any React element, so Reagent (`r/as-element`) and UIx (`$`) components mount today via one line of user composition. The open question was whether the library should ship thin adapter sugar (e.g. `reagent-renderer` / `uix-renderer` namespaces with their own optional deps), document the composition recipe only, or rule adapters out of scope.

Facts scoping any React-facing adapter (docs/research/ag-grid-react-wrapper.md §2–§3, §5; analysis pinned to ag-grid-react ~v31–32 source, current through 35.x):

- ReactUI mounts all custom components inside AG Grid's own *private* React root (`agGridReactUi.tsx` renders one `<GridComp>`; no `createRoot` per cell, no public injection point) — an adapter cannot reproduce ReactUI-style direct mounting.
- React issue #26281: a `createRoot()` root nested inside another root's tree is NOT unmounted with the parent and its effect cleanups don't run — each nested root needs explicit `root.unmount()`; amplified by cell virtualization churn.
- The proven Reagent/re-frame embedding is the two-component split with a static mount div (Day8 "Using Stateful JS Components") — which is what a Reagent adapter would package.

From the options-diffing resolution (ADR 0008): `update-grid!` in core is deliberately a PATCH op (absent key = leave-as-is, never revert-to-default). TRUE declarative full-state — where the opts map is the complete desired state and dropped keys revert to default — was rejected as a core default (fragile registry `:default` dependency across AG Grid versions; punishes partial/builder opts maps). If a reactive adapter wants full-state declarative semantics, that opt-in layer is built on top of `update-grid!`, not in core. Core owns the diff (the `GridHandle` stash), so an adapter wraps `update-grid!` rather than re-implementing diffing.

## Decision

Reagent/UIx adapters are ruled OUT of scope for v1 — the library ships no framework adapters (neither Reagent/UIx cell-renderer sugar nor a reactive/full-state-declarative layer); core stays framework-agnostic.

1. **Door already open, zero forward-compat work.** A future adapter effort wraps three already-public seams:
   - `update-grid!` (PATCH differ, ADR 0004/0008) + `GridHandle` `{:api :opts}` for a reactive layer — this resolves the full-state-declarative-layer punt from options-diffing;
   - `react/react-renderer` (per-cell local root, ADR 0011) for React-flavored cell sugar;
   - `raw`/`grid-api` as escape hatch.

2. **Docs deliverable: ONE prose recipe page** hung off the cell-renderer docs (not a code namespace; see ADR 0014 docs-cljdoc-strategy), covering:
   - the Reagent `r/as-element` + UIx `$` one-liner into `react-renderer`;
   - the static-mount-div two-component split for stateful re-frame/Reagent components;
   - the nested-`createRoot` unmount caveat (React issue #26281, amplified by cell virtualization churn);
   - a light pointer naming the reactive seams above.

3. **Fulcro-is-the-bar stays true** — the bar is a proof target, not shipped code; the Fulcro skeleton required zero adapter.

## Consequences

- Consumers of Reagent/UIx get a documented one-liner today, not a dependency; the recipe page is the only maintenance surface.
- Any future full-state-declarative semantics live in an adapter built on `update-grid!` and `GridHandle`; core's PATCH semantics (ADR 0008) are unaffected.

## Considered options

- **Ship thin adapter sugar** (`reagent-renderer` / `uix-renderer` namespaces with their own optional deps) — rejected: the composition is already a one-liner through `react-renderer`, so a namespace adds dependency surface without ergonomic gain.
- **Ship a reactive/full-state-declarative layer in v1** — rejected: full-state semantics were already rejected as a core default (ADR 0008), and a reactive layer composes later over the public `update-grid!`/`GridHandle` seams with no forward-compat work needed now.
- **Reproduce ReactUI-style direct mounting in an adapter** — not viable: AG Grid's React tree is a private root with no public injection point.

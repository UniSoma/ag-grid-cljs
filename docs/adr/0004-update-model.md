# 0004. Update model: options diffing + transactional data channel + raw escape hatch

- Status: accepted, 2026-07-20 (charting), mechanics confirmed 2026-07-21
- Origin: charting session of the wayfinder map plus knot ticket agd-01ky0ed8766f (tickets are ephemeral; this record is self-contained)

The wrapper's update model is three explicit channels: declarative options diffing via `update-grid!`, an explicit transactional data channel via `set-rows!` and `transact!`, and the raw grid API as escape hatch. There is no reactive/auto-watching layer in v1, and row data is deliberately excluded from options diffing.

## Context

AG Grid is an imperative widget: once created, it is updated through its own API, not by re-rendering. A ClojureScript wrapper must decide how consumer state changes reach the grid. Three kinds of change have different characters:

- **Grid options** (columns, features, callbacks) change rarely and coarsely; a declarative diff of the options map fits.
- **Row data** changes often and fine-grainedly; AG Grid has a dedicated, state-preserving transaction API for it, and pushing rows through an options diff would forfeit that.
- **Everything else** — the long tail of imperative grid capabilities — is already well served by AG Grid's own API.

The decision was settled 2026-07-20 during map charting; the data-channel mechanics were proven and refined by the Fulcro walking skeleton (ticket agd-01ky0ed8766f, closed 2026-07-21), which set the bar as: the skeleton grid living inside a Fulcro component (mount-point pattern, no React diffing inside the grid), with row updates flowing through the explicit data channel without losing grid state (scroll, selection, focus).

## Decision

The update model is three explicit channels:

1. **Declarative options diffing — `update-grid!`.** The consumer passes a new options map; the wrapper diffs it against the previous one and applies changes through AG Grid's option-update API. Semantics (what is updatable and how changes apply) are detailed in ADR 0008. Row data is deliberately excluded from this channel.
2. **Explicit transactional data channel — `set-rows!` and `transact!`.** `set-rows!` is a full swap: the consumer hands over the complete row collection and AG Grid diffs it by `:get-row-id`, preserving grid state. `transact!` applies fine-grained `:add` / `:update` / `:remove` transactions mapped to AG Grid's transaction API. (Rows cross the JS boundary by contract per ADR 0003.)
3. **Raw grid API escape hatch — `grid-api` on the GridHandle.** Anything the wrapper does not cover is reachable through AG Grid's own API object.

No reactive/auto-watching layer in v1. The wrapper does not watch atoms, normalized databases, or any other consumer state; updates happen only when the consumer calls one of the channels. This was ruled out together with framework adapters (ADR 0012).

### Mechanics proven by the Fulcro walking skeleton

The Fulcro reference bar was proven with **zero library support** for Fulcro — the three channels are sufficient. These proof-derived decisions are part of the record:

- **Class-based mount-point host.** A class-based `defsc` GridHost — stable ref div, `createGrid` in `componentDidMount`, `destroy!` in `componentWillUnmount`, `shouldComponentUpdate` false — is ~15 lines of consumer code. Grounding research against the Fulcro repo and developers guide confirmed this as the canonical pattern, not legacy: the `defsc` docstring explicitly blesses sCU-forced-false "so that other libraries can control the sub-dom", and the guide's only documented imperative-widget pattern ("Taking control of the sub-DOM (D3, etc)") is class-based and structurally identical. The hooks variant (`:use-hooks?` + memoization) only approximates never-re-render via props-equality memoization — weaker than sCU false. Fulcro db->UI refreshes re-render the page around the grid without touching grid DOM (verified by element identity).
- **The data channel preserves grid state.** Headless-verified: scroll position, single-row selection, and cell focus all survive an `:add` transaction, an `:update` transaction, and a `set-rows!` full swap (AG Grid diffs by `:get-row-id`); the grid never remounts; grid and consumer state stay in sync in both directions.
- **`transact!`-from-a-cell with an explicit app reference is one line and acceptable.** A react-renderer cell lives in a detached local root (no React context, no Fulcro app context — see the cell-renderer decision); its onClick is `(comp/transact! APP [(give-raise {:person/id id})])` with `APP` the defonce'd app — sanctioned by the Fulcro book's own detached-root example. No portal variant (one host root + `createPortal` per cell, ag-grid-react's architecture) is needed on this evidence.
- **Dual bookkeeping is eliminable via `set-rows!`-from-db.** Manually mirroring each mutation as both a db swap and a grid transaction is real but small friction. The default documented consumer pattern eliminates it: mutations swap only the consumer db, then call `set-rows!` with rows derived from the db — single bookkeeping, zero new API, state-preserving as proven. Manual `transact!` remains the fine-grained/performance channel. A generic auto-diff watcher (normalized-table watch -> grid transactions) is possible and cheap over normalized tables, but is framework-flavored machinery and stays out of the core.

The framework-composition docs article (ADR 0014) carries the mount-point host, the `set-rows!`-from-db pattern, and the explicit-app cell `transact!` as consumer guidance.

## Consequences

- Updating row data through `update-grid!` is unsupported by design; consumers must use the data channel, which is what preserves scroll/selection/focus.
- Consumers own the wiring from their state model to the channels; the wrapper never observes consumer state. Framework integration is documentation (ADR 0014), not code (ADR 0012).
- `set-rows!` requires `:get-row-id` for state-preserving diffs; the entity->row derivation function is the consumer's.

# 0023. React renderer render scheduling: batched microtask flush, not per-cell flushSync, not a portal host

- Status: accepted, 2026-08-11
- Origin: knot ticket agd-01kzr7wehb0d, opened on a standalone submission from the reference consumer asking to reopen ADR 0011's deferred portal variant (tickets are ephemeral; this record is self-contained)

`react-renderer` keeps its per-cell React roots but no longer issues a `flushSync` per cell on the caller's stack: cell renders are queued and drained in ONE `flushSync` per microtask. That closes the React DEV `flushSync was called from inside a lifecycle method` error class measured on refresh-from-a-commit, collapses N synchronous render passes into one, and changes the render contract from "content lands on the call stack" to "content lands before paint". The portal-host architecture (one root per grid + `createPortal` per cell) is **closed**, not deferred: the spike's kill rule was that it would be built only if the batched flush failed on DEV errors or empty-cell paint, and it failed neither.

## Context

ADR 0011 committed `react-renderer` to a per-cell `createRoot` with `flushSync` renders in `:init`/`:refresh` (async-by-default createRoot renders would flash empty cells), and deferred a portal variant to a Fulcro ergonomics criterion. That criterion was **met** (ADR 0012: "the Fulcro skeleton required zero adapter"), so the portal door was effectively closed on its original terms. The reference consumer's submission (written against sha 6548da8a, ag-grid-community 35.3.1, react-dom 19.2.7) asked to reopen it on new evidence: React runs passive effects under `CommitContext`, so a React host driving the grid from `useEffect` bodies makes every per-cell `flushSync` a DEV error — claimed at ~1 per live cell per create and per data push, and claimed unfixable per-cell because deferring the flush re-opens the empty-cell flash.

The spike (browser harness: a real React FC whose `useEffect`s create the grid, force-refresh cells, and push rows; 8 renderer cells; headless Chromium; React 19.2.7 DEV) measured the claim before any decision:

| leg driven from a `useEffect` | status quo errors | batched-flush errors |
|---|---|---|
| `create-grid!` | 0 | 0 |
| `refreshCells {:force true}` | 8 (1 per cell) | 0 |
| `set-rows!` | 0 | 0 |

- **The error class is real but narrower than submitted.** Create and `set-rows!` never fired it, under either scheduling: AG Grid builds rows and applies row data asynchronously, so `:init`'s flush was already off the commit stack. The only repro is a synchronous per-cell refresh API (`refreshCells`) called from a commit — which is exactly the documented way a React host forces renderer refreshes.
- **Empty-cell paint is unchanged.** A per-animation-frame monitor counted frames where a renderer cell existed with no text: `{create 1, set-rows 1}` under the status quo (AG Grid's own async cell build — the cell div exists a frame before the renderer's gui attaches), and identically `{create 1, set-rows 1}` under the batched flush. The microtask deferral adds zero flash because a microtask always drains before paint.
- **Measurement APIs:** autoHeight rows settle to content height under both schedulings (62px for 60px content — AG Grid re-measures via ResizeObserver, which fires after microtasks). Column auto-size called on the *same stack* as a `refreshCells` measures the stale content under the batched flush (88px vs the correct 326px); one microtask later it measures correctly. The status quo measured fresh content there. This is the one behavioral regression, recorded below as a caveat.
- The submission's claim that a portal host's batch flush is "schedulable outside any consumer commit" is wrong: `create-grid!` from a `useEffect` puts AG Grid's whole init stack inside the commit, so a portal host escapes the commit the same way the batched flush does — a microtask — or not at all. Its context-transparency claim also does not hold for a *library-created* host root (cells would inherit an empty library-owned tree; real context flow needs new consumer-facing API), and portals shrink the React #26281 unmount discipline from N cells to one host root rather than deleting it.

## Decision

1. **Batched microtask flush.** `react-renderer`'s `:init` and `:refresh` no longer render on the caller's stack. Each queues a render thunk; the first thunk of a tick schedules one microtask that drains the whole queue inside a single `flushSync`. The queue is module-level on purpose: one flush covers every cell of every grid on the page.
2. **The render contract changes: content lands before paint, not on the call stack.** `:init` returns a span that fills at end of microtask; `:refresh` returns true before the re-render lands. Same class of contract change as ADR 0011's deferred-unmount correction, documented in the ns docstring beside it. The browser suite gains an `await-microtask` helper and a contract test pinning both halves (stale on the caller's stack, fresh one microtask later).
3. **A destroyed guard on queued thunks.** `:destroy` marks the cell state destroyed before queueing its (already-deferred, agd-01kzr5qwmfyx) `root.unmount()`; a queued render thunk for a destroyed cell is skipped, so a destroy landing between queue and drain never renders into a root about to unmount.
4. **The portal host is closed.** ADR 0011's "deferred" portal option is resolved: not built. The kill rule — build it only if the batched flush failed the DEV-error or empty-cell-paint criteria — fired in the batched flush's favor on both. Its residual advantages are acknowledged as real and insufficient: one React commit per batch instead of N cell-root commits inside one flushSync, and a #26281 unmount audit of one host root instead of N cell roots. Against them: a per-grid host root and cell registry, a grid-destroy hook the react ns must wire, interplay with ADR 0021's deferred construction (the renderer value must stay `=` while closing over a per-grid host), and no error/flash improvement the measurements could detect.

## Consequences

- A React host can now drive `create-grid!`, `refreshCells`, and `set-rows!` from `useEffect` bodies with zero React DEV errors; before this, `refreshCells` printed one error per live renderer cell per call.
- A screenful of cell updates is one synchronous React render pass instead of N. ADR 0011's "`flushSync` defeats batching" consequence is retired; its scroll-churn caveat (~100–300 root create/destroy per second) still stands — roots are still per-cell.
- **Caveat: same-stack measurement reads stale content.** Code that calls `refreshCells` and a measuring API (`autoSizeAllColumns`) in the same synchronous stack measures the pre-refresh content. The escape is to measure after a microtask (or an animation frame). autoHeight needs no escape — ResizeObserver re-measures after the flush lands.
- Cell content is no longer inspectable on the stack that triggered it; tests await a microtask (`u/await-microtask`) instead of reading back synchronously.
- The shipped teardown fixes (deferred unmount agd-01kzr5qwmfyx, `isDestroyed` guard agd-01kzq9x25fez) stand unchanged; the deferred unmount is now permanent fixture rather than possible scaffolding, since cells remain roots.
- `CONTEXT.md` is unchanged: the *Renderer tiers* entry stays true (a per-cell local React root), and the flush scheduling is an implementation detail beneath the glossary.
- Context opacity of per-cell roots (no provider/error-boundary inheritance from the consumer's tree) remains, as it would have under a library-created portal host; making context flow is a separate, consumer-facing API question this ADR deliberately does not open.

  **Pointer, 2026-08-12 (agd-01kzva6hgxq5, ADR 0024).** That separate question is now opened and answered: ADR 0024 ships a CONSUMER-mounted portal host (`react/portal-host` + `react/portal-renderer`) as a tier beside the per-cell roots. This does not disturb what this ADR closed — the design rejected here was a *library-created* host root, whose context-transparency claim genuinely fails, and this kill rule never measured context inheritance; ADR 0024's did.

## Considered options

- **Status quo (per-cell `flushSync` on the caller's stack)** — rejected: one DEV error per live cell on every `refreshCells` from a commit, and N synchronous render passes where one suffices. The submission was right that consumers cannot fix this from outside.
- **Portal host (one root per grid + `createPortal` per cell, ag-grid-react's architecture)** — rejected via the kill rule above: every measured benefit is delivered by the batched flush at a fraction of the machinery, and the submission's two structural arguments unique to portals (context transparency, deleting the unmount discipline) do not survive scrutiny — the first needs new API, the second is N→1, not N→0.
- **Consumer-side deferral shim** (wrap every grid call from effects in `queueMicrotask`) — rejected: per-consumer boilerplate to fix library-caused noise, and it re-opens the empty-cell flash on the create path for hosts that create grids outside commits.
- **Per-cell microtask flush (defer each cell's own flushSync)** — rejected: fixes the error class but keeps N flushSync passes per batch; the queue that fixes one fixes both.
- **Plain async renders (drop flushSync entirely)** — rejected: createRoot renders flush on React's scheduler, after paint — the empty-cell flash ADR 0011 documented.

## References

- ADR 0011 — renderer tiers (the per-cell-root commitment and the portal deferral this closes; a correction note there records it)
- ADR 0012 — no framework adapters (the Fulcro bar whose deferral criterion was met, and why rebuild-during-render hosts exist)
- ADR 0021 §4 — deferred construction (unchanged by this; the react ns still owns its construction tag and core never requires react)
- ADR 0015 — testing split (why the evidence is a browser-suite harness)
- React issue #26281 — nested roots need explicit unmount (unchanged: cells remain roots)
- src/browser-test/ag_grid_cljs/browser/react_commit_flush_test.cljs — the committed regression guard distilled from the spike harness

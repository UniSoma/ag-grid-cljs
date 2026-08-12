# 0011. Cell renderer tiers: bare fn, lifecycle map, engine-free DOM sugar, per-cell React root

- Status: accepted, 2026-07-21
- Origin: knot ticket agd-01ky0ed8adbf (tickets are ephemeral; this record is self-contained)

The library commits to three custom cell-renderer tiers — bare fn shorthand, `render/renderer` lifecycle map with `render/dom-renderer` sugar, and `react/react-renderer` with a per-cell local React root — none of which owns a DOM-building engine. There is no wrapper-level typed-renderer catalog.

## Context

The walking-skeleton ticket had to prove the cell-renderer ergonomics risk: a helper letting users define custom cell renderers in ClojureScript against AG Grid's vanilla renderer interface (`init`/`getGui`/`refresh`/`destroy`), covering both plain DOM rendering and mounting React components into cells for React-hosted apps.

The skeleton proved three renderer styles against AG Grid v36's vanilla interface and surfaced interop hazards that are locked into the helpers and contract tests:

- AG Grid detects a component class via `prototype && 'getGui' in prototype` — the converter's fn auto-wrap would strip that, so the helpers return a `(raw class)` (see ADR 0005 conversion-boundary for `raw`).
- A JS constructor returning an object hijacks `new` — the constructor must return nil.
- AG Grid `.then`'s any non-null `init` return (its deferred-init protocol) — the `init` wrapper swallows the user fn's return.
- `flushSync` is required when rendering into a per-cell React root: React 18's async-by-default render would flash empty cells.

Post-resolution corroboration from docs/research/ag-grid-react-wrapper.md §3 independently confirms the tier design: the plain-DOM renderer is the safe default, and the per-cell React root works but is leak-prone — React issue #26281 shows nested `createRoot()` roots are NOT unmounted when the parent root unmounts and their effect cleanups don't run, so `react-renderer` must wire explicit `root.unmount()` to cell-destroy (virtualization destroys off-screen cells constantly). Reproducing ag-grid-react's ReactUI-style direct mounting is not viable — AG Grid's React tree is a private root with no injection point (see ADR 0001).

Verified at resolution: 14 node tests / 38 assertions; headless Chromium 11/11 (all three styles render, hiccup-style map applied, React counter interactive, React local state and DOM content survive `refreshCells` force).

## Decision

The library commits to three renderer tiers, none owning a DOM-building engine:

1. **Bare fn shorthand = the vanilla escape hatch.** Officially documented AG Grid behavior: a function renderer's string return is injected via `innerHTML` (`_loadTemplate`). We keep vanilla semantics — the wrapper never silently rewrites AG Grid's own contract — but dev-warn when a fn in a `*CellRenderer` position returns a string containing `<` (XSS trap; ag-grid-react precedent: React's escaping means strings-as-text there, HTML needs `dangerouslySetInnerHTML`). A fn returning a DOM Node also works here untouched — this is where BYO hiccup->DOM engines plug in, by plain composition, no config API. Zero helper needed: the converter's existing fn auto-wrap gives kebab bean params.

2. **`render/renderer`** — lifecycle map (`{:init :get-gui :refresh :destroy}`, per-instance state atom instead of `this`) -> component class (`raw`-wrapped). The tier for refresh-in-place, destroy hooks, and instance state.

3. **`render/dom-renderer`** — engine-free sugar over `renderer`: `(fn [params-bean] Node|string)`, where a string ALWAYS means text (`createTextNode`), and refresh swaps content in place inside a `<span>` container (refresh returns true). BYO DOM engines compose at the fn level.

4. **`react/react-renderer`** — committed with a per-cell local React root: `createRoot` in `init` + `flushSync` render; `refresh` re-renders into the same root so React local state survives (verified by clicking a `useState` counter then `refreshCells` force); explicit `root.unmount()` in `destroy` (mandatory per React issue #26281, amplified by virtualization churn). It lives in an optional namespace — consumers without React never require it, and react-dom is not a core dep. Right for sparse interactive columns; documented as wrong for React-everywhere grids (scroll churn of roughly 100–300 root create/destroy per second; `flushSync` defeats batching).

No wrapper-level typed-renderer catalog ships. Mantine DataTable precedent: none, pure composition. AG Grid built-ins are reachable by name string (e.g. `"agCheckboxCellRenderer"`), and AG Grid Cell Data Types (v31+) auto-wire boolean/date/number columns; `dataTypeDefinitions` is plain options data the conversion contract already handles (a `with-data-type-definitions` builder is noted on the builder-catalog ticket).

## Consequences

- The bare-fn tier keeps AG Grid's `innerHTML` string semantics, so an HTML-looking string return is an XSS trap by design — mitigated by the dev-mode warning, not by rewriting the contract.
- In `dom-renderer`, string means text, deliberately diverging from the bare-fn tier's `innerHTML` semantics; the divergence is safe because `dom-renderer` is opt-in sugar, not the vanilla path.
- `react-renderer`'s per-cell roots require explicit unmount wiring in `destroy`; skipping it leaks roots and skips effect cleanups under virtualization churn.

  **Correction, 2026-07-29 (agd-01kynwzt3a16).** "the helpers return a `(raw class)`" and tier 2's "-> component class (`raw`-wrapped)" describe the original construction, not today's. Each helper now returns a *deferred* value holding the consumer's own input, and the conversion boundary builds the class (ADR 0021 §4) — that is what makes two calls with the same input `=`, so a rebuilt options map does not re-apply `:column-defs`. The class still crosses the boundary raw for the `prototype && 'getGui'` reason above; only the timing moved. One tier relationship changes with it: `dom-renderer` no longer *calls* `renderer`, since the lifecycle map is fresh closures per call — it tags the render fn and reaches the same class builder at construction time (same for `react-renderer`).

  **Correction, 2026-08-11 (agd-01kzr5qwmfyx).** `react-renderer`'s `root.unmount()` is now deferred one microtask (`queueMicrotask`), not called on the destroy stack: a synchronous unmount inside a React commit — the typical React host destroys grids from a `useEffect` cleanup, which runs under `CommitContext` — trips React 19's DEV synchronous-unmount `console.error` once per live cell, and React defers the actual deletion in that case anyway. The #26281 leak story is unaffected: the root is still explicitly unmounted per cell destroy, only the tick moves, and double-unmount is a no-op. The traded-away guarantee is ordering — cell components' effect cleanups now run after `destroy!` returns for every caller, so cleanups must not touch the grid api.

  **Correction, 2026-08-11 (agd-01kzr7wehb0d, ADR 0023).** Tier 4's "`createRoot` in `init` + `flushSync` render" and the "`flushSync` defeats batching" consequence describe the original scheduling, not today's: cell renders are now queued and drained in ONE `flushSync` per microtask, so content lands before paint rather than on the caller's stack, and a refresh driven from inside a React commit (e.g. `refreshCells` from a `useEffect`) no longer prints React's DEV flushSync-from-lifecycle error per cell. The portal option below ("deferred to the Fulcro skeleton ticket") is **closed** by ADR 0023 — its deferral criterion had been met (ADR 0012), and the measured case for it did not survive the batched flush.

  **Correction, 2026-08-12 (agd-01kzva6hgxq5, ADR 0024).** "The library commits to three renderer tiers" is no longer the full set: a fourth tier, `react/portal-renderer` + `react/portal-host` (consumer-mounted host, `createPortal` per cell), ships beside the per-cell React root for provider-based cell content — providers and error boundaries flow from the consumer's own tree, which no per-cell root can inherit. The per-cell tier stands unchanged; ADR 0023's closure holds for the library-created variant it measured. See ADR 0024.

## Considered options

- **A mini hiccup->DOM engine vendored in the library** (a ~40-line engine with `:style` maps and `on-*` event fns was built and headless-verified in the skeleton) — cut: dialect trap versus real hiccup, maintenance treadmill, and BYO engines compose at the fn level (pluggable-by-composition beats a global config slot). `dom-renderer` ships engine-free instead.
- **Portal variant for React** (one host root + `createPortal` per cell, ag-grid-react's architecture) — not needed; deferred to the Fulcro skeleton ticket: prove `transact!`-from-a-cell with an explicit app reference, and specify a portal variant only if that fails ergonomically.
- **Reproducing ReactUI-style direct mounting** — not viable: AG Grid's React tree is a private root with no public injection point.
- **A wrapper-level typed-renderer catalog** — rejected: AG Grid Cell Data Types plus name-registered built-ins cover that ground, and pure composition covers the rest.

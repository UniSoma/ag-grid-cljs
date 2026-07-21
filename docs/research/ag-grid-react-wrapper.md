# AG Grid React Wrapper vs. Vanilla Core — Findings for a Fulcro-Native ClojureScript Wrapper

> Consolidated from two deep-research passes (2026-07-21). Sources are AG Grid's own
> primary docs, engineering blog, shipped npm source, a founder conference talk, React's
> issue tracker, and the Fulcro/re-frame ecosystem. Claims were adversarially verified
> (3-vote); confidence and refutations are noted inline.
>
> **Caveat that applies throughout:** no source addresses Fulcro directly. Every Fulcro
> mapping below is reasoned inference from documented React-wrapper behavior and generic
> React-reconciliation facts, not a Fulcro-specific citation.

## TL;DR

- **Most of `ag-grid-react`'s value is React-idiomatic plumbing, not grid functionality.**
  The reconciliation-critical machinery — identity-based row diffing (`getRowId`),
  immutable-data reference comparison, and `applyTransaction` — lives entirely in the
  **vanilla JS core**. A Fulcro wrapper inherits it for free by feeding data through core APIs.
- **The wrapper's genuine additions are narrow:** (1) the ReactUI rendering engine that
  mounts React components into grid DOM, (2) the declarative `reactiveCustomComponents`
  contract, and (3) a prop→grid bridge that re-applies every prop each render — the last of
  which is **redundant and mildly hostile** to Fulcro's model.
- **For custom cell content the (a)/(b)/(c) decision is settled:** ReactUI uses one
  *private* internal React root with no public injection point, so **(a) reproducing
  ReactUI-style direct mounting is not viable**. **(c) plain-DOM/hiccup renderers driven by
  the vanilla core is the safe default.** **(b) a separate React/Fulcro root per cell is
  possible but leak-prone** under virtualization and needs explicit per-cell teardown.

---

## 1. Core (inherited free) vs. wrapper-added

### 1.1 Redundant for Fulcro — this is all vanilla core

**Row reconciliation via `getRowId`.** Without Row IDs, "the grid rips all data out of the
grid and starts from scratch." With `getRowId` (a framework-agnostic `GridOption` — a pure
function returning a stable, unique string), the grid compares object references per ID:
differing reference = update that row, identical = leave it. Preserves selection, keeps
groups open, animates, touches only changed DOM rows. This is the same mechanism the old
React "immutable-data mode" merely *exposed* — it was never wrapper-owned. *(high; 3-0)*

- <https://www.ag-grid.com/javascript-data-grid/data-update-row-data/>
- <https://www.ag-grid.com/react-data-grid/row-ids/>

**`applyTransaction(transaction)`.** Core grid API with `add`/`remove`/`update` lists that
mutate the client-side row model in place — no full `rowData` replacement. With `getRowId`
it does key-based lookup (fast); without it, an `===` array scan the docs call slow for
thousands of rows. **This is the natural target for Fulcro:** mutations produce deltas
against a normalized DB, which map far more cleanly onto `applyTransaction` than onto
replacing the whole `rowData` array each render. *(high; 3-0)*

- <https://www.ag-grid.com/javascript-data-grid/data-update-transactions/>

> The claim that the *wrapper* "relies on `getRowId` for reconciliation" was **refuted
> (1-2)** — it's core-grid behavior the wrapper merely surfaces. This distinction is central
> to the whole synthesis.

### 1.2 Genuinely wrapper-added

**ReactUI rendering engine.** Mounts custom components (cell renderers, editors, filters)
directly into the grid's DOM hierarchy — **no wrapper element, no React Portals**. The
legacy engine (`AgGridReactLegacy`) wrapped every component in an `ag-react-container` div
in a Portal. ReactUI has been default since v27 (2022) and is the *only* engine since v31
(`suppressReactUi` removed). This is the central integration seam. *(high; 3-0)* — see §2.

- <https://blog.ag-grid.com/react-ui-overview/>

**`reactiveCustomComponents` (v31.1+, default in v32).** Declarative props+callbacks instead
of imperative instance methods: editors use `value`/`onValueChange` (with `initialValue`);
date components `date`/`onDateChange`; filters `model`/`onModelChange`. A `useGridCellEditor`
hook replaces `useImperativeHandle`. This controlled-component contract maps naturally onto
Fulcro's data-down/mutation-up — better than the old imperative `getValue`/`getModel`. *(high; 3-0)*

- <https://www.ag-grid.com/react-data-grid/upgrading-to-ag-grid-31-1/>

**Prop→grid bridge — the most redundant layer for Fulcro.** The wrapper re-applies every
prop on each React render. A new `rowData`/`columnDefs` reference each render triggers extra
grid renders and can reset grid state (selection, column order/width); hence the
`useState`/`useMemo` discipline. **Fulcro controls its own scheduling, avoids uncontrolled
re-renders, and drives data via mutations — so do NOT replicate set-`rowData`-on-render.**
Push deltas imperatively instead. *(high; 3-0)*
(Nuance: column state can survive when `colId`/`field` still match — docs hedge with "may",
so it's not a guaranteed full reset.)

- <https://www.ag-grid.com/react-data-grid/react-hooks/>
- <https://www.ag-grid.com/react-data-grid/grid-interface/>

**API access via ref, populated late.** The grid API comes through a ref
(`gridRef.current.api`), not a return value, and is not defined until after the component
initialises; the ready-safe path is the `gridReady` event's `params.api` (also on every
event's params). *(high; 3-0)*

- <https://www.ag-grid.com/react-data-grid/grid-interface/>

---

## 2. How ReactUI actually mounts components

**Single unified React tree, no portals.** Application cell renderers/editors/filters render
as ordinary React components nested *directly inside* AG Grid's own tree:
`RowContainerComp → RowComp → CellComp → your-comp`, all under one shared
`<BeansContext.Provider>`, all `React.memo`-wrapped. Founder's words: *"React 100% all the
way through the grid."* Shipped source (`agGridReactUi.tsx`) renders **one** `<GridComp>`
root — **no `createRoot`/`ReactDOM.render` per cell.** *(high; 3-0)*

- <https://gitnation.com/contents/ag-grids-new-react-rendering-engine>
- <https://blog.ag-grid.com/react-ui-overview/>
- <https://deepwiki.com/ag-grid/ag-grid/4.3-react-integration>

**Two-layer architecture — the key insight.** Framework-agnostic vanilla controllers
(`CellCtrl`/`RowCtrl`) own all grid logic and state. React components are reactive views
driven by them: `CellComp` builds a `compProxy` implementing `ICellComp`
(`setRenderDetails`/`setEditDetails`/`toggleCss`) backed by React `useState` setters, and
registers it via `cellCtrl.setComp(...)`. **`createGrid` + `CellCtrl`/`RowCtrl` are a
framework-agnostic surface a CLJS wrapper drives directly — React is not required to run the
grid.** *(high; 3-0)*

- <https://deepwiki.com/ag-grid/ag-grid/4.3-react-integration>

**`agFlushSync`/`agStartTransition` are React-only plumbing you can ignore.** Shipped source
(`@ag-grid-community/react@32.0.2`): `agFlushSync` calls `ReactDOM.flushSync` only when
React 18+ `createRoot`/`flushSync` are feature-detected; otherwise runs `fn()` directly
(React 17 gets no forced sync). Not part of the vanilla contract. *(high; 3-0)*

- <https://app.unpkg.com/@ag-grid-community/react@32.0.2/files/dist/package/index.esm.mjs>

**Cell renderers are virtualized.** A cell renderer is a plain React component receiving
data via props (no base class). `getCellRendererInstances` only returns cells in the
viewport — off-screen cell components are **destroyed** as you scroll. Any per-cell mounting
strategy must cope with aggressive create/destroy churn. *(high; 3-0)*

- <https://www.ag-grid.com/react-data-grid/component-cell-renderer/>

---

## 3. The (a)/(b)/(c) decision for custom cell content

| Option | Verdict | Why |
|---|---|---|
| **(a)** Reproduce ReactUI-style direct mounting into AG Grid's tree | **Not viable** | That tree is AG Grid's *private* internal root; no public injection point for a host. |
| **(b)** Separate React/Fulcro root per custom cell | **Possible but leak-prone** | Works, but see the nested-root hazard below, amplified by virtualization churn. |
| **(c)** Plain-DOM/hiccup cell renderers via the vanilla core | **Safe default** | Sidesteps React-in-React entirely; drives the framework-agnostic core directly. |

**Nested-root teardown hazard (the argument against (b)).** React issue #26281: a
`createRoot()` root created inside another root's tree is **NOT unmounted when the parent
root unmounts, and the child's `useEffect` cleanups do NOT run** — each nested root must be
explicitly `root.unmount()`-ed. Reproduced on React 17.0.2 and 18.2.0 (maintainer-labeled
"Unconfirmed" but reproducible). Combined with cell virtualization (§2), a per-cell root is
created/destroyed constantly and each must be torn down precisely on cell-destroy or you
leak roots and effects. *(high; 3-0)*

- <https://github.com/facebook/react/issues/26281>
- <https://react.dev/reference/react-dom/client/createRoot>

> This corroborates the recorded cell-renderer decision (`agd-01ky0ed8adbf`): the engine-free
> `dom-renderer` is option (c) and the right default; the committed `react-renderer` is
> option (b) and must wire explicit per-cell unmount to cell-destroy to be leak-safe.

---

## 4. Grid create/destroy lifecycle contract

- **Create:** v31 deprecates `new agGrid.Grid(...)` → use
  `const gridApi = agGrid.createGrid(mountEl, gridOptions)`, which returns the API directly.
  Call in `componentDidMount` (mount node must exist); hold the returned `api` for the
  component's lifetime. *(high; 3-0)*
- **Lifecycle events (ordered):** `gridReady` fires first on init — **grid may not be fully
  rendered yet**, so don't assume layout. `gridPreDestroyed` fires last, just before DOM
  removal — the documented hook for cleanup / saving state / **disconnecting other
  libraries**. Bind teardown to `api.destroy()` in `componentWillUnmount` (and/or
  `gridPreDestroyed`). *(high; 3-0)*
- **Stability:** hold the grid instance and `gridOptions` stably across host re-renders.
  Object props need `useState`/`useMemo`, callbacks need `useCallback`, **or the grid resets
  internal state on every re-render** — and wrong callback deps cause **stale closures** (AG
  Grid warns explicitly). CLJS analogue: build `gridOptions`/callbacks once and retain in
  component-local state or an atom. *(high; 3-0)*

- <https://www.ag-grid.com/javascript-data-grid/upgrading-to-ag-grid-31/>
- <https://www.ag-grid.com/javascript-data-grid/grid-lifecycle/>
- <https://www.ag-grid.com/react-data-grid/react-hooks/>

---

## 5. The proven CLJS embedding pattern (applies to Fulcro)

The established re-frame/Reagent pattern for a stateful JS component that owns its own DOM is
a **two-component split**, mapping cleanly onto a Fulcro `defsc`:

- **Outer** component sources data; **inner** wrapper's renderer **always emits the same
  minimal static container** (an empty mount `div`) regardless of props — so React/Fulcro
  **never reconciles the JS-owned subtree**.
- Lifecycle methods do the work: `createGrid` in `componentDidMount`; push updates via
  `api.applyTransaction` / `api.setGridOption` in `componentDidUpdate`; `api.destroy()` in
  `componentWillUnmount`.
- **Never let the Fulcro/React tree render children into the grid-owned node.** *(high; 3-0)*

- <https://github.com/Day8/re-frame/blob/master/docs/Using-Stateful-JS-Components.md>

> The re-frame source is Reagent-specific; it generalizes to Fulcro by React-reconciliation
> reasoning, not a Fulcro-specific citation.

---

## 6. What a Fulcro-native wrapper must do itself

1. **Own the grid lifecycle** — `createGrid(mountEl, gridOptions)` on `componentDidMount`;
   `api.destroy()` on unmount. Hold the instance stably (atom/component-local) so it survives
   Fulcro re-renders without recreation.
2. **Capture the API from `gridReady`/`createGrid` return, not a live-on-mount ref.** Stash
   it; route inbound pushes and outbound events through it. *(exact safe-call boundary is an
   open question — see below.)*
3. **Feed data via `applyTransaction`, not prop churn.** Always supply `getRowId`
   (deterministic, unique) so matching is key-based. Bypasses the wrapper's
   set-`rowData`-on-render reconciliation entirely.
4. **Get events out → into mutations.** Translate selection / edit-commit / sort / filter
   events into Fulcro transactions.
5. **Bridge components into custom slots via option (c)** by default (plain-DOM/hiccup
   renderers driven by the vanilla core); reserve option (b) (per-cell React/Fulcro root) for
   where Fulcro components are genuinely needed, with explicit per-cell teardown.
6. **Keep `gridOptions` and callbacks stable** across re-renders; watch for stale closures.

---

## 7. Open questions (unresolved / need primary-source verification)

1. **Exact safe-call boundary for the API.** Two claims were **refuted** — that `gridReady`
   is the only-safe hook, and that `gridOptions.api` no longer works under `createGrid`. So
   whether the API is safe immediately after `createGrid()` returns vs. only after
   `gridReady`, and whether `gridOptions.api` still populates, is unresolved. **This directly
   blocks the init sequence in the Fulcro walking skeleton (`agd-01ky0ed8766f`).**
2. **Is there any supported way to inject host React context into cells** — or must Fulcro
   pass all cell data purely through `gridOptions`/`rowData`/cell params?
3. **Fulcro-specific render-loop hazards** beyond the generic React/Reagent ones (ident-based
   refresh, keyframe render, `defsc` lifecycle interacting with a JS-owned node outside its
   query tree).
4. **No benchmark** compares plain-DOM (c) vs per-cell Fulcro roots (b) under heavy
   virtualized scroll inside a CLJS wrapper.

---

## Sourcing & version notes

- Confirmed claims trace mostly to AG Grid's own docs/blog + shipped npm source; the founder
  talk and DeepWiki (secondary/AI-generated) were cross-checked against primary source files.
  Performance words ("faster", "avoids flicker") are the vendor's, not independently benchmarked.
- **Version-pinned:** ReactUI default since v27, only engine since v31 (legacy removed);
  `reactiveCustomComponents` 31.1+, default v32; `createGrid` v31+; `agFlushSync` source is
  `@ag-grid-community/react@32.0.2` and may differ across versions. Current through
  `ag-grid-react` 35.x as of research.

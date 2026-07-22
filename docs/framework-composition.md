# Framework composition

The library ships **no framework adapters** in v1 — no Reagent/UIx renderer
sugar, no reactive layer (ADR 0012). It does not need them: the core is already
framework-agnostic, and the seams a future adapter would wrap are all public
today. This page is that composition recipe.

## Reagent and UIx components in a cell — one line

[[ag-grid-cljs.react/react-renderer]] accepts **any React element**, and both
Reagent and UIx produce React elements. So a component from either mounts into a
cell through one call, no adapter:

```clojure
;; Reagent — r/as-element turns hiccup into a React element
{:cell-renderer (react/react-renderer (fn [p] (r/as-element [my-cell (:data p)])))}

;; UIx — $ produces a React element directly
{:cell-renderer (react/react-renderer (fn [p] ($ my-cell {:row (:data p)})))}
```

The renderer fn still receives the lazy kebab-bean params
([Cell rendering](cell-rendering.md)); you pass whatever it needs into your
component. That is the entire "adapter" — a one-liner you write.

## Stateful components: the static-mount-div split

The per-cell React root is a **detached** root: no React context, no re-frame
subscription context reaches into it. For a genuinely stateful component
(re-frame subscriptions, a component that must re-render from app state), use the
proven **two-component split** — Day8's "Using Stateful JS Components" pattern.
One outer component renders a *static* mount div and never re-renders it; the
grid owns everything below that div.

```clojure
;; Reagent form-3: the mount div is static; :should-component-update false pins
;; it so app-state refreshes re-render AROUND the grid, never through it.
(defn grid-view [initial-rows]
  (let [handle (atom nil)]
    (r/create-class
     {:component-did-mount    (fn [this]
                                (reset! handle (ag/create-grid! (rdom/dom-node this)
                                                                (grid-opts initial-rows))))
      :component-will-unmount (fn [_] (ag/destroy! @handle))
      :should-component-update (fn [_ _ _] false)
      :reagent-render         (fn [_] [:div {:style {:height "400px"}}])})))
```

Post-mount, data reaches the grid through the [explicit
channels](updating-data.md) — `set-rows!` / `transact!` from a re-frame effect
or a `deref`-then-sync — **not** by re-rendering the component. A cell that needs
to dispatch back into your app calls its framework's API with an **explicit
reference** (a `re-frame.core/dispatch`, a Fulcro `comp/transact!` against the
app you `defonce`d), since the detached root has no ambient context to lean on.

## The nested-`createRoot` caveat

Each `react-renderer` cell is its own `createRoot`. A React root nested inside
another root's tree is **not** unmounted when the parent unmounts, and its effect
cleanups do not run (React issue [#26281](https://github.com/facebook/react/issues/26281)).
[[ag-grid-cljs.react/react-renderer]] already wires `root.unmount()` into cell
`destroy` for you, which matters because grid virtualization destroys and
recreates off-screen cells constantly — a missing unmount leaks a root (and skips
its effect cleanups) on every scroll. If you write a per-cell root by hand
instead of using the helper, you own that unmount.

## The reactive seams (for a future adapter)

A reactive or full-state-declarative layer is deliberately out of core (ADR
0008/0012) — but it composes later over three already-public seams, so nothing
here forecloses it:

- [[ag-grid-cljs.core/update-grid!]] (the PATCH differ) plus the `GridHandle`'s
  `{:api :opts}` — a reactive layer watches your state and pushes diffs through
  `update-grid!`; core owns the diff, an adapter wraps it.
- [[ag-grid-cljs.react/react-renderer]] — the mount point for any React-flavored
  cell sugar.
- [[ag-grid-cljs.core/grid-api]] and [[ag-grid-cljs.core/raw]] — the escape
  hatches for anything the wrapper does not cover.

Building on these is opt-in, later, and outside the library; the v1 deliverable
is this recipe, not a namespace.

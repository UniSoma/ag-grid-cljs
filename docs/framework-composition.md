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

That "not by re-rendering" is about **rows**. Options are the other way round:
rebuilding the whole options map during your own render and pushing it through
`update-grid!` is a supported shape, because the wrapper's builders assoc data
rather than freshly minted closures, so an unchanged rebuild diffs to nothing.
See [Rebuilding the whole map is a supported
shape](updating-data.md#rebuilding-the-whole-map-is-a-supported-shape).

## Design-system components in cells

Interactive cells in a React-hosted app are usually design-system components —
and every mainstream design system (Mantine, MUI, Chakra, styled-components
theming, react-intl, Redux) delivers its theme, store, or locale through a
**provider**. React context does not cross roots, and each `react-renderer`
cell is its own detached root, so a provider-requiring component **throws**
inside a cell and the cell paints empty: React unmounts a root on an uncaught
render error. Dev builds warn once per distinct error with a pointer back to
this section; a production console shows only React's default `reportError`
log, and the UI is silently empty.

The supported shape is the **provider wrap**: wrap the render fn's output in
the provider the cell content needs, with the provider's style-injection props
turned off (your app tree already injected styles once), and define the wrapped
renderer once at namespace level:

```clojure
;; provider inputs defined once — stable values, not rebuilt per cell render
(def ^:private cell-theme (create-theme {...}))

(defn- mantine-cell [render-fn]
  (react/react-renderer
   (fn [params]
     (react/createElement MantineProvider
                          #js {:theme cell-theme
                               :withCssVariables false    ; style injection off:
                               :withGlobalClasses false}  ; the app did it once
                          (render-fn params)))))

;; ONE def per wrapped renderer — never minted inline in a render fn
(def ^:private status-cell (mantine-cell status-badge))
(def ^:private type-cell   (mantine-cell type-icon))
```

The namespace-level `def` is the consumer's half of rebuild stability
(ADR 0021): the renderer value stays `=` across options-map rebuilds, so
`update-grid!` diffs it to nothing. A `(mantine-cell …)` call inlined in your
own render path would mint a fresh renderer each time and re-ship
`:column-defs` on every rebuild.

The cost is real, so weigh it per column. Each visible cell mounts its own
provider, which means cells *manage* what they should merely *consume* — a
provider that watches color scheme, for instance, adds one listener and one
writer per visible cell. And the wrap delivers context values only: your app's
**error boundaries** still cannot reach into a detached cell root. For
provider-based cell content at scale, a consumer-mounted **portal-host** tier —
cells portal out of one host component rendered under your real providers, so
context and error boundaries flow natively — is under evaluation; until it
lands, this wrap is the supported pattern.

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

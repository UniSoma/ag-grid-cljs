# Cell rendering

A cell renderer decides what a cell's DOM looks like. AG Grid's native renderer
interface is a component class (`init` / `getGui` / `refresh` / `destroy`);
this library gives you **three tiers** over it, none of which owns a DOM-building
engine (ADR 0011). Pick the lowest tier that does the job.

| Tier | You write | Reach for it when |
| --- | --- | --- |
| Bare fn | `(fn [params] …)` | one-shot formatting, the vanilla escape hatch |
| [[ag-grid-cljs.render/dom-renderer]] | `(fn [params] Node\|string)` | refresh-in-place DOM, BYO builder |
| [[ag-grid-cljs.react/react-renderer]] | `(fn [params] react-element)` | a sparse column of interactive React |

Across all three, **params arrive as a lazy kebab-keyed bean** —
`(:value p)`, `(:data p)`, `(:row-index p)` — the same view every callback gets
(see [Options and conversion](options-and-conversion.md#callbacks-what-your-functions-receive-and-return)).

The two helper tiers live in their own namespaces; the examples below assume:

```clojure
(:require [ag-grid-cljs.core   :as ag]
          [ag-grid-cljs.render :as render]
          [ag-grid-cljs.react  :as react])   ; optional; pulls in react-dom
```

## Tier 1 — bare fn (the vanilla escape hatch)

Put a plain function in a `:cell-renderer` position and the converter auto-wraps
it. This is AG Grid's own `ICellRendererFunc`: **a string return is injected as
the cell's `innerHTML`.** No helper, no import.

```clojure
{:field :born
 :cell-renderer (fn [p] (str "★ " (:value p)))}
```

The wrapper keeps AG Grid's contract exactly — it does not silently rewrite it —
so an HTML-looking string is an `innerHTML` sink by design. In dev you get an
**XSS nudge** when a `*CellRenderer` fn returns a string containing `<`; the fix
is to return a DOM `Node` instead, which also works here untouched. This is
where a bring-your-own hiccup→DOM engine plugs in: it composes at the fn level,
no config API needed.

Use this tier for static, non-interactive formatting. It has no `refresh` hook,
so on a value change AG Grid rebuilds the cell from scratch.

## Tier 2 — `dom-renderer` (engine-free DOM sugar)

[[ag-grid-cljs.render/dom-renderer]] wraps a `(fn [params] Node|string)` into a
real component class and adds **refresh-in-place**: the result lives inside a
`<span>` and a value change swaps its content without re-initializing. Here a
**string always means text** (`createTextNode`) — deliberately *not* the bare-fn
tier's `innerHTML`, because this tier is opt-in sugar, not the vanilla path.

```clojure
(defn salary-renderer [p]
  (let [bar (js/document.createElement "div")]
    (set! (.-textContent bar) (str "$" (:value p)))
    bar))

{:field :salary
 :cell-renderer (render/dom-renderer salary-renderer)}
```

No DOM engine ships in the box — you build nodes however you like (`createElement`
above, or any hiccup→DOM fn). If you need the full lifecycle (an instance state
atom, a `destroy` hook, custom `refresh` logic), drop to the underlying
[[ag-grid-cljs.render/renderer]], which takes the raw
`{:init :get-gui :refresh :destroy}` map with a per-instance state atom in place
of `this`.

## Tier 3 — `react-renderer` (a React root per cell)

[[ag-grid-cljs.react/react-renderer]] mounts a **local React root in each cell**:
`createRoot` in `init`, `flushSync` render so the cell has content synchronously,
and `refresh` re-renders into the *same* root so component local state survives a
value refresh. It lives in the optional `ag-grid-cljs.react` namespace, so only
consumers who require it need `react-dom` on the classpath — the core stays
framework-agnostic.

```clojure
(defn actions-cell [p]
  (react/createElement counter-button #js {:id (-> p :data :id)}))

{:header-name "Actions"
 :cell-renderer (react/react-renderer actions-cell)}
```

It is the right tool for a **sparse** column of genuinely interactive cells. It
is the **wrong** tool for a React-everywhere grid: scroll virtualization churns
roughly 100–300 root create/destroy per second, and `flushSync` defeats React's
batching. The per-cell root also carries a hazard the helper handles for you —
`destroy` calls `root.unmount()`, mandatory because a nested `createRoot` root is
**not** unmounted with its parent and its effect cleanups never run (React issue
#26281), amplified by virtualization. Mounting Reagent or UIx components into
this tier is covered in [Framework composition](framework-composition.md).

## Built-in renderers by name

AG Grid ships its own renderers; reach them by **name string** — no wrapper
catalog, no import (ADR 0011):

```clojure
{:field :done :cell-renderer "agCheckboxCellRenderer"}
{:field :progress :cell-renderer "agGroupCellRenderer"}
```

The wrapper deliberately ships no typed-renderer catalog: name-registered
built-ins plus Cell Data Types (below) plus your own composition cover the
ground.

## Data type definitions

AG Grid **Cell Data Types** (v31+) auto-wire editing, formatting, and filtering
for `boolean` / `date` / `number` / `text` columns, and let you define your own.
This is plain options data — the
[conversion contract](options-and-conversion.md) already handles it, so there is
nothing wrapper-specific to learn: `:data-type-definitions` is a passthrough map.

```clojure
{:data-type-definitions
 {:percentage {:extends-data-type :number
               :value-formatter (fn [p] (str (:value p) "%"))}}
 :column-defs [{:field :margin :cell-data-type :percentage}]}
```

Keys camelize (`:extends-data-type` → `extendsDataType`), the formatter fn is
auto-wrapped with a kebab-bean, and `:percentage` (a keyword — AG Grid
vocabulary) becomes the string `"percentage"`. Because a column with a declared
data type gets its editor and renderer for free, defining a data type is often a
better move than hand-writing a renderer per column.

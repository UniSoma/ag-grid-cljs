# Getting started

This walks you from an empty project to a running grid, then tours the builder
catalog. It stays narrative: every builder and runtime function has its full
contract in its docstring, linked inline — this page shows them in context, it
does not restate them.

## Install

The library ships no npm package. AG Grid itself is a **peer dependency** you
install and own: `ag-grid-community` for Community, plus `ag-grid-enterprise`
if you use Enterprise features.

```sh
npm install ag-grid-community
```

Add the library to your `deps.edn` (Clojure CLI / shadow-cljs):

```clojure
io.github.unisoma/ag-grid-cljs {:mvn/version "..."}
```

It has zero runtime dependencies beyond the AG Grid peer packages.

## Register modules

AG Grid v33+ is modular: nothing renders until modules are registered, and the
consumer owns that step. [[ag-grid-cljs.core/register!]] is thin sugar over AG
Grid's `ModuleRegistry` — call it once, before the first grid is created.

```clojure
(ns my.app
  (:require [ag-grid-cljs.core :as ag]
            ["ag-grid-community" :refer [AllCommunityModule]]))

(ag/register! AllCommunityModule)
```

`AllCommunityModule` is the batteries-included bundle. Tree-shaking to the
individual feature modules is an AG Grid concern — register whichever modules
you want; the wrapper does not care which.

## Your first grid

Build an EDN options map, hand it and a DOM element to
[[ag-grid-cljs.core/create-grid!]], and keep the `GridHandle` it returns — every
runtime channel takes that handle.

```clojure
;; Rows are JS by contract: a JS array of JS objects (see the next article).
(def rows
  #js [#js {:id 1 :name "Ada"   :price 42}
       #js {:id 2 :name "Alan"  :price 37}])

(defonce handle
  (ag/create-grid! (js/document.getElementById "app")
    (-> (ag/options)
        (ag/with-columns [{:field :name} {:field :price}])
        (ag/with-row-id :id)
        (ag/with-row-data rows))))
```

That is the whole lifecycle: `create-grid!` mounts, the handle drives updates
(see [Updating data](updating-data.md)), and
[[ag-grid-cljs.core/destroy!]] tears it down. To reach any imperative AG Grid
capability the wrapper does not wrap, [[ag-grid-cljs.core/grid-api]] returns the
raw `GridApi` — the escape hatch.

## The builder catalog

The bottom layer is *always* a plain EDN options map: the full AG Grid options
surface is reachable by ordinary `assoc`, and every builder is just sugar that
`assoc`es for you. [[ag-grid-cljs.core/options]] starts a map (empty, or from an
existing one); reach for a builder when it earns its keep by coercing a friendly
shape or bundling options that must move together.

**Columns and data.** [[ag-grid-cljs.core/with-columns]] sets the column
definitions; [[ag-grid-cljs.core/with-row-data]] sets the initial rows.

**Row identity.** [[ag-grid-cljs.core/with-row-id]] gives the grid a stable
per-row id — the load-bearing choice that lets `set-rows!` and `transact!` diff
by id and preserve scroll, selection, and focus across updates. Pass a keyword
naming an id field, or a function.

**Selection and pagination.** [[ag-grid-cljs.core/with-selection]] configures
row selection through the modern object form (shielding you from AG Grid's
deprecated string flags); [[ag-grid-cljs.core/with-pagination]] turns on and
sizes pagination.

**Server-side data.** [[ag-grid-cljs.core/with-infinite-datasource]] wires up
the Infinite Row Model with your fetch function.

Nothing forces a builder. `(-> (ag/options) (assoc :dom-layout :auto-height))`
is exactly as valid — the builders are documented sugar, not a gate. Anything
they do not cover, you `assoc`; anything the conversion boundary should not
touch, you wrap with [[ag-grid-cljs.core/raw]] (see the next article).

## Dev-mode validation

In development, opt into the wrapper's typo and deprecation warnings with
[[ag-grid-cljs.core/enable-dev-validations!]] (call once at startup). Unknown
keys get a kebab did-you-mean; it is warn-only and compiled out of production
builds entirely. For type and option-dependency checks, register AG Grid's own
`ValidationModule` alongside it.

## Enterprise setup

Enterprise is opt-in and isolated in the `ag-grid-cljs.enterprise` namespace, so
Community-only consumers never import `ag-grid-enterprise`. Install the peer
package, install your license with
[[ag-grid-cljs.enterprise/set-license-key!]] **before** the first grid, and
register an Enterprise module bundle through the same `register!` you already
use:

```clojure
(ns my.app
  (:require [ag-grid-cljs.core :as ag]
            [ag-grid-cljs.enterprise :as ent]
            ["ag-grid-enterprise" :refer [AllEnterpriseModule]]))

(ent/set-license-key! "<your-license-key>")   ; before the first grid
(ag/register! AllEnterpriseModule)
```

With that in place, Enterprise builders such as
[[ag-grid-cljs.core/with-cell-selection]] (range selection, the fill handle)
work like any other. Without a valid key, Enterprise features still run but AG
Grid logs a license error and renders a watermark.

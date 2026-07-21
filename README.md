# ag-grid-cljs

ClojureScript wrapper for [AG Grid](https://www.ag-grid.com/): EDN options, transactional updates, and a framework-agnostic vanilla core.

> **Status: design phase — not released.** This repo holds the design
> spec in progress (tracked as tickets under `.tickets/`) and a walking skeleton
> that proves the risky parts. APIs, namespaces, and everything below are
> provisional until the spec is locked. Do not depend on this yet.

## What it will be

`io.github.unisoma/ag-grid-cljs` — an MIT-licensed wrapper over the **vanilla**
AG Grid core (`createGrid`), not `ag-grid-react`. AG Grid's reconciliation
machinery (`getRowId` diffing, `applyTransaction`) lives in the core, so you
get it without a React layer; any framework (the reference consumer is
Fulcro) hosts the grid in a plain mount node and drives it through data
functions.

Design pillars, as decided so far:

- **Plain EDN options map** at the bottom: kebab-case keys auto-translated to
  camelCase, the full AG Grid options surface reachable by plain `assoc`.
  Curated `with-*` builders on top as documented sugar. A generated key
  registry powers dev-mode typo warnings instead of a typed schema layer.
- **Transactional update model**: `set-rows!` / `transact!` map onto AG Grid's
  own diffing and `applyTransaction`, preserving scroll, selection, and focus.
  The raw grid API remains the escape hatch.
- **Row data is JS by contract**; callbacks receive lazy kebab-keyed beans
  (a vendored cljs-bean slice, so there are no runtime dependencies beyond AG Grid).
- **Community + Enterprise** both wrapped; AG Grid npm packages are peer
  dependencies, never bundled; the consumer owns module registration.

Provisional sketch (from the walking skeleton, subject to renaming):

```clojure
(ns my.app
  (:require [ag-grid-cljs.core :as ag]))

(def api
  (ag/create-grid el
    (-> (ag/options {:get-row-id (fn [p] (:id (:data p)))})
        (ag/with-columns [{:field :name} {:field :price}])
        (ag/with-row-data rows))))

(ag/transact! api {:update [{:id 1 :name "Ada" :price 42}]})
```

## Development

Requires Node and Clojure CLI; shadow-cljs ≥ 3.3.0.

```sh
npm install
npm run dev   # dev app at http://localhost:8080
npm test      # node contract tests
```

Design decisions and open questions are tracked with [knot](.tickets/); see
the map ticket `agd-01ky0ebxg01e`. Background research lives in
[`docs/research/`](docs/research/).

## License

MIT (planned; no release yet).

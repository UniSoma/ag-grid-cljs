# ag-grid-cljs

[![Clojars Project](https://img.shields.io/clojars/v/io.github.unisoma/ag-grid-cljs.svg)](https://clojars.org/io.github.unisoma/ag-grid-cljs)
[![cljdoc badge](https://cljdoc.org/badge/io.github.unisoma/ag-grid-cljs)](https://cljdoc.org/d/io.github.unisoma/ag-grid-cljs)
[![CI](https://github.com/UniSoma/ag-grid-cljs/actions/workflows/ci.yml/badge.svg)](https://github.com/UniSoma/ag-grid-cljs/actions/workflows/ci.yml)

ClojureScript wrapper for [AG Grid](https://www.ag-grid.com/): a plain EDN
options map over the **vanilla** AG Grid core (`createGrid`, not
`ag-grid-react`), transactional updates that map onto AG Grid's own diffing,
and a framework-agnostic mount you drive from any framework.

> **Status: design phase — not released.** This repo holds the design spec in
> progress (tracked as tickets under `.tickets/`) and a walking skeleton that
> proves the risky parts. APIs and namespaces are provisional until the spec is
> locked. Do not depend on this yet.

## Quickstart

`io.github.unisoma/ag-grid-cljs` is MIT-licensed and ships no npm package —
`ag-grid-community` (and, for Enterprise, `ag-grid-enterprise`) are peer
dependencies you install and register yourself.

```sh
npm install ag-grid-community
```

Add the wrapper to your `deps.edn`:

```clojure
;; deps.edn
io.github.unisoma/ag-grid-cljs {:mvn/version "0.1.0-SNAPSHOT"}
```

`0.1.0-SNAPSHOT` is a mutable, pre-stable artifact — it can change under you, so
don't pin it in production.

```clojure
(ns my.app
  (:require [ag-grid-cljs.core :as ag]
            ["ag-grid-community" :refer [AllCommunityModule]]))

;; The consumer owns module registration; must run before the first grid.
(ag/register! AllCommunityModule)

;; Rows are JS by contract — a JS array of JS objects.
(def rows
  #js [#js {:id 1 :name "Ada"   :price 42}
       #js {:id 2 :name "Alan"  :price 37}])

(defonce handle
  (ag/create-grid! (js/document.getElementById "app")
    (-> (ag/options)
        (ag/with-columns [{:field :name} {:field :price}])
        (ag/with-row-id :id)
        (ag/with-row-data rows))))

;; Transactional update: AG Grid diffs by :id and preserves scroll/selection.
(ag/transact! handle {:update [#js {:id 1 :name "Ada Lovelace" :price 42}]})
```

## Documentation

Full docs are the [cljdoc](https://cljdoc.org/) site — topical articles plus the
API reference rendered from docstrings. Start with **Getting started**
([`docs/getting-started.md`](docs/getting-started.md)) and **Options and
conversion** ([`docs/options-and-conversion.md`](docs/options-and-conversion.md)).
The full kebab↔camel option surface is in the generated
[options reference](docs/reference/ag-grid-options.md).

## Development

Requires Node and Clojure CLI; shadow-cljs ≥ 3.3.0.

```sh
npm install
npm run dev           # dev app at http://localhost:8080
npm test              # node contract suite
npm run test:browser  # committed browser suite (Playwright-driven headless Chromium)
```

The browser suite (ADR 0015) runs cljs.test assertions inside the real AG Grid
runtime; a Playwright driver serves the compiled build, drives headless
Chromium, and fails on any unexpected console error. It needs a browser once:
`npx playwright install --with-deps chromium`. The Enterprise smoke runs
unlicensed by default (its license console errors are allowlisted). To run it
licensed locally, inline your key at compile time:

```sh
AG_GRID_LICENSE="<your-key>" npm run test:browser
```

Design decisions and open questions are tracked with [knot](.tickets/); the
architecture is recorded in [`docs/adr/`](docs/adr/). Background research lives
in [`docs/research/`](docs/research/).

## License

MIT (planned; no release yet).

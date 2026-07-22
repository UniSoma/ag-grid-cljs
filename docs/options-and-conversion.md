# Options and conversion

The whole library rests on one idea: you write AG Grid options as a plain EDN
map, and a mechanical boundary converts it to the JavaScript AG Grid expects.
There is no schema layer and no lookup table — one law you can hold in your
head, applied by type. This article is that law.

## The EDN options map

Everything you configure — grid options, column definitions, nested params — is
an ordinary Clojure map with kebab-case keyword keys:

```clojure
{:pagination true
 :pagination-page-size 25
 :column-defs [{:field :first-name :sortable true}
               {:field :price :type :numeric-column}]
 :default-col-def {:flex 1}}
```

The full AG Grid options surface is reachable this way — the
[builders](getting-started.md#the-builder-catalog) are sugar over this map, and
anything they do not cover you reach by plain `assoc`. Because the wrapper never
gatekeeps on a schema, options from a newer AG Grid than the library was pinned
against just work.

## The kebab→camel law

Every keyword — in key *and* value position — camelizes by pure string
transform: split the name on `-`, capitalize each segment after the first, join.

| EDN | JS |
| --- | --- |
| `:row-data` | `"rowData"` |
| `:pagination-page-size` | `"paginationPageSize"` |
| `:sortable` | `"sortable"` (single segment, unchanged) |
| `:rowData` | `"rowData"` (already camel, passes through) |

That last row matters: **already-camel input passes unchanged**, so you can
paste option names straight from the AG Grid docs and they still work. The
transform is collision-free over the entire real AG Grid option surface.

The library also ships the reverse of this table as a browsable
[options reference](reference/ag-grid-options.md) — every kebab name with
the camelCase it converts to. Use your browser's find to look up either side.

## Type-driven recursion

Conversion recurses by *type*, with no notion of which key or how deep:

- **CLJS map** → JS object (keys camelized, values recursed)
- **CLJS vector / list / seq** → JS array (elements recursed)
- **keyword** → camelCase string
- **everything else passes through untouched** — strings, numbers, booleans,
  functions, JS objects and arrays, class instances, `js/Date`, datasources

So row data arriving as JS is untouched by construction, and a JS datasource you
built by hand is handed to AG Grid exactly as-is. `nil` converts to `null` with
the key *kept* in the output, so "explicitly unset" stays expressible (which the
[update differ](updating-data.md) relies on).

## Keywords are AG Grid's vocabulary

The keyword-vs-string distinction is the API's whole enum story: **a keyword
means "an AG Grid term — translate it"; a string means "my data — hands off".**

```clojure
{:row-selection {:mode :multiple}}   ; :multiple -> "multiple"
{:dom-layout :auto-height}           ; :auto-height -> "autoHeight"
{:field :first-name}                 ; -> "firstName"
{:field "first_name"}                ; snake_case data column: string, verbatim
```

Use the keyword form for AG Grid vocabulary; use the string form when the value
is your own data (a snake_case field name, a literal string AG Grid should not
touch).

## Row data is JS by contract

Row data is the one place the library asks you to hand it JavaScript directly: a
**JS array of JS objects**. It is on the hot path (thousands of rows, re-diffed
on every update), and the boundary deliberately never walks it.

```clojure
(def rows
  #js [#js {:id 1 :name "Ada"  :price 42}
       #js {:id 2 :name "Alan" :price 37}])
```

Column props (`:field :first-name`) still convert normally; the *rows* stay
untouched. In dev, passing a CLJS collection to a data-carrying option
(`:row-data`, `:pinned-top-row-data`, `:context`, …) warns you — see below.
This applies equally to [`set-rows!` and `transact!`](updating-data.md).

## Callbacks: what your functions receive and return

Functions found in the options tree are auto-wrapped in both directions:

- **Params arrive as a lazy kebab-keyed bean** — `(:row-index p)`, `(:value p)`,
  `(:data p)`. It is a view, not a copy: only the keys you touch pay conversion,
  and the underlying JS (including row data) is reachable and never converted.
- **Return values run forward through the converter** — a keyword becomes a
  camel string, `{:font-weight "bold"}` becomes a JS object, scalars are free.

The full event and callback shape is its own topic; renderer functions in
particular have their own article ([Cell rendering](cell-rendering.md)).

## The `raw` escape hatch

[[ag-grid-cljs.core/raw]] is the sole opt-out. Wrap any value and the converter
emits it untouched — no recursion, no renaming, no function wrapping:

```clojure
;; Identity round-trip: a CLJS map you want back unchanged in a callback.
{:context (ag/raw {:tenant-id 42})}

;; Hot-path callback: raw JS params in, return passed as-is (no bean, no
;; forward conversion) — the documented idiom for per-cell getters on large grids.
{:value-getter (ag/raw (fn [^js p] (.. p -data -price)))}
```

`raw` is a visible wrapper type on purpose — metadata was rejected because it is
invisible in code and silently lost through collection operations.

Two more edge rules worth knowing: a **CLJS set** passes through untouched *with
a warning* (converting it to an array would emit non-deterministic order — set
literals are almost always a `#{}`-for-`[]` mistake), and a **namespaced
keyword** converts by its name only, warning that the namespace was dropped.

## Dev-mode warnings

All of the above nudges — plus unknown-key typo detection and deprecation
notices — are **dev-only** and compiled out of production builds entirely
(`goog.DEBUG` false dead-code-eliminates the validation code and the key
registry). They never reject or alter what AG Grid receives; the open-surface
guarantee holds. You get:

- **JS-by-contract nudge** — a data-carrying key received a CLJS collection.
- **XSS nudge** — a renderer function returned an HTML-looking string (AG Grid
  injects it via `innerHTML`); see [Cell rendering](cell-rendering.md).
- **Set / namespaced-keyword warnings** — as described above.
- **Typo and deprecation warnings** — opt in with
  [[ag-grid-cljs.core/enable-dev-validations!]]; unknown keys get a kebab
  did-you-mean, deprecated keys point at their replacement.

For type, option-dependency, and row-model checks beyond the kebab layer,
register AG Grid's own `ValidationModule` in your dev bundle alongside
`enable-dev-validations!`.

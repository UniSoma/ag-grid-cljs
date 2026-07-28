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

### The same law in key position

A few options are maps whose keys are names *you* coin and then cite somewhere
the converter cannot see. Those keys are your vocabulary, so they are **strings**:

```clojure
;; CSS class names — cited by your stylesheet
{:row-class-rules {"row-warning" #(< (:qty (:data %)) 10)}}

;; an agg-func name — cited by a ColDef, so the same string goes on both sides
{:agg-funcs   {"my-total" (fn [p] (reduce + (.-values p)))}
 :column-defs [{:field :qty :agg-func "my-total"}]}
```

| Option | Keys are | Cited from |
| --- | --- | --- |
| `:row-class-rules`, `:cell-class-rules` | CSS class names | your stylesheet |
| `:agg-funcs` | agg-func names | ColDef `:agg-func` |
| `:column-types` | column type names | ColDef `:type` |
| `:data-type-definitions` | cell-data-type names | ColDef `:cell-data-type` |
| `:components` | component names | `:cell-renderer`, `:cell-editor`, `:filter` |

Write a keyword instead and it camelizes like any other key: `{:row-warning f}`
emits the class `"rowWarning"`, which matches no `.row-warning` rule — no error,
no styling, nothing in the console.

Keywords on **both** sides of a citation do work: `{:agg-funcs {:my-total f}}`
with `:agg-func :my-total` camelizes to `"myTotal"` twice and matches. But it
leaves you one edit away from a silent break, since changing either side alone
breaks the citation. Strings on both sides is the spelling that cannot rot.

Not every map with your own keys needs strings. `:cell-renderer-params` keys are
yours too and keywords are fine there, because the name never leaves the
wrapper — it comes back through the [literal-key
fallback](#callbacks-what-your-functions-receive-and-return) when your renderer
reads it. The rule is about names that escape into something matching them
literally.

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

### If your rows are CLJS data

Convert them yourself, at the edge. What matters is that **a row spelling and a
column `:field` spelling are a pair**: `{:field :first-name}` emits
`"firstName"`, so a row carrying `"first-name"` renders a blank cell under it.
Two pairings are supported:

| Row shape | Column field | Callback read |
| --- | --- | --- |
| `{"firstName": "Ada"}` — camel-keyed | `{:field :first-name}` (keyword) | `(:first-name (:data p))` |
| `{"first-name": "Ada"}` — literal kebab | `{:field "first-name"}` (string) | `(:first-name (:data p))` |

**Camel-keyed rows** — convert with [[ag-grid-cljs.core/kebab->camel]] as the
`:keyword-fn`, and your columns stay in ordinary keyword form:

```clojure
(-> (ag/options)
    (ag/with-columns [{:field :first-name} {:field :price}])
    (ag/with-row-data
      (clj->js [{:first-name "Ada" :price 42}] :keyword-fn ag/kebab->camel)))
```

`clj->js` applies `:keyword-fn` to keyword *values* too, so `{:status
:in-progress}` becomes `{"status": "inProgress"}` and renders that way. Keep
values as strings where the literal spelling matters.

**Literal kebab-keyed rows** — bare `clj->js` keys objects with `(name k)`, so
rows keep their kebab spelling and the columns must name them as strings:

```clojure
(-> (ag/options)
    (ag/with-columns [{:field "first-name"} {:field "price"}])
    (ag/with-row-data (clj->js [{:first-name "Ada" :price 42}])))
```

Callbacks read `(:first-name (:data p))` under **either** recipe — camel-keyed
rows resolve on the normal camel path, literal kebab rows through the
[literal-key fallback](#callbacks-what-your-functions-receive-and-return). That
fallback is a *callback lookup* rule only — it does not rewrite row objects or
column fields, so it cannot rescue a keyword `:field` pointed at a kebab-keyed
row. Pick a row, pick the matching field.

Neither recipe is free: converting 100k rows measured ~600ms against ~9ms for
supplying JS directly ([ADR 0003](adr/0003-row-data-js-by-contract.md)), which
is why `#js` rows above are the primary path and these are the answer for
consumers who already hold CLJS data. `raw` is not a third recipe — it hands
AG Grid the CLJS collection unconverted, which is a way to render nothing.

## Callbacks: what your functions receive and return

Functions found in the options tree are auto-wrapped in both directions:

- **Params arrive as a lazy kebab-keyed bean** — `(:row-index p)`, `(:value p)`,
  `(:data p)`. It is a view, not a copy: only the keys you touch pay conversion,
  and the underlying JS (including row data) is reachable and never converted.
- **Return values run forward through the converter** — a keyword becomes a
  camel string, `{:font-weight "bold"}` becomes a JS object, scalars are free.

Keyword lookup on these beans follows one law: **camel when present, literal
otherwise**. `(:row-index p)` reads `rowIndex`; if the camelized property is
absent, the lookup falls back to the keyword's literal name, so
`(:first-name (:data p))` also reads a row that carries `"first-name"` — the
spelling bare `clj->js` produces. The decision is per object and per lookup
(presence, not truthiness: a present `false` or `nil` camel value still wins),
it applies recursively to nested objects and to objects inside arrays, and it
covers callbacks that receive row data directly. Camel priority means AG Grid's
own vocabulary and camel-keyed data never change meaning. Note this is a
*callback lookup* rule only — rendering is separate, so a kebab-keyed row
still needs a string column field (`{:field "first-name"}`) to show up in a
cell; see [If your rows are CLJS data](#if-your-rows-are-cljs-data) for both
row/field pairings.

Callback beans are a read view, not a write channel. `assoc`, `dissoc`,
`update`, and friends are ordinary CLJS collection operations over a snapshot
or clone — they never mutate the underlying AG Grid params, row, or
`RowNode.data`, and their results cross a non-raw callback return through the
normal EDN→JS converter, where keyword keys camelize (a map derived from a
`"first-name"` row comes back out as `firstName`). A callback that must mutate
an AG Grid object or preserve literal property names on return should use
`(ag/raw f)`, explicit JS objects and string keys, or unwrap the backing JS
object and mutate it through JS/API calls.

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

All of the above nudges — plus unknown-key typo detection and the field check —
are **dev-only** and compiled out of production builds entirely
(`goog.DEBUG` false dead-code-eliminates the validation code and the
key registry). They never reject or alter what AG Grid receives; the
open-surface guarantee holds. You get:

- **JS-by-contract nudge** — a row key (`:row-data`, `:pinned-top-row-data`, …)
  received a CLJS collection; it points here for the two recipes above.
- **Context nudge** — `:context` received a CLJS collection. Unlike rows it
  *does* convert, just lossily (keys camelize, keyword values become strings),
  so the nudge points at `raw` rather than at a recipe.
- **Class-rule key nudge** — a `:row-class-rules` / `:cell-class-rules` key
  written as a keyword whose name contains a `-`, or carrying a namespace; it
  emits a camelized CSS class no stylesheet rule matches. Runs at creation and on
  update. The other consumer-keyed options above are *not* checked: their names
  are cited from inside the options map, where a keyword key is correct and a
  mismatch is a different diagnostic — so silence there is not a clean bill of
  health.
- **XSS nudge** — a renderer function returned an HTML-looking string (AG Grid
  injects it via `innerHTML`); see [Cell rendering](cell-rendering.md).
- **Set / namespaced-keyword warnings** — as described above.
- **Typo warnings** — opt in with
  [[ag-grid-cljs.core/enable-dev-validations!]]; unknown keys get a kebab
  did-you-mean.
- **Field check** — a column's `:field` (or `:tooltip-field`) names a key your
  row data does not have, so the column renders blank. Warns once per field
  with a did-you-mean, at creation and whenever columns or rows change.

The field check and the class-rule key nudge need no opt-in — both are on in
every dev build, because neither consults the key registry. The gate exists
because typo detection tests your keys against a registry pinned to one AG Grid
version, and a consumer on a newer AG Grid would get false "unknown option"
warnings from that drift; the field check compares your columns against your own
rows, so it has nothing to drift against.

The field check reports the **camel string AG Grid is looking up**, not your
kebab source:

```
[ag-grid-cljs] column field "fristName" is not a key in the row data — did you mean "firstName"?
```

A `:value-getter` on the ColDef suppresses the `:field` half (the getter
supersedes the field) but never the `:tooltip-field` half, which AG Grid reads
straight off the row regardless. A dotted field is checked one segment deep, so
legitimately sparse nested data stays quiet.

Beyond the kebab layer, AG Grid has its own checks — deprecated options with
their replacement, types, option dependencies, row-model support — and they need
no call from you: `create-grid!` registers AG Grid's `ValidationModule` itself in
`goog.DEBUG` builds, and nothing in production. Those warnings name camel
(`enableRangeSelection`), so an unknown key can warn twice — once in your kebab
spelling, once in AG Grid's. Deprecations are AG Grid's exclusively: the wrapper
used to duplicate them over a narrower set of keys and no longer does.

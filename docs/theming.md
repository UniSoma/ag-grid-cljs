# Theming

AG Grid v33+ styles grids through the **Theming API**: a JS theme object passed
as the `:theme` grid option. The wrapper ships **no theming code** in v1 — no
builder, no params helper (ADR 0013). It does not need any: a theme object is an
opaque JS value, so `:theme` rides the
[conversion boundary](options-and-conversion.md) untouched (non-CLJS values pass
through verbatim, ADR 0005). `:theme` is a known option key, so it still gets
dev-mode validation for free. This page is the whole recipe.

## Default theme

Omit `:theme` entirely and you get AG Grid's default (Quartz). Nothing to
configure:

```clojure
(-> (ag/options)
    (ag/with-columns [...])
    (ag/with-row-data rows))
```

## Customizing with `.withParams`

Import a base theme from AG Grid and build a variant with `.withParams` — this
is **raw interop past the conversion boundary**, so the param object is a `#js`
map with **camelCase** keys (the kebab→camel law does not apply here — you are
already in JS):

```clojure
(ns my.app
  (:require [ag-grid-cljs.core :as ag]
            ["ag-grid-community" :refer [themeQuartz]]))

(def my-theme
  (.withParams themeQuartz #js {:accentColor "red"
                                :fontFamily  "Inter, sans-serif"}))

(-> (ag/options)
    (assoc :theme my-theme)          ; opaque JS value, passes through verbatim
    (ag/with-columns [...]))
```

## Dark mode

Two routes, both plain interop. Set dark parameters via `.withParams` on a base
theme, or compose AG Grid's `colorSchemeDark` part with `.withPart`:

```clojure
(ns my.app
  (:require ["ag-grid-community" :refer [themeQuartz colorSchemeDark]]))

;; compose the dark color-scheme part
(def dark-theme (.withPart themeQuartz colorSchemeDark))

;; or set dark params directly
(def dark-theme
  (.withParams themeQuartz #js {:backgroundColor "#1a1a1a"
                                :foregroundColor "#e0e0e0"}))
```

Assign whichever you build to `:theme` exactly as above. Switching themes at
runtime is an ordinary options update — pass the new theme object through
[[ag-grid-cljs.core/update-grid!]].

## The `theme: "legacy"` escape hatch

To fall back to the pre-v33 CSS-files approach, set `:theme` to the string
`"legacy"` and import the stylesheets through your own bundler:

```clojure
(-> (ag/options)
    (assoc :theme "legacy"))         ; keyword would camelize; a string is verbatim

;; then, via your bundler:
;;   (:require ["ag-grid-community/styles/ag-grid.css"]
;;             ["ag-grid-community/styles/ag-theme-quartz.css"])
```

Note the value is the **string** `"legacy"`, not the keyword `:legacy`: a
keyword would camelize by the conversion law, and `"legacy"` is a literal AG
Grid needs verbatim — this is the same
[keyword-vs-string distinction](options-and-conversion.md#keywords-are-ag-grids-vocabulary)
as everywhere else. The Theming API is AG Grid's default from v33+ and the story
we recommend; legacy CSS is only the escape hatch.

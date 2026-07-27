---
id: agd-01kygjg6avt2
title: JS-by-contract warning gives an incomplete clj->js recipe
status: open
type: bug
priority: 2
mode: afk
created: '2026-07-27T01:19:47.544644867Z'
updated: '2026-07-27T01:59:37.752320929Z'
tags:
- docs
- conversion
acceptance:
- title: The warning no longer presents bare clj->js as a complete row-data recipe
  done: false
- title: The docs show camel-keyed rows with keyword fields and literal kebab rows with string fields
  done: false
- title: The docs state that callback literal-key fallback does not change AG Grid field resolution
  done: false
- title: Both documented row/field pairings render correctly in browser tests
  done: false
- title: The literal kebab recipe returns the same value through a wrapped callback
  done: false
- title: docs/options-and-conversion.md and the warning use consistent guidance
  done: false
links:
- agd-01kygja77mxj
deps:
- agd-01kygja77mxj
---

## Description

`convert.cljs` currently warns:

```text
<prop> received a CLJS collection; row data is JS by contract — pass a JS array (or wrap with raw / clj->js if intentional)
```

Bare `clj->js` is not a complete recipe. It turns `{:first-name "Ada"}` into a row carrying `"first-name"`, while a keyword field `{:field :first-name}` emits `"firstName"`. AG Grid then renders a blank cell and the field check reports the mismatch.

ADR 0018's literal-key fallback fixes callback access, not AG Grid field resolution. The warning and `docs/options-and-conversion.md` must describe complete, internally consistent row shapes rather than naming `clj->js` alone.

## Design

Document two supported recipes:

1. **Camel-keyed rows:** convert CLJS data with a camelizing `:keyword-fn`; use keyword fields such as `{:field :first-name}`. This has zero fallback requirement for AG Grid's own field reads.
2. **Literal kebab-keyed rows:** bare `clj->js` is acceptable when column fields are strings such as `{:field "first-name"}`. Wrapped callbacks can use `(:first-name ...)` through ADR 0018's camel-first literal-key fallback.

Keep the console warning short and point to the conversion documentation for code. The article should include copyable examples for both recipes and state explicitly that callback fallback does not rewrite column fields or row objects. `raw` remains valid when the consumer intentionally wants the original CLJS collection passed through, but it is not a row conversion recipe.

Verify both documented row/field pairings in the browser. The literal recipe should also prove that the rendered value and callback value agree.
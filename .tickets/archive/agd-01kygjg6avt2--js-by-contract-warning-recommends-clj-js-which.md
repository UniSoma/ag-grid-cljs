---
id: agd-01kygjg6avt2
title: JS-by-contract warning gives an incomplete clj->js recipe
status: closed
type: bug
priority: 2
mode: afk
created: '2026-07-27T01:19:47.544644867Z'
updated: '2026-07-27T20:12:13.225910088Z'
closed: '2026-07-27T20:12:13.225910088Z'
tags:
- docs
- conversion
acceptance:
- title: The warning no longer presents bare clj->js as a complete row-data recipe
  done: true
- title: The docs show camel-keyed rows with keyword fields and literal kebab rows with string fields
  done: true
- title: The docs state that callback literal-key fallback does not change AG Grid field resolution
  done: true
- title: Both documented row/field pairings render correctly in browser tests
  done: true
- title: The literal kebab recipe returns the same value through a wrapped callback
  done: true
- title: docs/options-and-conversion.md and the warning use consistent guidance
  done: true
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

## Notes

**2026-07-27T20:12:13.225910088Z**

Split the data-carrying nudge (rows point at the Options and conversion article; :context gets its own raw-pointing message), exposed ag/kebab->camel as the camelizing :keyword-fn, and documented both row recipes as row-shape/field pairings with a pairing table, the keyword-values caveat, ADR 0003's conversion cost, and the statement that ADR 0018's fallback cannot rescue a mismatched field. New row-recipes browser suite runs both documented calls verbatim (render + callback read); node test pins both warning texts. Commit 1aea82e. node 75 tests/235 assertions green; browser 11 tests/37 assertions green.

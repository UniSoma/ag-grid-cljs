# 0013. Theming: no theming code in v1, docs recipe only

- Status: accepted, 2026-07-22
- Origin: knot ticket agd-01ky55n5xn5c (tickets are ephemeral; this record is self-contained)

The wrapper ships no theming code in v1: `:theme` rides the conversion boundary as an opaque JS value (already true by contract per ADR 0005, zero special-casing), the Theming API (v33+) is the assumed default with legacy CSS files as the escape hatch, and the entire deliverable is one docs recipe covering the common cases.

## Context

AG Grid v33+ styles grids through the Theming API — JS theme objects (`themeQuartz`, `.withParams(...)`, `.withPart(...)`) passed as the `theme` grid option — with the pre-v33 CSS-files approach demoted to a `theme: "legacy"` opt-in. The wrapper had to decide: Theming API vs legacy CSS files as the primary story; whether to offer theming sugar (a builder? a params-coercion helper?) or stay hands-off; how theme objects interact with the conversion boundary; and what the docs tell a consumer to do for the common cases (default theme, dark mode, custom params).

## Decision

1. **No theming code in v1.** No `with-theme` builder, no params-coercion helper — this honors the builder-catalog v1 deferral (ADR 0009 deferred theming post-v1).

2. **Conversion boundary: already true by contract.** A theme object (`themeQuartz`, a `.withParams(...)` result) is an opaque JS value, not a CLJS map, so `:theme` rides the conversion boundary untouched — per ADR 0005, non-CLJS values pass through verbatim. Zero special-casing. `:theme` is a known GridOptions key in the registry (ADR 0007), so dev-mode validation and `:initial?` classification come for free.

3. **Theming API is the assumed default** (v33+; we target v34+). Legacy CSS files are the escape hatch, not the primary story.

4. **Deliverable: one docs recipe** covering:
   - **Default theme** — no `:theme` key means quartz.
   - **Customization via `.withParams` in plain interop** — `(.withParams themeQuartz #js {:accentColor "red"})`; camelCase param names, since it is raw interop past the boundary.
   - **Dark mode** — `.withParams` on a dark base theme, or composing the `colorSchemeDark` part via `.withPart`.
   - **The `theme: "legacy"` escape hatch** — string value; the consumer imports `ag-grid.css` + `ag-theme-*.css` through their bundler.

   The recipe's home is `docs/theming.md` in the cljdoc article tree, per ADR 0014.

## Considered options

- **Option B: a kebab-keyed `.withParams` coercion sugar** — considered and rejected for v1: it meets the coerce bar, but the builder catalog (ADR 0009) already deferred theming, and plain interop is acceptable.
- **A `with-theme` builder** — rejected for v1 under the same catalog deferral.
- **Legacy CSS files as the primary story** — rejected: the Theming API is AG Grid's default from v33+ and the wrapper targets v34+; CSS files remain only as the documented escape hatch.

## References

- ADR 0005 — conversion boundary (non-CLJS values pass through verbatim)
- ADR 0007 — generated key registry (`:theme` validation and `:initial?` classification for free)
- ADR 0009 — builder catalog v1 (theming sugar deferred post-v1)
- ADR 0014 — docs/cljdoc strategy (home of the theming recipe)

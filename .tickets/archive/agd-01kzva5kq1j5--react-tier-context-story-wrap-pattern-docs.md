---
id: agd-01kzva5kq1j5
title: 'React tier context story: wrap-pattern docs, uncaught-cell-error diagnostic, raw-context browser test'
status: closed
type: task
priority: 2
mode: afk
created: '2026-08-12T15:41:32.769433372Z'
updated: '2026-08-12T15:49:36.767491586Z'
closed: '2026-08-12T15:49:36.767491586Z'
tags:
- react
- docs
- diagnostics
acceptance:
- title: framework-composition.md documents the provider-wrap recipe with its cost and rebuild-stability note; cell-rendering.md cross-links it
  done: true
- title: react-renderer roots warn once per process per distinct uncaught render error in dev builds, with provider-shaped hint; prod behavior unchanged
  done: true
- title: browser test pins context detachment (bare cell) and context delivery (wrap pattern) using a raw createContext
  done: true
links:
- agd-01kzva6hgxq5
---

## Description

Outcome of the grilled field-consumer submission (wip/ask.md, Agilis/Mantine): per-cell React roots are detached, so provider-based cell content (Mantine/MUI/Chakra/etc.) throws and paints an empty cell. The detachment is already documented (framework-composition.md 'detached root'), but the severity for design-system components and the remedy are not. Decision: NO :wrap option on react-renderer — it is compositionally identical to consumer-side wrapping, adds zero capability, and fails the ADR 0009-style admission bar; ADR 0012's precedent (documented one-liner, not API) applies. This ticket ships the documented pattern plus the diagnostic plus the pinned contract.

## Design

1) Docs: a 'Design-system components in cells' section in framework-composition.md (cross-linked from cell-rendering.md tier 3): the provider-wrap recipe (namespace-level def wrapping the render fn output in the design system's provider, style-injection props off — keeps ADR 0021 rebuild-stability, the consumer's half), its cost (each visible cell mounts a provider; cells manage what they should consume), and a pointer to the portal tier for provider-based consumers once it lands. 2) Diagnostic: dev-gated onUncaughtError passed at createRoot time in react.cljs, warning on ANY uncaught cell render error (never a false positive — the observable is always an empty cell, ADR 0017 §8), with a provider-shaped hint appended when the message matches 'Provider was not found'-class text, pointing at the wrap pattern and the portal tier. Period per ADR 0022: once per process, site ::react/cell-render-error, discriminator = error message, via impl.warn/warn-once!. Prod builds keep React's default (reportError) — do not pass the option when goog.DEBUG is false. 3) Browser test: raw createContext (no design-system dep) pinning both halves: a context-consuming component in a bare react-renderer cell does NOT see an app-level provider value; the wrap pattern DOES deliver it. Fact-checks recorded during grilling: 'undocumented trap' was overstated (detachment is documented; severity/remedy are not) and 'surfaces nowhere in prod console' is wrong (React 19 default onUncaughtError is reportError, which logs; the UI silence claim stands).

## Notes

**2026-08-12T15:49:36.767491586Z**

All three deliverables shipped and verified. Docs: framework-composition.md gains 'Design-system components in cells' — provider-wrap recipe (namespace-level def, style-injection off), the ADR 0021 consumer's-half rebuild-stability note, the cost paragraph (cells manage what they should consume; error boundaries still don't reach in), and the portal-tier pointer; cell-rendering.md tier 3 cross-links it. Diagnostic: react.cljs passes onUncaughtError at createRoot only when goog.DEBUG (prod keeps React's reportError default), warning via warn-once! site ::react/cell-render-error keyed by error message, with the wrap-pattern hint appended on 'Provider was not found'-class messages. Browser test: react_context_test.cljs pins detachment (bare cell under an app-level raw-createContext provider sees the default, not the app value) and delivery (wrapped cell sees the wrap value), plus the diagnostic contract (two throwing cells => one prefixed warn with hint, fired under the site key, cell paints empty). Node suite 114/363 green; browser suite 23 tests/76 assertions green with the console-error tripwire quiet.

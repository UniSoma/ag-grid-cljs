---
id: agd-01kymx7yf4dy
title: Register ValidationModule in dev builds; drop the wrapper's deprecation branch
status: closed
type: task
priority: 3
mode: hitl
created: '2026-07-28T17:44:29.411989388Z'
updated: '2026-07-28T18:43:46.707059932Z'
closed: '2026-07-28T18:43:46.707059932Z'
tags:
- dx
- validate
acceptance:
- title: Deprecation branch removed from check-key!; unknown-key branch and its kebab did-you-mean untouched
  done: true
- title: create-grid! registers ValidationModule once, in goog.DEBUG builds, before createGrid
  done: true
- title: 'Orphans cleaned: block-deprecations, :deprs in block-spec and grid-spec, the deprs destructuring, the spec comment'
  done: true
- title: 'Registry codegen still emits :deprecated (decided: yes; reference table is independent of it)'
  done: true
- title: ADR 0020 written; pointer edits in ADR 0001:33, ADR 0007 section 5, ADR 0017 appendix
  done: true
- title: Docs and docstrings no longer claim deprecation warnings or ValidationModule-as-homework (7 files); CONTEXT.md Dev validations entry updated
  done: true
- title: 'Node suite: deprecation tests removed, unknown-key tests still green'
  done: true
- title: 'Browser suite: new test proves AG Grid''s deprecation warning reaches the console with no explicit registration'
  done: true
- title: Advanced-build DCE grep extended to assert ValidationModule does not survive
  done: false
links:
- agd-01kymx8m23sj
---

## Description

Audit of all nine wrapper checks against AG Grid 36.0.2 (recorded in the ADR 0017 appendix) found the deprecation branch of `check-key!` (validate.cljs:147-150) redundant and strictly narrower than AG Grid's own.

AG Grid's `ValidationService.processOptions` (community main.cjs.js:57570) emits `As of v{version}, {name} is deprecated. {message}` from two hand-maintained runtime tables: `GRID_OPTION_DEPRECATIONS` (26389) and `COLUMN_DEFINITION_DEPRECATIONS` (25490), via `_warnOnce` — so it dedupes like our own `warn-once!`.

Measured coverage, diffing camel names:

| | keys |
|---|---|
| AG Grid runtime deprecation tables | 106 |
| our registry `:deprecated` | 32 |
| ours minus theirs | 1 — `reactiveCustomComponents` |

Message content is the same replacement text from the same upstream source. The gap is explained by origin: our registry records `@deprecated` tsdoc tags found during codegen, AG Grid's tables are hand-maintained and wider. The one key we uniquely flag is a framework-adapter option with no meaning for a vanilla-core wrapper (ADR 0012).

The ticket originally read as a straight deletion. Grilling found the premise "both are gated, so they are reached under the same conditions" false — the two gates are INDEPENDENT opt-ins (`enable-dev-validations!` vs `register! ValidationModule`), and the docs frame the latter as advice for a different class of check, so deleting the branch alone would drop deprecation coverage for anyone who took the docs at their word. Resolving that asymmetry in docs is not enough (prose can be ignored), which turns this ticket into a behavior change plus a removal.

## Design

## 1. `create-grid!` registers `ValidationModule` in dev builds (the larger half)

Under `^boolean goog.DEBUG`, once, before `createGrid`. Always-on, NOT behind `enable-dev-validations!`.

Rationale: ADR 0017 §1's gating criterion is *registry drift* — our checks test consumer keys against a registry pinned to one AG Grid version. AG Grid's validation tables ship inside the consumer's own AG Grid, so they cannot drift against it; `ValidationModule` is the most drift-free instrument in the picture, more so than the always-on field check. By the project's own stated rule it earns always-on, and inheriting `enable-dev-validations!`'s gate would be borrowing a gate for a problem it does not have.

Consequences of always-on over gated:
- Deprecation coverage **improves** rather than relocating: 32 keys opt-in becomes 106 keys in every dev build.
- The sibling ticket's blocker fully dissolves. Under a *gated* registration it would not: the initial-only warning in `update-grid!` is always-on, so trading it for a gated upstream warning would still leave a hole.
- Every dev build gets upstream type/dependency/row-model warnings unbidden, and unknown keys warn twice (ours kebab, AG Grid's camel). Accepted — same posture the field and class-rule checks already carry, with a stronger case since this output cannot be wrong.

Placement in `create-grid!` (not a top-level side-effecting `defonce`) makes the "must register before the first grid" ordering constraint structurally impossible to violate rather than merely documented.

Bundle cost is ~zero: `ag-grid-community` ships as one bundled file with `ValidationService` (57570) in the same file as `createGrid` (911), and ADR 0001:43 already records that consumers ship the whole ~1 MB community bundle regardless of what they register. The reference sits inside `goog.DEBUG`, so `:advanced` eliminates it and ADR 0007 §1's DCE story holds.

Unconditional — no detection of prior registration (`registerModules` is additive and idempotent; reading `ModuleRegistry` state is an unstable private surface) and no opt-out arg (speculative; the escape hatch is not calling `enable-dev-validations!` and registering what you want yourself).

Verified `ValidationService` (57400-57700) contains no `throw`: registration only logs, never changes what the grid does. That is the distinction ADR 0020 rests on — a *diagnostics* module is ours to register in dev; a *capability* module stays the consumer's call.

## 2. Remove the deprecation arm of `check-key!`

The `if` collapses to `when-not (contains? camels prop)`; the `camels` hit becomes a plain no-op. Unknown-key branch and its kebab did-you-mean untouched — the ADR 0017 appendix keeps it for two deltas AG Grid cannot supply (kebab naming, position-awareness against a flat merged `colDefPropertyMap`).

Orphans: `block-deprecations` (111-112), `:deprs` in `block-spec` (116) AND in the inline `grid-spec` (129), the `deprs` destructuring (144), the `:deprs` line of the spec comment (103-106).

## 3. Registry codegen keeps emitting `:deprecated`

DECIDED — and the ticket's original reason for keeping it was wrong. `docs/reference/ag-grid-options.md` is built by `keyTable` (extract.mjs:199-203) reading the in-memory `entry` objects, a SIBLING of `ednEntry` (143), not a consumer of it; deleting line 143 would leave the markdown byte-identical. ADR 0014 never mentions `:deprecated`, so cljdoc does not feed on it either. After this change the key has zero consumers.

Kept anyway on ADR 0007:41's actual rationale — the registry is dev-warnings + docs authority only and DCE's away entirely, so "richness costs only dev-bundle and repo size" — plus the unbuilt `(registry/reference-table)` REPL helper noted at 0007:53. Withholding one field of the extracted shape from the EDN would be a special case contradicting the documented shape in `registry.cljs:18-19` and `extract.mjs:179-180`.

## 4. Record

New **ADR 0020**, "the wrapper registers ValidationModule in dev builds" — clears all three bars: hard to reverse (public behavior consumers build habits on), surprising (it contradicts ADR 0001:33 and a reader finding `registerModules` inside `create-grid!` will ask why), a real trade-off (gated → always-on; consumer-owned registration rejected for diagnostics specifically). Carries the drift argument.

Three pointer edits, not rewrites — ADR 0001:33 (one clause noting the dev-only carve-out; the decision text is a dated record and stands), ADR 0007 §5 (its claim that AG Grid's deprecation warnings are "off by default on v36+" stops being true of our builds), ADR 0017 appendix ("tracked for removal" becomes "removed, see 0020"; the initial-only bullet's asymmetric-gate caveat is marked dissolved).

`CONTEXT.md` gets the "Dev validations" entry edited only — no new term. The diagnostics-vs-capability distinction is rationale for one decision with one instance; the glossary is vocabulary and must stay free of implementation detail. If a second diagnostics-only module appears, it earns a name then.

## 5. Tests

No node test for the registration. It would need a seam (`create-grid!` needs a live element, so `core_test` cannot reach it) around a two-line dev-only side effect whose failure mode is harmless, since `registerModules` is idempotent.

The load-bearing test is one **browser** test instead, following the ADR 0019 precedent of pinning an AG-Grid-not-us premise with a single browser assertion: create a grid with `{:enable-range-selection true}` and NO explicit registration, capture `console.warn` with the `field_check_test.cljs:13-24` helper pattern, assert a line matching `As of v32.2 … deprecated`. That one test proves the registration happened, the upstream warning reaches the console, and it carries the replacement — the whole justification for the deletion. It goes red on an AG Grid bump that moves the tables, which is the signal we want.

The Playwright tripwire (`test/browser/run.mjs:98-99`) fires only on `console.error`, so upstream warnings cannot break the browser suite. `_error(200, missingModule)` for icons needing an unregistered module is the one new error path; if it surfaces, fix the fixture rather than allowlisting — the warning would be true.

Extend the `:advanced` DCE grep to assert `ValidationModule` does not survive `goog.DEBUG false`; this is the first time `core` names a module object.

## 6. Docs surface

Seven files claim deprecation warnings as ours, or `ValidationModule` as the consumer's homework: `core.cljs:260` and `265-273` (incl. the 3-line sample), `validate.cljs:8`, `getting-started.md:101-105`, `options-and-conversion.md:242, 263-265, 290`, `docs/spec.md:13`, `CONTEXT.md:44`. `create-grid!`'s docstring gains a line. `CONTEXT.md:36` (registry shape) stays — `:deprecated` is still in the shape.

## Notes

**2026-07-28T18:43:46.707059932Z**

create-grid! registers AG Grid's ValidationModule once under goog.DEBUG before createGrid, always-on rather than behind enable-dev-validations! (ADR 0017 §1's drift criterion does not apply to tables shipping inside the consumer's own AG Grid). The wrapper's deprecation branch and its orphans are gone; unknown-key branch and kebab did-you-mean untouched. Registry codegen still emits :deprecated. Reasoning in new ADR 0020; pointer edits in ADR 0001, ADR 0007 §5 (note placed ABOVE the stale sentence, not only after it) and the ADR 0017 appendix, committed separately as the audit record it is. Seven doc/docstring surfaces updated plus getting-started's 'Register modules' section, which also claimed the consumer owns every registration. Node suite 83 tests / 254 assertions / 0 failures (the two deprecation tests became one asserting silence); browser suite 13 tests / 42 pass / 0 fail, up from 12/41 on clean main, no new console.error tripwires. New browser test verified red with the registration commented out. LAST AC LEFT UNCHECKED, deliberately: there was no committed DCE grep to extend (ADR 0017:48 and 0019:97 describe it as manual), and a hit-count tripwire would fire on any AG Grid release that reworded a message. Verified manually instead — 4 ValidationModule hits, every one inside AG Grid's own bundled npm code, and decisively the :advanced release bundle is byte-identical to the parent commit's. Both checks and their reproduction commands are recorded in ADR 0020's consequences.

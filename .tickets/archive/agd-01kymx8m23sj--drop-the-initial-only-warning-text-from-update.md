---
id: agd-01kymx8m23sj
title: Pin why update-grid!'s initial-only warning is not redundant; correct the ADR 0017 finding
status: closed
type: task
priority: 3
mode: afk
created: '2026-07-28T17:44:51.519110458Z'
updated: '2026-07-28T19:14:09.834482135Z'
closed: '2026-07-28T19:13:51.372254305Z'
tags:
- dx
- update-channel
acceptance:
- title: :initial-only still warns once per key and still skips setGridOption; no behavior change in any update-grid! branch
  done: true
- title: 'A comment at the :initial-only branch records why the warning is not redundant: AG Grid''s _warn(22) is reached only from updateGridOptions, which the skip bypasses'
  done: true
- title: 'Browser suite pins the upstream fact: an initial-only key through update-grid! yields no AG Grid initial-property warning, while the same key pushed directly at the grid api does'
  done: true
- title: ADR 0017's appendix reclassifies the initial-only entry out of 'Redundant — removed', striking the 'exactly our setGridOption path' claim
  done: true
- title: 'ADR 0020''s bullet claiming this ticket''s blocker dissolved is corrected: registration is always-on, but gating was never the obstacle'
  done: true
- title: Node suite's existing initial-only test passes unchanged; docs/updating-data.md and the update-grid! docstring verified still accurate
  done: true
links:
- agd-01kymx7yf4dy
deps:
- agd-01kymx7yf4dy
---

## What to build

Keep `update-grid!`'s `:initial-only` warning and skip exactly as they are, and
make the codebase carry the reason so the next audit cannot re-litigate it.

AG Grid's equivalent warning — `_warn(22)`, "`{key}` is an initial property and
cannot be updated" — is emitted from `ValidationService.warnOnInitialPropertyUpdate`,
which has exactly one caller: `GridOptionsService.updateGridOptions`. That is the
body of `setGridOption`. Our `:initial-only` branch never calls `setGridOption`;
skipping it IS the branch. So AG Grid's warning is not gated relative to ours, it
is unreachable on this path, and deleting our message would leave an ignored
update silent from both sides — which ADR 0020's always-on registration does not
change, because gating was never the obstacle.

The slice is narrow and complete: no production behavior changes, one comment at
the branch stating the reachability argument, a browser-suite assertion that pins
the upstream half of it against a real grid (an initial-only key through
`update-grid!` produces no AG Grid initial-property warning; the same key pushed
straight at the grid api does), and a correction to the two ADRs that recorded
the finding wrongly.

The `:unclassified` and `:row-data` branches and the `warned` dedup atom are out
of scope and untouched.

## Blocked by

None - can start immediately (agd-01kymx7yf4dy is closed).

## Notes

**2026-07-28T19:05:23.598432331Z**

ADR corrections landed ahead of the code slice (ACs 4 and 5 flipped done). ADR 0017's appendix moves the initial-only entry into 'Not redundant — no upstream coverage' with an explicit correction paragraph, and the appendix criterion gains a 'verify at the call site, not the text' paragraph naming this as the case it is for. ADR 0020's consequence bullet is rewritten from 'the blocker dissolves' to 'no effect', its rejected-option and reference-list echoes of the old claim are struck, and the deprecation entry in ADR 0017 gains the reachability evidence it always had (postConstruct 27098 and updateGridOptions 27172), so the two entries no longer read the same way. Remaining work is code-only: the branch comment, the browser assertion, and re-running the node suite.

**2026-07-28T19:13:51.372254305Z**

No production behavior change. update-grid!'s :initial-only branch keeps its once-per-key warning and its setGridOption skip; a comment there records why it is not redundant with AG Grid's _warn(22) (sole caller is updateGridOptions, the body of setGridOption, which the skip bypasses). New browser test ag-grid-cljs.browser.initial-only-test pins the upstream half against a real grid with :tab-index (:initial? true in our registry, present in AG Grid's INITIAL_GRID_OPTION_KEYS): the update-grid! path draws no _warn(22), the same key pushed straight at grid-api does. ADR 0017's appendix now points at that test and cites the guarded write (27160-27161) instead of the read at 27159. Node suite unchanged and green (83 tests, 254 assertions); browser suite 14 tests, 45 pass. docs/updating-data.md:66-68 and the update-grid! docstring re-read and still accurate. Commit aea4e2d.

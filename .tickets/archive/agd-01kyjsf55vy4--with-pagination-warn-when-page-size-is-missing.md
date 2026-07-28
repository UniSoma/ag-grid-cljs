---
id: agd-01kyjsf55vy4
title: 'with-pagination: warn when :page-size is missing from :page-size-selector'
status: closed
type: task
priority: 3
mode: hitl
created: '2026-07-27T22:00:02.491099594Z'
updated: '2026-07-28T17:29:49.228203417Z'
closed: '2026-07-28T17:29:49.228203417Z'
tags:
- builders
- dx
acceptance:
- title: Dev-warns when :page-size-selector is a collection that does not contain :page-size
  done: false
- title: No warning when the selector is true/false or when :page-size is absent
  done: false
- title: Docstring notes the rule; node suite covers both branches
  done: false
---

## Description

with-pagination already owns one rule for this key pair — the :auto-page-size x :page-size mutual exclusion (core.cljs:171-180). AG Grid states a second rule over the same two keys: errors 94 and 95, "'paginationPageSize=N', but N is not included in paginationPageSizeSelector=[...]" and "Either set 'paginationPageSizeSelector' to an array that includes N or to 'false' to disable the page size selector" (validation/errorMessages/errorText.d.ts:198-202). A page size absent from the selector array breaks the selector.

Same builder, same dev-warn style, a few lines. Only applies when :page-size-selector is a vector (true/false disable the check).

## Notes

**2026-07-28T17:29:49.228203417Z**

wontdo: AG Grid already warns on this, ungated and on first paint. PageSizeSelectorComp.reloadPageSizesSelector (community main.cjs.js:52402-52413) computes shouldAddAndSelectEmptyOption = !pageSizeOptions.includes(paginationPageSizeOption) and emits _warn(94) — 'paginationPageSize=25, but 25 is not included in paginationPageSizeSelector=[20, 50, 100]' (text at 56883); _warn(95) additionally fires only when the selector was left at true, so the ticket's array case gets 94 alone. No interaction needed to reach it: updateVisibility -> toggleSelectDisplay(true) -> reloadPageSizesSelector, gated on shouldShowPageSizeSelector = !paginationAutoPageSize && selector !== false, which is exactly this ticket's condition. Without ValidationModule it still prints via the minifiedLog fallback (getErrorParts, main.cjs.js:925) as 'warning #94 Visit .../errors/94?pageSizeOptions=20,50,100&paginationPageSizeOption=25', values included.

AG Grid's check is also strictly WIDER than the proposed builder check: it runs on effective values, so it catches {:page-size 25 :page-size-selector true} against the default [20 50 100] (a case this ticket explicitly excluded), and it sees :pagination-page-size however it arrived — plain assoc, a second with-pagination call, or a set-grid-option update — where a builder only sees the single config map passed to it. A builder-side copy would be narrower, worse-worded, and would drift if AG Grid changes its default selector.

Contrast with the rule with-pagination already owns: AG Grid silently ignores paginationPageSize when paginationAutoPageSize is on (main.cjs.js:52073, 53111) with no _warn anywhere, so that warning covers a genuinely silent case and stays. Same premise failure as agd-01kymfv9sh68.

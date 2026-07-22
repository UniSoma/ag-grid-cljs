---
id: agd-01ky0ed8fw7v
title: 'Walking skeleton: Enterprise smoke test (modules, license, range selection)'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-20T19:00:26.234760194Z'
updated: '2026-07-22T14:54:59.221327591Z'
closed: '2026-07-22T14:54:59.221327591Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0ed83xww
- agd-01ky0eck3myz
---

## Description

## Question

Prove Enterprise works through the wrapper: register enterprise modules per the module-registry research, set a license key (trial/dev), and exercise one enterprise feature — cell range selection with fill handle (the with-range-fill inspiration case) — from the builder API. Confirms the wrapper imposes nothing that blocks Enterprise.

## Notes

**2026-07-22T14:54:51.961586527Z**

**2026-07-22T14:54:59.221327591Z**

Enterprise proven through the wrapper (5th/final risk point retired). New opt-in ag-grid-cljs.enterprise ns (set-license-key!) + core/with-cell-selection builder; consumer registers AllEnterpriseModule via register!. Headless-verified v36 both license states: cell range selection + fill handle work identically with and without a license — no-license path only logs AG Grid's console error, nothing throws. License injected at compile time from AG_GRID_LICENSE (dev macro), lands only in gitignored build output; never committed.

## Resolution — Enterprise works through the wrapper, unblocked. 5th/final risk point retired.

Proven end-to-end by headless-Chromium probe (dev build, AG Grid v36.0.1, both license states). The wrapper imposes **nothing** that blocks Enterprise.

### What was built
- **Library (permanent):** new opt-in `ag-grid-cljs.enterprise` namespace with a single fn `set-license-key!` (wraps `LicenseManager.setLicenseKey`). Core never imports `ag-grid-enterprise` — Community-only consumers pay nothing. Confirms the namespace-layout decision (agd-01ky0m0btmrp: opt-in `enterprise` ns with `set-license-key!`) and the module-registry research (agd-01ky0eck3myz: consumer owns registration via `register!`).
- **Builder (permanent):** added `core/with-cell-selection` per locked catalog v1 (agd-01ky0edx5mfs) — the `with-range-fill` inspiration case. Takes the v32.2+ cellSelection object form; `{:handle {:mode "fill"}}` turns on the fill handle. Rides the conversion boundary unchanged (`:cell-selection`→`cellSelection`, nested kw keys→camel, "fill" string verbatim).
- **Dev harness (throwaway, gitignored output):** `src/dev/ag_grid_cljs/dev/enterprise_app.cljs` + `enterprise.html` + `:enterprise-app` shadow build. Registers `AllEnterpriseModule` via `core/register!`, sets license, mounts a grid with cell range selection + fill handle via the builder API.

### License handling (no key in committed source)
Injected at COMPILE time via a dev-only macro `ag-grid-cljs.dev.license/license-key` that reads `System/getenv "AG_GRID_LICENSE"` and inlines it. The key lands ONLY in the gitignored `src/dev/public/js-enterprise/` output — verified `git check-ignore` + staged-tree grep: zero license bytes in anything git tracks. On CI (env unset) the macro expands to `""`.

### Verification (headless probe, mouse-drag range select + fill-handle DOM check)
| | rows | cellSelection opt | licenseValid | fill handle | range cells selected | license console errors | page errors |
|---|---|---|---|---|---|---|---|
| **With license (local)** | 5 | true | true | present | 6 | 0 | 0 |
| **No license (CI)** | 5 | true | false (missing) | present | 6 | 2 ("License Key Not Found") | 0 |

Confirms the user's expectation exactly: without a license the Enterprise feature works identically — only a console error is logged, nothing throws or degrades. `npm test` (14 tests / 38 assertions) still green after the `with-cell-selection` core addition.

Probe script kept out of the repo (scratchpad), consistent with prior skeleton tickets.

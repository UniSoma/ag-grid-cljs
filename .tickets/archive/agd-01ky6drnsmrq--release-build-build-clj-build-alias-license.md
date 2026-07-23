---
id: agd-01ky6drnsmrq
title: 'Release build: build.clj + :build alias + LICENSE, produces a correct source-only jar'
status: closed
type: task
priority: 1
mode: afk
created: '2026-07-23T02:44:38.324387334Z'
updated: '2026-07-23T02:56:59.290972737Z'
closed: '2026-07-23T02:56:59.290972737Z'
tags:
- release
- v0.1
- build
acceptance:
- title: clojure -T:build jar produces a jar under target/ containing src/main sources + LICENSE + THIRD-PARTY.md at root
  done: false
- title: Generated pom declares groupId io.github.unisoma, artifactId ag-grid-cljs, version 0.1.0-SNAPSHOT, MIT license, and ZERO dependencies
  done: false
- title: pom <scm><tag> is the current commit SHA (SNAPSHOT) and the <scm> block survives cljdoc's Jsoup parser (no self-closing collapse)
  done: false
- title: MIT LICENSE file exists at repo root with UniSoma copyright
  done: false
---

## Description

Implementation of ADR 0016 (docs/adr/0016-clojars-release-engineering-v0.1.md) — the ADR is the spec. Deliver a build that produces a correct, publishable source-only jar locally.

Scope: a build.clj using tools.build + slipset/deps-deploy with clean/jar/install/deploy tasks (deploy wiring may be stubbed here; exercised in the deploy ticket). Source-only jar (:src-dirs ["src/main"], no AOT). Pom written programmatically via b/write-pom: MIT via :pom-data; ZERO Maven deps via a pom-basis built from empty deps; hardcoded SCM (github.com/UniSoma/ag-grid-cljs) with tag = built commit SHA for -SNAPSHOT else v<version>; expand-empty-elements! post-processing so cljdoc's Jsoup parser keeps the <scm> block. Version literal "0.1.0-SNAPSHOT" in build.clj. Add the :build alias to deps.edn (io.github.clojure/tools.build + slipset/deps-deploy). Create an MIT LICENSE file (UniSoma copyright) — none exists today. Jar bundles LICENSE + THIRD-PARTY.md at its root.

Out of scope (ADR 0016): no deps.cljs, no release-check, no CI publish leg, no GPG, no CHANGELOG.

## Notes

**2026-07-23T02:56:59.290972737Z**

build.clj (tools.build+deps-deploy, clean/jar/install/deploy) produces a source-only jar: src/main + LICENSE + THIRD-PARTY.md at root; pom is MIT-only, zero Maven deps (empty pom-basis), SCM <tag>=commit SHA for SNAPSHOT, and expand-empty-elements! now genuinely rewrites <foo/>→<foo></foo> (was a no-op via data.xml indent-str — caught in review) so no empty element collapses under cljdoc's Jsoup. :build alias added; MIT LICENSE at root. Verified: jar contents, zero deps, tag=HEAD SHA, npm test green (44 tests/124 assertions). Commit b9a38a4.

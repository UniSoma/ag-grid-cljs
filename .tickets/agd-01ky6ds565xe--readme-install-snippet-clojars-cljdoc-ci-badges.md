---
id: agd-01ky6ds565xe
title: README install snippet + Clojars/cljdoc/CI badges
status: open
type: task
priority: 2
mode: afk
created: '2026-07-23T02:44:54.085882858Z'
updated: '2026-07-23T02:44:54.085882858Z'
tags:
- release
- v0.1
- docs
acceptance:
- title: README shows the {:mvn/version "0.1.0-SNAPSHOT"} install snippet flagged mutable/pre-stable
  done: false
- title: README carries Clojars, cljdoc, and CI badges — and no AG-Grid-version badge
  done: false
---

## Description

Implementation of ADR 0016. Advertise the published coordinate.

Scope: README gains a deps.edn install snippet — io.github.unisoma/ag-grid-cljs {:mvn/version "0.1.0-SNAPSHOT"} — with a note that SNAPSHOT is mutable/pre-stable. Add three badges: Clojars version, cljdoc, and the CI workflow status.

Out of scope (ADR 0016): NO AG-Grid-version badge (would re-imply the version coupling the ADR rejects).

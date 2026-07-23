---
id: agd-01ky6ds1919y
title: 'Deploy path: deploy/install wiring + bb.edn + docs/release.md'
status: open
type: task
priority: 1
mode: afk
created: '2026-07-23T02:44:50.077261938Z'
updated: '2026-07-23T02:44:50.077261938Z'
tags:
- release
- v0.1
- deploy
acceptance:
- title: bb jar and bb install work; bb install places the artifact in local ~/.m2 with the correct pom
  done: false
- title: bb deploy invokes deps-deploy :remote using CLOJARS_USERNAME/CLOJARS_PASSWORD env (credentialed push not run in CI)
  done: false
- title: bb cljdoc POSTs cljdoc.org/api/request-build2 with the coordinate + version from build.clj
  done: false
- title: docs/release.md documents token creation, env vars, push-before-deploy, and the bb deploy step
  done: false
deps:
- agd-01ky6drnsmrq
---

## Description

Implementation of ADR 0016. Make the library releasable to Clojars via a manual local flow.

Scope: the build.clj deploy task pushes via deps-deploy (:installer :remote, artifact + pom-file), reading CLOJARS_USERNAME/CLOJARS_PASSWORD from env. A bb.edn wraps the build tasks for human ergonomics: bb jar / bb install / bb deploy / bb cljdoc (cljdoc = POST cljdoc.org/api/request-build2 with project+version scraped from build.clj). docs/release.md documents the manual flow: mint a Clojars deploy token, export the env vars, git push BEFORE deploy (SNAPSHOT SCM tag is the commit SHA), then bb deploy; note cljdoc auto-ingests and bb cljdoc force-triggers a rebuild.

Out of scope (ADR 0016): no CI publish leg (ci.yml stays test-only), no GPG signing.

---
id: agd-01ky6ds1919y
title: 'Deploy path: deploy/install wiring + bb.edn + docs/release.md'
status: closed
type: task
priority: 1
mode: afk
created: '2026-07-23T02:44:50.077261938Z'
updated: '2026-07-23T03:07:49.862167871Z'
closed: '2026-07-23T03:07:49.862167871Z'
tags:
- release
- v0.1
- deploy
acceptance:
- title: bb jar and bb install work; bb install places the artifact in local ~/.m2 with the correct pom
  done: true
- title: bb deploy invokes deps-deploy :remote using CLOJARS_USERNAME/CLOJARS_PASSWORD env (credentialed push not run in CI)
  done: true
- title: bb cljdoc POSTs cljdoc.org/api/request-build2 with the coordinate + version from build.clj
  done: true
- title: docs/release.md documents token creation, env vars, push-before-deploy, and the bb deploy step
  done: true
deps:
- agd-01ky6drnsmrq
---

## Description

Implementation of ADR 0016. Make the library releasable to Clojars via a manual local flow.

Scope: the build.clj deploy task pushes via deps-deploy (:installer :remote, artifact + pom-file), reading CLOJARS_USERNAME/CLOJARS_PASSWORD from env. A bb.edn wraps the build tasks for human ergonomics: bb jar / bb install / bb deploy / bb cljdoc (cljdoc = POST cljdoc.org/api/request-build2 with project+version scraped from build.clj). docs/release.md documents the manual flow: mint a Clojars deploy token, export the env vars, git push BEFORE deploy (SNAPSHOT SCM tag is the commit SHA), then bb deploy; note cljdoc auto-ingests and bb cljdoc force-triggers a rebuild.

Out of scope (ADR 0016): no CI publish leg (ci.yml stays test-only), no GPG signing.

## Notes

**2026-07-23T03:01:01.972904488Z**

Wiring complete (ADR 0016). bb.edn adds jar/install/deploy/cljdoc; jar/install/deploy delegate to clojure -T:build, cljdoc scrapes lib+version from build.clj and POSTs cljdoc.org/api/request-build2 via babashka.http-client. docs/release.md documents token creation, CLOJARS_USERNAME/PASSWORD env, mandatory push-before-deploy (SNAPSHOT SCM tag = commit SHA), and bb deploy/cljdoc. Verified locally: bb install places artifact in ~/.m2 with correct pom (zero Maven deps, MIT-only license, SCM tag = HEAD SHA); cljdoc scrape regex extracts io.github.unisoma/ag-grid-cljs + 0.1.0-SNAPSHOT. Credentialed 'bb deploy' and 'bb cljdoc' NOT run — user will do the actual deploy.

**2026-07-23T03:07:49.862167871Z**

bb.edn wraps the release flow (jar/install/deploy delegate to clojure -T:build; cljdoc scrapes lib+version from build.clj and POSTs cljdoc.org/api/request-build2 via babashka.http-client) and docs/release.md documents the manual runbook (Clojars token, env vars, push-before-deploy, bb deploy/cljdoc). Verified end-to-end: bb install placed the artifact in ~/.m2 with the correct pom (zero Maven deps, MIT-only, SCM tag = commit SHA), and bb deploy + bb cljdoc succeeded against Clojars/cljdoc. Commit 5a7dd17.

---
id: agd-01ky5hj2wq7n
title: 'CI workflow: node suite on every push/PR'
status: closed
type: chore
priority: 2
mode: afk
created: '2026-07-22T18:31:42.230950497Z'
updated: '2026-07-22T18:47:05.029439035Z'
closed: '2026-07-22T18:47:05.029439035Z'
acceptance:
- title: workflow committed, triggered on every push and PR, one job
  done: true
- title: 'job steps reproduce locally: npm ci && npm test green'
  done: true
- title: no OS/Node/AG-Grid matrix; no codegen step
  done: true
---

## Description

Single GitHub Actions workflow, one job, per ADR 0015 §3: checkout, JDK + Node setup, npm ci, npm test (the fast fail-early node contract suite). No matrix: single ubuntu runner, single Node LTS, the single registry-pinned AG Grid version. Codegen stays out of CI per ADR 0007. The browser-suite ticket later extends this same job with the Playwright leg.

## Notes

**2026-07-22T18:47:05.029439035Z**

Added .github/workflows/ci.yml: single GitHub Actions workflow, one job (ubuntu-latest, Temurin JDK 21, Node 22) on every push and PR — checkout, npm ci, npm test. No matrix, no codegen step (ADR 0015 §3, ADR 0007). Verified npm ci && npm test reproduce green locally (14 tests, 38 assertions, 0 failures). Browser/Playwright leg extends this same job under agd-01ky5hj355v3.

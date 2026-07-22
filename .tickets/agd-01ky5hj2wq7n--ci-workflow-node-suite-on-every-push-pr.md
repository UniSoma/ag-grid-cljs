---
id: agd-01ky5hj2wq7n
title: 'CI workflow: node suite on every push/PR'
status: open
type: chore
priority: 2
mode: afk
created: '2026-07-22T18:31:42.230950497Z'
updated: '2026-07-22T18:31:42.230950497Z'
acceptance:
- title: workflow committed, triggered on every push and PR, one job
  done: false
- title: 'job steps reproduce locally: npm ci && npm test green'
  done: false
- title: no OS/Node/AG-Grid matrix; no codegen step
  done: false
---

## Description

Single GitHub Actions workflow, one job, per ADR 0015 §3: checkout, JDK + Node setup, npm ci, npm test (the fast fail-early node contract suite). No matrix: single ubuntu runner, single Node LTS, the single registry-pinned AG Grid version. Codegen stays out of CI per ADR 0007. The browser-suite ticket later extends this same job with the Playwright leg.

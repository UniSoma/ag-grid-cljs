---
id: agd-01ky5hj382ea
title: 'Docs I: cljdoc tree, README, getting-started, options-and-conversion'
status: open
type: task
priority: 2
mode: afk
created: '2026-07-22T18:31:42.588331364Z'
updated: '2026-07-22T18:31:42.588331364Z'
acceptance:
- title: doc/cljdoc.edn lists the six articles plus the generated reference, in order
  done: false
- title: README is pitch + quickstart only
  done: false
- title: getting-started.md and options-and-conversion.md complete per their ADR 0014 outlines
  done: false
- title: no article restates a docstring contract; builders referenced by var link
  done: false
deps:
- agd-01ky5hj2mbj5
- agd-01ky5hj2q8bx
- agd-01ky5hj2zhjt
---

## Description

The cljdoc-canonical docs foundation per ADR 0014: doc/cljdoc.edn curating the article tree (all six topical articles plus the generated options reference last in the sidebar); README cut to pitch + quickstart; docs/getting-started.md (install, module registration via register!, first grid, builder tour in narrative form linking to var docstrings, Enterprise setup with set-license-key! folded in); docs/options-and-conversion.md (the EDN options map, the kebab->camel law, type-driven recursion rules, keywords-in-value-position, raw, JS-by-contract row data, dev warnings). Articles link to docstrings and never restate a per-var contract.

---
id: agd-01ky5hj382ea
title: 'Docs I: cljdoc tree, README, getting-started, options-and-conversion'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-22T18:31:42.588331364Z'
updated: '2026-07-22T20:38:43.150761322Z'
closed: '2026-07-22T20:38:43.150761322Z'
acceptance:
- title: doc/cljdoc.edn lists the six articles plus the generated reference, in order
  done: true
- title: README is pitch + quickstart only
  done: true
- title: getting-started.md and options-and-conversion.md complete per their ADR 0014 outlines
  done: true
- title: no article restates a docstring contract; builders referenced by var link
  done: true
deps:
- agd-01ky5hj2mbj5
- agd-01ky5hj2q8bx
- agd-01ky5hj2zhjt
---

## Description

The cljdoc-canonical docs foundation per ADR 0014: doc/cljdoc.edn curating the article tree (all six topical articles plus the generated options reference last in the sidebar); README cut to pitch + quickstart; docs/getting-started.md (install, module registration via register!, first grid, builder tour in narrative form linking to var docstrings, Enterprise setup with set-license-key! folded in); docs/options-and-conversion.md (the EDN options map, the kebab->camel law, type-driven recursion rules, keywords-in-value-position, raw, JS-by-contract row data, dev warnings). Articles link to docstrings and never restate a per-var contract.

## Notes

**2026-07-22T20:38:43.150761322Z**

Docs I per ADR 0014. doc/cljdoc.edn curates the full seven-entry tree (six topical articles + generated options reference last), in sidebar order — updating-data/cell-rendering/framework-composition/theming are placeholders owned by Docs II (agd-01ky5hj3awrs). README cut to pitch + quickstart (+ docs pointers, dev, license). Wrote docs/getting-started.md (install, register!, first grid, builder tour, dev-validations, Enterprise with set-license-key! folded in) and docs/options-and-conversion.md (EDN map, kebab->camel law, type-driven recursion, keywords-in-value-position, JS-by-contract rows, callback beans, raw, dev warnings). Both link builders/runtime fns by [[ns/var]] wikilink and never restate a docstring contract. Forward cross-links to the four Docs-II articles resolve once that ticket lands.

---
id: agd-01kyqf7t09w5
title: 'Builder catalog v2: open admission-bar questions'
status: open
type: task
priority: 3
mode: hitl
created: '2026-07-29T17:37:28.073680457Z'
updated: '2026-07-29T17:37:46.004826761Z'
tags:
- builders
- adr
acceptance:
- title: 'with-data-type-definitions: decided in or out, with the coercion question answered on its merits'
  done: false
- title: Any resulting amendment to ADR 0009 originates here
  done: false
links:
- agd-01kyqf8bgsq5
---

## Description

Home for open questions against the ADR 0009 builder-catalog admission bar (coerce-or-bundle). ADR 0019 designates this ticket as the place any amendment to ADR 0009 originates; its original id (agd-01kyjsd6sk2s) was never created, leaving a dangling reference in the accepted record.

Open question carried over from ADR 0019:

- `with-data-type-definitions` — ADR 0009 rejected it as a builder because `:data-type-definitions` is "plain passthrough the conversion contract already handles". ADR 0019 found that reason true but misleading: the contract handles it by camelizing the keys, and a builder that coerced hyphenated keyword keys to the literal strings AG Grid expects WOULD clear the bar that pure passthrough failed. Decide whether v2 admits it.

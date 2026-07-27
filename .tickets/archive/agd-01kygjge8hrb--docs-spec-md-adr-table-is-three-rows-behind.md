---
id: agd-01kygjge8hrb
title: docs/spec.md ADR table is three rows behind
status: closed
type: task
priority: 3
mode: afk
created: '2026-07-27T01:19:55.664989241Z'
updated: '2026-07-27T20:16:24.656384324Z'
closed: '2026-07-27T20:16:24.656384324Z'
tags:
- docs
acceptance:
- title: The table's intended scope is stated explicitly, or the three missing rows are added
  done: true
links:
- agd-01kygja77mxj
---

## Description

The ADR index table in `docs/spec.md` ends at 0015. Missing: 0016 (Clojars release engineering), 0017 (always-on field check), and 0018 (literal-key fallback in callback beans).

Decide first whether the table is exhaustive or v1-scoped. It is immediately followed by `**Status:** v1 implemented`, and 0016–0018 were added without updating it, so the omission may be deliberate. If the table is v1-scoped, state that scope above it so later ADRs do not read as an oversight. If it is exhaustive, add all three rows and use ADR 0018's current title and path: `docs/adr/0018-literal-key-fallback-callback-beans.md`.

## Notes

**2026-07-27T20:16:24.656384324Z**

Decided the table is v1-scoped, not exhaustive, and stated that scope above it. Evidence: the spec is framed as the locked v1 design (CONTEXT.md, the 'Status: v1 implemented' line under the table), ADR 0016 is by its own words the redraw of a scope-boundary item the spec parks as out of the effort, and 0017/0018 are implementation-phase amendments to contracts the spec's prose states — making the table exhaustive would have required rewriting that prose too. The new sentence names the 0001-0015 range and points at docs/adr/ for 0016+. Commit becc9d2.

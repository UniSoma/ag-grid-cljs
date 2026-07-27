---
id: agd-01kygjftnhwa
title: Optimize callback key transforms without unbounded caches
status: open
type: task
priority: 1
mode: afk
created: '2026-07-27T01:19:35.598605570Z'
updated: '2026-07-27T02:01:21.744109212Z'
tags:
- conversion
- perf
links:
- agd-01kygja77mxj
acceptance:
- title: kebab->camel bypasses splitting when the input contains no dash
  done: false
- title: camel->kebab bypasses regex replacement and lowercasing when the input contains no uppercase character
  done: false
- title: No process-lifetime cache is populated by arbitrary runtime JS property names
  done: false
- title: Any memoization is scoped to bounded internal inputs or uses an explicit bound
  done: false
- title: Transform outputs and existing conversion behavior remain unchanged
  done: false
- title: Standalone transforms, flat callback-bean lookup, and ADR 0018 fallback are benchmarked in dev and release builds
  done: false
---

## Description

Key transforms dominate callback-bean lookup. Current measurements on Node/V8, release build, 500k warmed iterations:

```text
kebab->camel "first-name"   726 ns
kebab->camel "value"        364 ns
camel->kebab "firstName"    266 ns
flat bean lookup             792 ns
flat bean lookup, memoized   154 ns
```

The common undashed case still runs `str/split`, and the original proposal memoized both public string transforms globally. That cache claim is too broad: `camel->kebab` receives JS property names during bean iteration, including arbitrary runtime data keys, so it is not bounded by consumer keywords.

Optimize the transforms without introducing a process-lifetime cache of arbitrary strings. This work precedes ADR 0018's literal-key fallback benchmarks.

## Design

1. Add allocation-light fast paths before any regex or split work:
   - `kebab->camel`: return the original string when it contains no `-`.
   - `camel->kebab`: return the original string when it contains no uppercase character, so neither regex replacement nor lowercasing is needed.
2. Profile the fast paths before adding caches. The common callback keys (`value`, `data`, `node`, `api`, `id`) should need neither splitting nor cache lookup.
3. If memoization still earns its keep, scope it to bounded internal inputs, such as keyword-derived lookup names. Do not make the public string functions or `prop->key` retain arbitrary runtime property names forever. Prefer no cache or a bounded cache for `camel->kebab`.
4. Preserve the public mechanical transforms exactly. This is a performance change, not a new conversion law.
5. Benchmark standalone transforms, flat callback-bean lookup, and ADR 0018's broad literal-key fallback in dev and release builds. Record absolute values and methodology; do not make an exact 5x ratio the contract.
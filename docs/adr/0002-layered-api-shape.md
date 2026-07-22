# 0002. Layered API shape: EDN options map, `with-*` builders, key registry

- Status: accepted, 2026-07-20
- Origin: charting session of the wayfinder map (tickets are ephemeral; this record is self-contained)

The public API is layered: a plain EDN options map at the bottom, curated `with-*` builders on top as sugar, and a generated key registry powering dev-mode typo warnings and a kebab<->camel reference table.

## Context

The design space for a ClojureScript AG Grid wrapper's API runs between two poles:

- **Fully typed/schema'd API**: every option modeled with a schema (e.g. Malli), giving validation and discoverability, but requiring the wrapper to track AG Grid's entire options surface release after release — a maintenance treadmill. A Malli layer was explicitly ruled out of scope elsewhere.
- **Raw interop with no help**: consumers write camelCase JS options by hand, getting the full surface but none of the idiomatic ergonomics or safety a wrapper exists to provide.

Neither pole is acceptable: the first is unsustainable, the second is not worth shipping.

## Decision

The public API is layered:

1. **Plain EDN options map (bottom layer).** Options are an ordinary Clojure map with kebab-case keys, auto-translated to camelCase at the conversion boundary (ADR 0005). The FULL AG Grid options surface is reachable via raw `assoc` — the wrapper never gatekeeps which options may be set. Any option AG Grid understands, including ones the wrapper has never heard of, passes through.
2. **Curated `with-*` builders (sugar layer).** Documented builder functions layered on top of the options map. Each builder is plain-map `assoc` under the hood and is never a required path — everything a builder does can be done by hand on the map. The builder catalog is ADR 0009.
3. **Generated key registry (assist layer).** A generated registry of known option keys powers dev-mode typo warnings (unknown key => warning, not error, since the full surface stays open) and a kebab<->camel reference table for documentation. The registry is ADR 0007.

## Consequences

- The wrapper never blocks access to new AG Grid options: raw `assoc` reaches anything, so AG Grid releases do not force wrapper releases.
- Typo detection is advisory only — a key absent from the registry produces a dev-mode warning, never rejection, because gatekeeping would contradict the open-surface guarantee.
- Builders are pure sugar; their absence for a given option is never a functional gap.

## Considered options

- **Fully typed/schema'd API (Malli or similar)** — rejected: chasing AG Grid's options surface schema-by-schema is a maintenance treadmill; a Malli layer is explicitly out of scope.
- **Raw interop with no help** — rejected: provides no kebab-case ergonomics, no discoverability, no typo assistance; the wrapper would add nothing over using AG Grid directly.

## References

- ADR 0005 — conversion boundary (kebab->camel translation of the options map)
- ADR 0007 — generated key registry
- ADR 0009 — builder catalog

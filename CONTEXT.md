# ag-grid-cljs

A framework-agnostic ClojureScript wrapper for AG Grid (Community + Enterprise), built on the vanilla `createGrid` core. The locked design lives in [docs/spec.md](docs/spec.md) and `docs/adr/`.

## Language

**Conversion boundary**:
The single translation layer between EDN options and the JS values AG Grid receives — mechanical kebab→camel keys, type-driven recursion, callbacks wrapped both ways.
_Avoid_: serialization, marshalling layer

**Raw**:
The sole escape hatch through the conversion boundary — `(ag/raw x)` passes a value to AG Grid verbatim, with no conversion of the value or of callback params/returns.
_Avoid_: passthrough, opaque wrapper

**JS-by-contract**:
The row-data rule: consumers supply plain JS row objects directly; the wrapper never converts row collections in either direction.
_Avoid_: eager conversion, proxy rows

**Row recipe**:
One of the two supported pairings for turning CLJS data into renderable rows — camel-keyed rows with keyword `:field`s, or literal kebab-keyed rows with string `:field`s. A recipe is the pairing, not the conversion call; half a recipe renders blank cells. The pairing outlives the datasource: a later write into a row must use the same converter the row was built with, or it lands a ghost key beside the live one — and a getter citing that name reads the ghost.
_Avoid_: row conversion, clj->js recipe

**Consumer-keyed option**:
An option whose nested map keys are names the consumer coins and cites elsewhere by exact spelling — CSS class names, agg-func names, row-data values (`:ref-data`) — rather than AG Grid vocabulary. Under the conversion boundary's one law those keys are strings, not keywords. Which string depends on the citation site: for the six cited from a stylesheet or from inside the options map it is the kebab name as written; for `:ref-data`, whose citation site is the rows, it is whatever spelling the row recipe carries.
_Avoid_: literal-keyed prop, name table

**Options map**:
The plain EDN map at the bottom of the API — kebab-case keys, the full AG Grid options surface reachable by ordinary `assoc`.
_Avoid_: config, grid spec

**Builder**:
A pure, `->`-threadable `with-*` function over the options map that coerces input, bundles behavior, or teaches a contract the reference table cannot express — never one that merely renames an option.
_Avoid_: setter, option helper

**Key registry**:
The generated, dev-only catalog of AG Grid option/event keys (`{:camel :type :default :initial? :deprecated :doc}`) powering typo warnings and the kebab↔camel reference table.
_Avoid_: schema, spec layer

**Initial-only key**:
A grid option AG Grid accepts only at creation; the differ warns once and ignores changes to it.
_Avoid_: immutable option

**Dev validations**:
The opt-in, registry-backed warnings over the EDN options map — unknown keys with a kebab did-you-mean. Heuristic against a registry pinned to one AG Grid version, so `enable-dev-validations!` gates them. Deprecation, type, option-dependency and row-model warnings are not these: they come from AG Grid's own `ValidationModule`, which `create-grid!` registers in dev builds (ADR 0020).
_Avoid_: linting, schema check

**Field check**:
The always-on dev diagnostic comparing each column's emitted field string against the keys of a sampled row, warning once per field with a did-you-mean. Registry-free — it compares two consumer-supplied things — so it needs no opt-in.
_Avoid_: field validation, column validation

**Ref-data check**:
The always-on dev diagnostic comparing a `:ref-data` column's sampled row value against that column's own emitted `refData` keys, warning only on a near-match — since `:ref-data` is sparse by intent, an unmatched value with no close key is unmapped rather than misspelled. The near-match rule is also what keeps it silent under both row recipes. Shares the field check's plumbing (same events, same row sample) with its own per-grid state.
_Avoid_: ref-data validation, label check

**Warning period**:
How often a given dev warning repeats. A warning about what the consumer *wrote* — an unknown key, a keyword where a string was meant, two conflicting builder options — is true for the life of the process, so it fires once. A warning about a *relationship between live things* — this column's field against this grid's rows — can be true for one grid and false for the next, so it fires once per grid (ADR 0022).
_Avoid_: dedup, warn-once

**Callback bean**:
The lazy view a wrapped callback receives over each JS object argument and recursively reached object, resolving keyword lookups without converting the underlying value.
_Avoid_: params bean, row bean, data bean

**Literal-key fallback**:
The callback bean's lookup rule: a keyword resolves to its camelized property when that property is present, and to its literal name otherwise.
_Avoid_: kebab fallback, verbatim-key fallback

**Rebuild-stable**:
The property every value the wrapper manufactures for an options map must have: `=` to itself given `=` inputs, so a consumer who rebuilds the whole map during render gets a clean `update-grid!` diff (ADR 0021). The consumer's own closures are their half of the contract.
_Avoid_: idempotent, referentially stable, memoized

**Deferred value**:
A wrapper-manufactured option value whose construction is postponed to the conversion boundary, with the consumer's input stashed under an internal tag so equal inputs compare `=`. The tag is internal; public `raw` stays verbatim.
_Avoid_: lazy value, thunk, promise

**GridHandle**:
The value returned by `create-grid!` — `{:api :opts}` — carrying the raw GridApi (via `grid-api`) and the last-applied options for diffing.
_Avoid_: grid instance, grid ref

**Data channel**:
The explicit row-data path — `set-rows!` (full swap) and `transact!` (:add/:update/:remove) — deliberately excluded from options diffing.
_Avoid_: row sync

**Renderer tiers**:
The three cell-renderer levels: bare fn (vanilla escape hatch), `render/renderer` lifecycle map with `dom-renderer`, and `react/react-renderer` with a per-cell local React root.

**Reference-consumer bar**:
Fulcro as the proof target the design must satisfy without shipping any adapter code.
_Avoid_: Fulcro support, Fulcro adapter

**Walking skeleton**:
The committed proof code in `src/main`, `src/dev`, and `src/test` that retired the five risk points; the scaffold implementation evolves in place.
_Avoid_: prototype, throwaway spike

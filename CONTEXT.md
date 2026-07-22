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

**Options map**:
The plain EDN map at the bottom of the API — kebab-case keys, the full AG Grid options surface reachable by ordinary `assoc`.
_Avoid_: config, grid spec

**Builder**:
A pure, `->`-threadable `with-*` function over the options map that coerces input or bundles behavior — never one that merely renames an option.
_Avoid_: setter, option helper

**Key registry**:
The generated, dev-only catalog of AG Grid option/event keys (`{:camel :type :default :initial? :deprecated :doc}`) powering typo warnings and the kebab↔camel reference table.
_Avoid_: schema, spec layer

**Initial-only key**:
A grid option AG Grid accepts only at creation; the differ warns once and ignores changes to it.
_Avoid_: immutable option

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

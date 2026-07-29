# 0021. Rebuild stability: wrapper-manufactured option values compare `=` across rebuilds

- Status: accepted, 2026-07-29
- Origin: knot tickets agd-01kynwykcdyg, agd-01kynwzbcmnt, agd-01kynwzt3a16 (tickets are ephemeral; this record is self-contained)

Every value the wrapper manufactures inside an options map is `=` to itself given `=` inputs, so a consumer who rebuilds the whole map during render gets a clean `update-grid!` diff: no spurious initial-only warnings, no redundant `setGridOption`. `Raw` compares by value. Values that need construction — the `with-row-id` callback, the renderer classes — defer construction to the conversion boundary, dispatched by an **internal** tag on `Raw`. The consumer's own closures remain the consumer's half of the contract.

## Context

The differ compares by `=` uniformly, per key present in the patch (ADR 0008 §"Diff mechanism"). ADR 0012 ships no framework adapters, so a React-family consumer has exactly one shape available: rebuild the options map during render and hand it to `update-grid!`. ADR 0008 anticipated that shape from the consumer's side — "the consumer re-supplies opts (with fresh closures) each update, and `=` detects the new fn object and applies it" — and asked whether the *consumer's* values are stable. It never asked whether *ours* are.

They were not. Three instances, all the same shape — a wrapper-manufactured value that is a fresh object on every call, so `=` reports a change that did not happen:

- **`:context (ag/raw {...})`**, ADR 0005 §4's canonical idiom. `Raw` was a `deftype` with no `IEquiv`, so `=` fell through to identity and two `Raw`s wrapping the same value were never equal. `:context` is `:initial? true`, so the first `update-grid!` after mount emitted "grid option `:context` is initial-only and cannot change after creation" — for a value that had not changed. Updatable raw-valued keys churned `setGridOption` instead.
- **`with-row-id`** built a fresh closure per call in both branches. `:get-row-id` is `:initial? true`, so a rebuilt map warned on every grid in the app, and no consumer-side memoization could suppress it: the closure is minted inside the builder. The only local workaround was to reach around the builder entirely.
- **The three renderer helpers** (`render/renderer`, `render/dom-renderer`, `react/react-renderer`) build a fresh component class per call. The value nests inside `:column-defs`, an ordinary updatable key, so the differ re-applies the whole `columnDefs` value on every render — redundant churn plus the column-state reset ADR 0008 records as "may, not guaranteed".

Why this survived to now: `update-grid!` has exactly one non-test call site in this repo, and the Fulcro reference consumer (ADR 0004's proof target) drives rows through the data channel and never calls the differ. The whole-rebuilt-map path was never exercised here.

## Decision

### 1. The promise

**Every value the wrapper manufactures for an options map is `=` to itself given `=` inputs.** It binds any public fn contributing an option value — the ADR 0009 builders, the renderer helpers, `raw` itself — not just the eight numbered builders.

The division of responsibility stops there. A consumer's own callbacks are the consumer's half: an inline `(fn [params] ...)` written fresh each render is a genuine new value and the differ will ship it, which is correct behavior on the input it is given. That half is guidance (define handlers at namespace level), and ADR 0008 already records the case no differ policy can fix — a stable ref closing over changed captured state.

### 2. `Raw` compares by value

`Raw` implements `IEquiv`, comparing the wrapped value with `=` rather than `identical?`. That is what makes two rebuilt `(raw {...})` values equal, and it is the whole fix for `:context`. For wrapped functions and JS objects `=` degrades to identity anyway — the strongest answer available for them, and the same answer the differ already gives a bare consumer fn.

### 3. `IHash` derives from the tag alone

Not from the wrapped value. The natural `(hash [x tag])` routes a wrapped JS object or function through `goog/getUid`, which **mutates it** with a `closure_uid_` property — and the values we wrap include consumer renderer classes and callbacks. Unequal values colliding on a hash is legal; writing a property onto a consumer's value to compute one is not. Every untagged `Raw` therefore hashes alike, which costs nothing real: a `Raw` is a value inside an options map, never a map key, and hashing one happens only when a map carrying it is itself hashed.

### 4. Deferred values: an internal tag on `Raw`, constructed at the boundary

This section records the mechanism as decided, not as shipped: `Raw` carries the tag field from the start (it participates in `=`, §2), but the boundary dispatch and its first two users land in agd-01kynwzbcmnt, and the renderer helpers in agd-01kynwzt3a16. See Consequences for what is true today.

A builder that would mint a closure, or a helper that would mint a class, instead stashes its *input* in a tagged `Raw` and lets the conversion boundary construct the real value. Equal inputs then give `=` options maps, because the comparable thing is the input.

- `Raw` carries an optional internal tag. The `raw?` branch of `->js` dispatches through an internal multimethod whose default is today's plain unwrap; the untagged case stays a direct field read rather than a dispatch, because `->js` runs on every non-raw callback return and that is a hot path.
- Public `raw` keeps its single arity and its verbatim meaning (`CONTEXT.md`). The tag, the two-arg constructor and the multimethod are internal to `impl`.
- **Open dispatch, not a `case`**: each helper namespace registers its own construction, so `convert` never has to require `render` or `react`. That matters because `react` is optional precisely so core consumers need not install `react-dom`. Load order is safe by construction — a tagged value can only exist if the namespace that mints it was loaded.
- **Tag the consumer's input, not the constructed value.** Load-bearing for the renderer helpers: `dom-renderer` and `react-renderer` each build a fresh lifecycle map of three fresh closures over the render fn before delegating to `renderer`, so tagging at the `renderer` level would leave the value non-`=` even with a perfectly stable render fn.

Constructing at conversion time rather than call time also means a new class object reaches AG Grid only when the diff actually fired — which is exactly when re-creating cell components is acceptable.

### 5. ADR 0009's admission bar gains a clause

A public fn contributing an option value must produce rebuild-stable output. A helper that mints a fresh object per call fails the bar unless it defers construction. This is stated here rather than only in ADR 0009 because it covers the renderer helpers too, which are not builders.

## Consequences

- **`update-grid!` on a whole rebuilt options map is the supported shape**, not a tolerated one. `docs/updating-data.md` §"The options channel" is where the promise and the division of responsibility are documented (agd-01kynwzbcmnt); `docs/framework-composition.md` carries a pointer, since its "not by re-rendering" line is right about rows but reads as forbidding the options pattern blessed here.
- **What is true today** (the `Raw` half, agd-01kynwykcdyg): the promise holds for `raw` itself, so `:context (ag/raw {...})` and every other raw-valued key survive a rebuild. `->js` still unwraps by a plain field read, nothing mints a tagged `Raw`, and the two remaining bug instances stand: `with-row-id` (agd-01kynwzbcmnt) and the three renderer helpers (agd-01kynwzt3a16). Those are the open gaps, and the docs name them as the caveats until each lands.
- **The renderer-helper gap is the last one, and the mildest.** `render/renderer`, `render/dom-renderer` and `react/react-renderer` degrade to churn plus a possible column-state reset rather than to a warning, which is why they were split off last. The remaining consumer half after that fix: an inline `(fn [params] ...)` passed to `dom-renderer` during render still churns.
- **Nothing about the emitted JS changes.** `->js` produces the same output it did before; only `=` and `hash` on `Raw` are new, and deferred construction moves only in *timing*. The `with-row-id` relocation will be held to that standard by leaving its existing tests untouched as the evidence.
- **`(with-row-id opts (raw f))` will start working.** It takes the non-keyword branch and calls a `Raw` as a function, which raises a `TypeError` on the first row — `Raw` implements no `IFn` — despite the docstring promising raw-wrapped fns receive raw JS params. Routing construction through the tag method fixes the documented idiom as a side effect (agd-01kynwzbcmnt).
- **Testing is node-only** (ADR 0015): every assertion here is about our contract — `=`, `hash`, `setGridOption` call counts against a fake api — not about AG Grid's runtime. The runtime half of the `with-row-id` relocation needs no new browser test either: six existing browser tests already mount real grids with `(with-row-id :id)`.
- **`CONTEXT.md` gains two terms**, *Rebuild-stable* and *Deferred value*. The **Raw** entry stays as written: it describes the public one-arity fn, which still means verbatim.

## Considered options

- **A key-keyed rule at the converter** (special-case the `getRowId` prop, memoize or construct there) — rejected: `->js` recursion is depth-uniform, so a rule on the prop `getRowId` also fires on a consumer's own `:get-row-id` nested inside `:cell-renderer-params` or a non-`raw` `:context`, silently replacing their value. It also contradicts ADR 0005 §1-2's mechanical, no-key-tables law, which is the boundary's whole selling point.
- **Builder-internal memoization** (cache the closure per input inside `with-row-id`) — rejected: it does not generalize to the renderer helpers, whose lifecycle map is a fresh object every call, and it puts a cache with a lifetime question where a value with an identity would do.
- **A marker record instead of a tagged `Raw`** — rejected: records are maps, so the type test must precede the `map?` branch of `->js`, and a consumer's hand-written `{:get-row-id :id}` would silently convert to a string. Two type tests on the hot path to express what one field already expresses.
- **A public tag with consumer `defmethod`s** — rejected: it makes the conversion boundary an open extension point, against ADR 0005's one-law promise, and leaves `Raw` meaning two incompatible things in the docs — "verbatim" for consumers and "dispatch me" for us.
- **`identical?` for `Raw` equality** — rejected: that is the behavior being fixed. It is what `=` already did by falling through to the default `IEquiv`.
- **Hashing the wrapped value** — rejected: mutates consumer classes and callbacks with `closure_uid_` (§3).
- **Documenting "hold your options map stable" as consumer guidance instead** — rejected: for a React-family consumer with no adapter (ADR 0012) that is an instruction to hand-memoize around our builders, and for `with-row-id` it was not even possible — the churning value is minted inside the builder.
- **ADR 0004 phrasing correction** — not needed. Its whole-map language becomes the accurate reading rather than a claim to fix, so no correction note is added there (repo practice is correction notes only, never rewrites).

## References

- ADR 0004 — update model (the whole-map phrasing this makes accurate)
- ADR 0005 §1-2, §4, §7 — conversion boundary: the no-key-tables law, `raw` as sole escape hatch, the callback-return path that keeps the untagged case a field read
- ADR 0008 — options diffing (`=` per key, the initial-only warning, the callback-stability guidance this completes from our side)
- ADR 0009 — builder catalog (the admission bar this adds a clause to)
- ADR 0011 — renderer tiers (the three helpers in the open gap)
- ADR 0012 — no framework adapters in v1 (why rebuild-during-render is the only shape available)
- ADR 0015 — testing split (why this is node-only)
- ADR 0018 §4 — literal-key fallback (the lookup law `with-row-id`'s keyword branch follows, preserved across the relocation)

# 0019. Consumer-keyed options: warn, never coerce, and split by citation reachability

- Status: accepted, 2026-07-28
- Origin: knot tickets agd-01kyjsex69qt, agd-01kymfv9sh68 (tickets are ephemeral; this record is self-contained)

A handful of AG Grid options are maps whose keys are names the *consumer* coins and cites elsewhere by exact spelling. The conversion boundary camelizes them like any other keyword key, which can silently break the citation. These are **consumer-keyed options**. The boundary is left exactly as it is; the wrapper only warns — and the six options split into two families needing two different diagnostics, because a keyword key is a real bug in one family and correct code in the other.

## Context

`map->js` camelizes every keyword map key (ADR 0005 §1-2). For nearly every option that is right: the key is an AG Grid option name. Six are different, because the key is a name the consumer invented:

| Option | Keys are | Cited from |
| --- | --- | --- |
| `:row-class-rules` (grid), `:cell-class-rules` (ColDef) | CSS class names | the consumer's stylesheet |
| `:agg-funcs` | agg-func names | ColDef `:agg-func`, `:initial-agg-func` |
| `:column-types` | column type names | ColDef `:type` |
| `:data-type-definitions` | cell-data-type names | ColDef `:cell-data-type` |
| `:components` | component names | `:cell-renderer`, `:cell-editor`, `:filter`, tool panels |

`{:row-class-rules {:row-warning pred}}` emits the class `"rowWarning"`, which matches no `.row-warning` rule. The failure is total silence: no exception, no console output, no visual error — the row simply is not styled. Found during the builder sweep for ADR 0009's catalog, and rated above any Tier-2 builder candidate because it is wrong output rather than missing sugar.

The near misses matter, because they show the category is narrow. `:icons` and `:locale-text` are also maps, but their keys are AG Grid's own vocabulary (`sortAscending`, `noRowsToShow`), so camelizing them is *correct*. `:cell-style` keys are CSS properties feeding a DOM style object, where `fontWeight` is what the DOM wants.

## Decision

### 1. The term, and a two-part test

A **consumer-keyed option** is one whose nested map keys are names the consumer coins **and** cites somewhere the conversion boundary does not reach.

Both clauses are load-bearing. `:cell-renderer-params` satisfies the first — its keys are entirely the consumer's — and is nevertheless *exempt*, because the name never escapes into a system that matches literally: `{:my-thing 1}` emits `myThing`, and the renderer reading `(:my-thing params)` finds it through the callback bean's camel-first lookup (ADR 0018). It round-trips, so keywords are fine there. The second clause is what separates it from `:agg-funcs`.

The set is a hand-maintained list of six, not a derived one. The second clause is not a fact the generated registry knows.

The glossary (`CONTEXT.md`) carries the term. "Literal-keyed prop" was the working name and is **retired**: `literal` already denotes the callback bean's **literal-key fallback** (ADR 0018) and, in `validate`'s tests, string keys. A third meaning on the forward direction would collide with both.

### 2. The contract is already correct; the wrapper only warns

ADR 0005 §3 states the law: *keyword means "an AG Grid term — translate it"; string means "my data — hands off"*. A CSS class name or an agg-func name is the consumer's data. **The string form was always the correct spelling**, and it already works — `map->js` copies string keys verbatim, and function values inside still get wrapped, which `(ag/raw #js {...})` over the whole map would not do.

So there is no gap in the contract, only in what the consumer knows. Conversion output is unchanged; the wrapper emits a dev warning. This keeps ADR 0002's open-surface guarantee (warnings never reject or alter what AG Grid receives) and keeps the boundary's one-sentence law true.

### 3. A keyword key is *not* the bug for the four reference options

The finding that shaped everything else. For the four options cited from inside the options map, keywords on **both** sides camelize consistently and the grid works:

```clojure
{:agg-funcs {:my-total f} :agg-func :my-total}   ; both -> "myTotal"; correct
{:agg-funcs {"my-total" f} :agg-func :my-total}  ; "my-total" vs "myTotal"; broken
```

The first spelling is the one a ClojureScript consumer reaches for first. A key-position keyword warning would fire on it — a false positive on correct code, which ADR 0017 §8 already established is the only failure that matters for a diagnostic the developer cannot switch off. A plain typo (`"my-totl"`) is the same user-visible bug and no key-position rule catches it either.

**The bug is the mismatch between the two sides, not the keyword.** Only cross-reference can see a mismatch.

### 4. Split by whether the citation is reachable

- **`:row-class-rules`, `:cell-class-rules`** — cited from a stylesheet the wrapper cannot read. Cross-reference is impossible, so the keyword-key heuristic is the only instrument, and it is used (agd-01kyjsex69qt).
- **`:agg-funcs`, `:column-types`, `:data-type-definitions`, `:components`** — cited from inside the options map. Cross-reference is available, strictly better (it catches typos too, and names the registered spelling), and is used instead (agd-01kymfv9sh68).

One term, two mechanisms. A future reader seeing class rules warn about keyword keys while `:agg-funcs` does not should read §3 before "fixing" the inconsistency.

### 5. The class-rules check

Lives in `impl.validate`, not `impl.convert`, though `convert`'s existing data-carrying-props nudge (ADR 0005 §5) is the same species. `validate` sees the authored EDN, so the message speaks the consumer's kebab vocabulary — ADR 0017 §3 accepted camel for the field check only because a listener genuinely cannot reach the EDN, and that constraint does not apply to a synchronous call. It also already has `warn-once!`, is already dead-code-eliminated wholesale (ADR 0007 §1), is position-aware so a map inside `:context` holding a key spelled `cellClassRules` cannot trip it, and is where the reference check must live anyway.

- **Always on**, ungated by `enable-dev-validations!`. Registry-free, so ADR 0017 §1's criterion applies — the gate exists to contain registry version drift, and there is no registry here. A new ungated entry point (`check-class-rules!`) rather than folding into `validate-options!`, which is gated.
- **Called from `create-grid!` and `update-grid!`**, each behind a `^boolean goog.DEBUG` call-site guard. That guard is load-bearing: `core.cljs` documents that keeping `validate` reachable only from `goog.DEBUG` branches is what lets `:advanced` eliminate the namespace and the registry. `update-grid!` is included because neither class-rules key is `:initial?` and `:column-defs` is an ordinary updatable key (ADR 0008), so both can first appear at update — and `update-grid!` ran no validation before this. It checks `new-opts`, the patch: it is a merge differ, so anything already applied was checked when it arrived.
- **Warns only when conversion changes the name** — a `-` in the key's name, or a namespace. Never on `:warning`, never on `"row-warning"`. Single-word CSS class names are common, so warning on every keyword key would fire routinely on working code. The general rule is taught in `docs/options-and-conversion.md` instead, where it costs nobody a console line.
- **Deduped** through the existing `warn-once!` on `[:row-class-rules :row-warning]` — the option keyword in the slot where other checks pass strings, so the shared set cannot collide. Once per class key, not per column; the column is deliberately not named, since naming it would force it into the dedup key and one typo across ten columns would warn ten times.

### 6. The reference check is drift-free, and therefore also always on

Designed here, built under agd-01kymfv9sh68. It carries **no list of AG Grid's built-in names**. It warns only when an unresolved citation is a **near-match against one of the consumer's own registrations** — `:agg-func "my-total"` against a registered `"myTotal"` — reusing `validate`'s existing `levenshtein`/`suggest`. A genuine built-in the consumer never registered has no near-match and stays silent.

That is what makes it registry-free: like the field check, it compares two consumer-supplied things to each other, so ADR 0017 §1 puts it always-on. A built-in name table would have been version-pinned AG Grid vocabulary — exactly the drift the `enable-dev-validations!` gate exists to contain.

It needs no api listeners. Unlike rows, which leave the options map (ADR 0004) and forced the field check to be event-driven, both the registrations and the citations live in the options map and stay there; `validate-options!` already walks that tree.

### 7. Not builders

Per-option builders for these fail ADR 0009's admission bar — they would merely name an option. ADR 0009 already rejects `with-data-type-definitions` on exactly that ground.

But its stated reason, *"plain passthrough the conversion contract already handles"*, is true and misleading: the contract does handle it, by camelizing the keys. And a builder that **hyphenated keyword keys would coerce**, which clears the bar that pure passthrough failed. ADR 0009 is left as written — it is a dated record of a decision that still stands — and the coercion question is recorded on the open builder-catalog-v2 ticket (agd-01kyjsd6sk2s), which is where it gets decided and where any amendment to 0009 should originate.

### 8. Docs carry the rule for all six

`docs/options-and-conversion.md` documents consumer-keyed options as the key-position half of the law already stated in its "Keywords are AG Grid's vocabulary" section, covering all six and prescribing **strings on both sides** for the four reference options — the complete fix, since the code deliberately does not police the citation side. The warnings list states the class-rules check's limited scope explicitly, so a reader cannot infer from silence that the other four are fine.

## Consequences

- **The class-rules check can false-positive.** A consumer who genuinely styles `.rowWarning` gets warned. No instrument can do better without reading CSS; the prior that a ClojureScript consumer writes kebab class names (plain CSS, Tailwind) is strong enough to accept it. This is the one place the project knowingly violates ADR 0017 §8's no-false-positives posture, because the alternative is no diagnostic at all.
- **`:detail-grid-options {:column-defs [...]}` is not walked**, so a master/detail `:cell-class-rules` goes unchecked. Deferred to the master-detail builder ticket, which owns detail-grid options as a whole.
- **`warned` is `defonce` and survives hot reload**, so re-introducing a just-fixed key later in a session is met with silence. ADR 0017 §9 rejected this atom for the field check, whose state is inherently per-grid; a misspelled class name is a spelling mistake in the options map, which is what the shared atom already dedupes for unknown keys and deprecations. Diverging would be inconsistency for its own sake.
- **`:ui/row-warning` warns twice** — `convert`'s existing dropped-namespace warning plus this one. Rare, and both are true.
- **`update-grid!` becomes a validation site for the first time**, for this check only. The gated registry-backed family still runs at creation only.
- **A gap persists until the reference check lands**: a registration/citation mismatch on the four options is silent. Not a regression — it is silent today — but the original ticket claimed to cover it.
- **Testing** follows ADR 0015. Node owns the condition, the four walk positions, dedup, the always-on property, and the `update-grid!` call site (`core_test` is DOM-free and already drives `update-grid!` against a fake api). One browser assertion covers the premise the warning rests on — that AG Grid applies the map key verbatim as a CSS class — which is about AG Grid rather than about us. Dead-code elimination is verified by grepping the `:advanced` build.
- **`validate` gains a test-only `disable!`** beside `reset-warnings!`. `enable!` is one-way and the namespace fixture calls it, so without a counterpart the always-on property — the entire reason the new entry point exists — cannot be expressed as a test.

## Considered options

- **Suppress camelization for these six props** (emit keyword keys verbatim) — rejected. It makes correct code the default, but it makes `->js` position-aware for the first time, trading a universal law users can hold in their head (ADR 0005 §1-2) for six special cases. It would also silently regress anyone who adapted to the current behaviour by styling `.rowWarning`, with no warning at all. The correct spelling is already available and one character-class away.
- **One keyword-key check over all six options** — rejected on the §3 finding: it fires on `{:agg-funcs {:my-total f} :agg-func :my-total}`, which works.
- **Coercing builders** (`with-row-class-rules` that hyphenates) — not chosen. It would clear ADR 0009's bar, but it fixes one call site and leaves the raw options map, which is the bottom of the API, just as broken. Recorded as a live candidate on the catalog-v2 ticket rather than dismissed.
- **A built-in name table for the reference check** — rejected: version-pinned AG Grid vocabulary, which would have forced the check behind `enable-dev-validations!` and made it invisible by default. The near-match-against-registrations rule gets the same catches without the pin.
- **Keeping the class-rules check in `impl.convert`** — rejected. It is ~5 lines beside the existing data-carrying nudge and it recurses everywhere (including detail-grid options), but it would print camel keys instead of the consumer's kebab, key on the emitted prop name so a `cellClassRules` key inside `:context` would trip it, need hand-gating at the call site to keep a per-map key scan out of production, and separate the two halves of one glossary term across two namespaces.
- **Detecting keyword *values* in the citation positions** — rejected as undecidable where the converter stands. Dashed-ness is not a signal there: `:cell-data-type :date-string`, `:type :right-aligned` and `:cell-renderer :ag-group-cell-renderer` are all dashed built-ins, so a converter-level rule would warn on correct code. The information needed is cross-option, which is what the reference check does with the whole tree in hand.
- **Refactoring `validate-options!` so both checks share one parameterized walk** — deferred. ~6 lines of duplicated traversal is cheaper than restructuring working gated code inside a bug fix; the reference-check ticket will have the better view.

## References

- ADR 0002 — layered API shape (open-surface guarantee: warnings never gatekeep)
- ADR 0005 §1-3, §5 — conversion boundary: the mechanical law, keyword-vs-string vocabulary, the data-carrying nudge this warning is modelled on
- ADR 0007 §1, §4-5 — key registry: dead-code elimination, and the opt-in validations these checks sit beside
- ADR 0008 — options diffing (`:column-defs` is an ordinary updatable key, which is why `update-grid!` is a call site)
- ADR 0009 — builder catalog v1 (the admission bar these options fail, and the wording that misleads)
- ADR 0015 — testing split (node vs browser)
- ADR 0017 §1, §3, §8, §9 — always-on field check: the registry-free criterion, why messages name camel there and kebab here, the no-false-positives posture, and the `defonce` dedup argument
- ADR 0018 — literal-key fallback (why `:cell-renderer-params` is exempt, and the naming collision that retired "literal-keyed prop")

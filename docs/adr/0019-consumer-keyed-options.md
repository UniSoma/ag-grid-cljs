# 0019. Consumer-keyed options: warn, never coerce, and split by citation reachability

- Status: accepted, 2026-07-28
- Amended 2026-07-29 (agd-01kyqmb9ssq7): `:ref-data` admitted as a seventh member, §4 gains a third case, §9 added, the keyword-value rejection narrowed
- Origin: knot tickets agd-01kyjsex69qt, agd-01kymfv9sh68, agd-01kyqmb9ssq7 (tickets are ephemeral; this record is self-contained)

A handful of AG Grid options are maps whose keys are names the *consumer* coins and cites elsewhere by exact spelling. The conversion boundary camelizes them like any other keyword key, which can silently break the citation. These are **consumer-keyed options**. The boundary is left exactly as it is; the wrapper only warns — and the seven options split into three families needing three different diagnostics, because a keyword key is a real bug in one family, correct code in the second, and recipe-dependent in the third.

## Context

`map->js` camelizes every keyword map key (ADR 0005 §1-2). For nearly every option that is right: the key is an AG Grid option name. Seven are different, because the key is a name the consumer invented:

| Option | Keys are | Cited from |
| --- | --- | --- |
| `:row-class-rules` (grid), `:cell-class-rules` (ColDef) | CSS class names | the consumer's stylesheet |
| `:agg-funcs` | agg-func names | ColDef `:agg-func`, `:initial-agg-func` |
| `:column-types` | column type names | ColDef `:type` |
| `:data-type-definitions` | cell-data-type names | ColDef `:cell-data-type` |
| `:components` | component names | `:cell-renderer`, `:cell-editor`, `:filter`, tool panels |
| `:ref-data` (ColDef) | the column's own cell values | **the row data** |

`{:row-class-rules {:row-warning pred}}` emits the class `"rowWarning"`, which matches no `.row-warning` rule. The failure is total silence: no exception, no console output, no visual error — the row simply is not styled. Found during the builder sweep for ADR 0009's catalog, and rated above any Tier-2 builder candidate because it is wrong output rather than missing sugar.

> **`:ref-data` added 2026-07-29 (agd-01kyqmb9ssq7).** The seventh member, found while judging an `as-select` `col`-level builder, and the one whose citation site is a kind the original six do not have: **row-data values**. It has the family's worst failure mode. `:ref-data {:in-progress "In Progress"}` emits the property `inProgress`; AG Grid resolves `refData[value] || ""` (`ValueService.formatValue`, `main.cjs.js` 35657-35659), so a row holding `"in-progress"` — the spelling bare `clj->js` produces, and the pairing the literal-kebab row recipe commits a consumer to — misses the lookup and the cell renders **blank**. Class rules lose their styling; this loses the content. `:ref-data` is registry-known, so nothing else flags it: it converts silently and validates clean. Its diagnostic is §9.

The near misses matter, because they show the category is narrow. `:icons` and `:locale-text` are also maps, but their keys are AG Grid's own vocabulary (`sortAscending`, `noRowsToShow`), so camelizing them is *correct*. `:cell-style` keys are CSS properties feeding a DOM style object, where `fontWeight` is what the DOM wants.

## Decision

### 1. The term, and a two-part test

A **consumer-keyed option** is one whose nested map keys are names the consumer coins **and** cites somewhere the conversion boundary does not reach.

Both clauses are load-bearing. `:cell-renderer-params` satisfies the first — its keys are entirely the consumer's — and is nevertheless *exempt*, because the name never escapes into a system that matches literally: `{:my-thing 1}` emits `myThing`, and the renderer reading `(:my-thing params)` finds it through the callback bean's camel-first lookup (ADR 0018). It round-trips, so keywords are fine there. The second clause is what separates it from `:agg-funcs`.

The set is a hand-maintained list — six at first writing, seven since `:ref-data` — not a derived one. The second clause is not a fact the generated registry knows, which is also why growing the list is an ordinary amendment rather than a regeneration.

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

> **Amended 2026-07-29 (agd-01kyqmb9ssq7): the split is three-way, not two-way.** As written, "reachable" silently meant *reachable from the options map*, and the two-case split reads as exhaustive. `:ref-data` is the third case:
>
> - **`:ref-data`** — cited from the row data. **Unreachable from the options map, reachable from the live grid.** Rows leave the options map at creation (ADR 0004), so a check driven from the EDN can never see the citation — but the field check already holds the live columns and samples a loaded row (ADR 0017 §2, §4-5), and that is all a cross-reference needs. So cross-reference is available after all, and by §4's own rating it wins.
>
> The lesson generalizes: *unreachable from the converter* is not the same claim as *unreachable*, and the second is the one that rules cross-reference out. One term, three mechanisms.

### 5. The class-rules check

Lives in `impl.validate`, not `impl.convert`, though `convert`'s existing data-carrying-props nudge (ADR 0005 §5) is the same species. `validate` sees the authored EDN, so the message speaks the consumer's kebab vocabulary — ADR 0017 §3 accepted camel for the field check only because a listener genuinely cannot reach the EDN, and that constraint does not apply to a synchronous call. It also already has `warn-once!`, is already dead-code-eliminated wholesale (ADR 0007 §1), is position-aware so a map inside `:context` holding a key spelled `cellClassRules` cannot trip it, and is where the reference check must live anyway.

- **Always on**, ungated by `enable-dev-validations!`. Registry-free, so ADR 0017 §1's criterion applies — the gate exists to contain registry version drift, and there is no registry here. A new ungated entry point (`check-class-rules!`) rather than folding into `validate-options!`, which is gated.
- **Called from `create-grid!` and `update-grid!`**, each behind a `^boolean goog.DEBUG` call-site guard. That guard is load-bearing: `core.cljs` documents that keeping `validate` reachable only from `goog.DEBUG` branches is what lets `:advanced` eliminate the namespace and the registry. `update-grid!` is included because neither class-rules key is `:initial?` and `:column-defs` is an ordinary updatable key (ADR 0008), so both can first appear at update — and `update-grid!` ran no validation before this. It checks `new-opts`, the patch: it is a merge differ, so anything already applied was checked when it arrived.
- **Warns only when conversion changes the name** — a `-` in the key's name, or a namespace. Never on `:warning`, never on `"row-warning"`. Single-word CSS class names are common, so warning on every keyword key would fire routinely on working code. The general rule is taught in `docs/options-and-conversion.md` instead, where it costs nobody a console line.
- **Deduped** through the existing `warn-once!` on `[:row-class-rules :row-warning]` — the option keyword in the slot where other checks pass strings, so the shared set cannot collide. Once per class key, not per column; the column is deliberately not named, since naming it would force it into the dedup key and one typo across ten columns would warn ten times.

> **Superseded in part by ADR 0022 (2026-07-29).** The two placement clauses above that rest on `validate` owning the mechanism are retired: the set moved to `impl.warn`, and the key space is `[site discriminator]`, so the keyword-in-a-string-slot sidestep is no longer load-bearing and the check is an ordinary `warn-once!` call on `[::class-rule-key [option k]]`. §5's four other reasons for the placement, and the once-per-class-key-not-per-column rule, are unchanged.

### 6. The reference check is drift-free, and therefore also always on

Designed here, built under agd-01kymfv9sh68. It carries **no list of AG Grid's built-in names**. It warns only when an unresolved citation is a **near-match against one of the consumer's own registrations** — `:agg-func "my-total"` against a registered `"myTotal"` — reusing `validate`'s existing `levenshtein`/`suggest`. A genuine built-in the consumer never registered has no near-match and stays silent.

That is what makes it registry-free: like the field check, it compares two consumer-supplied things to each other, so ADR 0017 §1 puts it always-on. A built-in name table would have been version-pinned AG Grid vocabulary — exactly the drift the `enable-dev-validations!` gate exists to contain.

It needs no api listeners. Unlike rows, which leave the options map (ADR 0004) and forced the field check to be event-driven, both the registrations and the citations live in the options map and stay there; `validate-options!` already walks that tree.

### 7. Not builders

Per-option builders for these fail ADR 0009's admission bar — they would merely name an option. ADR 0009 already rejects `with-data-type-definitions` on exactly that ground.

But its stated reason, *"plain passthrough the conversion contract already handles"*, is true and misleading: the contract does handle it, by camelizing the keys. And a builder that **hyphenated keyword keys would coerce**, which clears the bar that pure passthrough failed. ADR 0009 is left as written — it is a dated record of a decision that still stands — and the coercion question stays open, recorded here rather than in the catalog ADR. A builder-catalog v2 is where it gets decided.

### 8. Docs carry the rule for all six

`docs/options-and-conversion.md` documents consumer-keyed options as the key-position half of the law already stated in its "Keywords are AG Grid's vocabulary" section, covering all six and prescribing **strings on both sides** for the four reference options — the complete fix, since the code deliberately does not police the citation side. The warnings list states the class-rules check's limited scope explicitly, so a reader cannot infer from silence that the other four are fine.

> **Amended 2026-07-29 (agd-01kyqmb9ssq7): all seven, and the prescription is recipe-relative for `:ref-data`.** "Strings on both sides" is not the fix here, and stating it would be wrong under one of the two supported row recipes. A `:ref-data` key must match the exact spelling the **rows** carry, which under the camel row recipe is camel: `{"inProgress" "In Progress"}`, not `{"in-progress" …}`. Under the literal-kebab recipe it is kebab. The rule is therefore *the key is spelled like your row values*, and the docs state it that way. This is the first place the family's prescription depends on something outside the options map — which is the same fact §4's third case turns on.

### 9. The `:ref-data` check: cross-reference against the live grid, gated on a near-match

Added 2026-07-29 (agd-01kyqmb9ssq7). Lives in `impl.validate` beside the field check, whose plumbing it shares: the same two events (`modelUpdated`, `newColumnsLoaded`), the same `getColumns()` column list, the same `first-row` sample, the same resolve-to-short-circuit mechanism — extracted to a private `install-live-check!` / `run-live-check!` pair, each check keeping its **own** per-grid state atom. Sharing one atom would let the field check's verdict resolve a field for the ref-data check and silence it.

It reads the **emitted** JS, so ADR 0017 §3 applies unchanged: the message names camel key strings, because those are what AG Grid is looking up and failing to find.

**The near-match is the whole design.** The check warns only when the sampled row value is absent from that column's own `refData` keys **and** near-matches one of them, reusing `validate`'s `levenshtein`/`closest` — §6's rule, which is what keeps it registry-free (it carries no table of anything) and false-positive-free:

| Row value | `:ref-data` keys | AG Grid looks up | Cell | Warn? |
| --- | --- | --- | --- | --- |
| `"inProgress"` (camel recipe) | `{:in-progress …}` | `refData["inProgress"]` | renders | no |
| `"in-progress"` (kebab recipe) | `{:in-progress …}` | `refData["in-progress"]` | **blank** | yes, suggest `"in-progress"` |
| `"in-progress"` (kebab recipe) | `{"in-progress" …}` | `refData["in-progress"]` | renders | no |
| anything with no near-match | sparse by intent | — | blank | no |

**The message prescribes a spelling only where it can prove which side is wrong.** The near-match tells us two spellings are one edit apart; it does not tell us which one the consumer meant. When the nearest key is *exactly* what conversion emits for the value's kebab form, the diagnosis is certain — the consumer wrote a keyword key, the key is the wrong side, and the authored keyword can even be named back to them (`:in-progress`). Otherwise the mismatch is a plain misspelling that could sit on either side: `:ref-data {"in-progress" …}` against a row holding `"in-progres"` is bad *data*, and "write `"in-progres"`" would be telling the consumer to break a correct `:ref-data`. There the message reports the mismatch and states the rule. This is the one place where "catches plain typos for free" needs qualifying: it catches them, but it does not adjudicate them.

Row 1 is what the near-match rule buys. The ticket was filed to adjudicate a suspected false positive on the camel row recipe, and the rule **refutes** it rather than accepting it: correct code under *either* documented recipe stays silent, so the check needs no signal telling it which recipe is in force. Row 4 is the other half — `:ref-data` is sparse by intent (map three of ten statuses and the rest render blank on purpose), so a value with no close key is an unmapped value, not a misspelling. Like §6's reference check, it catches plain typos for free.

**Two supersessions, both mirroring skips the field check already makes.** A `valueFormatter` on the column means AG Grid never consults `refData` at all — `formatValue` reaches the `refData` branch only in the formatter's `else` (35640-35659) — and a `valueGetter` means the emitted field is not where the value comes from.

**Silence beyond those:** a non-string or empty sampled value (which covers a field the field check already reported absent, a nil cell, and a numeric column — no lookup happens, so there is nothing to be wrong about), and no rows loaded, which resolves nothing so the first real row still warns.

**Period: once per grid, per column** (ADR 0022 §1). The warning states a relationship between *this column's* `:ref-data` and *this grid's* rows, so a second grid whose rows are spelled differently must warn again. Hence `warn!` plus the per-grid atom, not `warn-once!` — and, as in ADR 0022 §7, that atom holds *resolved* targets rather than warned ones, because it is also the short-circuit keeping the check off `forEachNode` on every sort and resize.

The state key is the **col id**, not the field string, which is where the two checks' identities part company. The field check asks about the *field*, so keying on it is what makes "one typo across ten columns warns once" true (the same rule ADR 0019 §5 applied to class-rule keys). This check asks about a *column's own map*, so a raw column and a labelled one over the same field are two questions and must both get an answer. Both go through one `target-key`, which falls back to `:field` when a target supplies no `:col-id`, so the field check's behaviour is unchanged.

**One divergence from the field check, deliberate: a dotted field walks the whole path.** ADR 0017 §7 checks only the first segment because it tests *presence* and nested data is legitimately sparse. This check needs the *value*, and walking cannot false-positive here: a missing hop yields a non-string, and a non-string is silence.

Upstream-coverage bar (ADR 0017's appendix): AG Grid is silent. `refData[value] || ""` is the entire mechanism — no `_warn`, no `_error`, no `beans.validation?.` call anywhere near it. Verified at the call site, not just the text.

### 10. `:values` is docs-only

Decided 2026-07-29 (agd-01kyqmb9ssq7). `{:cell-editor-params {:values [:pending :in-progress]}}` and the Set Filter's `{:filter-params {:values …}}` camelize keyword *elements* the same way `:ref-data` camelizes keys, so a select editor writes `"inProgress"` into a column whose rows hold `"in-progress"` — the same mismatch, arriving from the value side.

One line in `docs/options-and-conversion.md` states the rule; **no check ships.** Not because detection is undecidable — the narrowing in Considered options removes that objection — but because nothing has yet asked for one. `:values` is a vector, not a citation *table*, so there is no second side to cross-reference against; a check would have to compare it to sampled row values, which is the ref-data check's shape and can be built from it if a real report arrives. Shipping it speculatively would add a second always-on diagnostic on the strength of a hypothetical.

**Also not requested: a builder.** `:ref-data` makes the label mapping plain data, so a `valueFormatter` closure is unnecessary — and would fail ADR 0021 anyway (a fresh fn per call inside `:column-defs`, churning `columnDefs` on every rebuild). What remained of the `as-select` shape this family member was found judging was `mapv name` over `:values` plus stringifying the label keys: coercion whose only purpose is dodging the camelization above. It also bundled a Community editor (`agSelectCellEditor`) with an Enterprise Set Filter (`agSetColumnFilter`) under one unannotated name, and had zero call sites, against ADR 0009's post-v1 deferral of per-column builders. A coercing `col`-level builder belongs with the coercion question §7 parked.

## Consequences

- **The class-rules check can false-positive.** A consumer who genuinely styles `.rowWarning` gets warned. No instrument can do better without reading CSS; the prior that a ClojureScript consumer writes kebab class names (plain CSS, Tailwind) is strong enough to accept it. This is the one place the project knowingly violates ADR 0017 §8's no-false-positives posture, because the alternative is no diagnostic at all.
- **`:detail-grid-options {:column-defs [...]}` is not walked**, so a master/detail `:cell-class-rules` goes unchecked. Deferred to the master-detail builder ticket, which owns detail-grid options as a whole.
- **`warned` is `defonce` and survives hot reload**, so re-introducing a just-fixed key later in a session is met with silence. ADR 0017 §9 rejected this atom for the field check, whose state is inherently per-grid; a misspelled class name is a spelling mistake in the options map, which is what the shared atom already dedupes for unknown keys and deprecations. Diverging would be inconsistency for its own sake.
- **`:ui/row-warning` warns twice** — `convert`'s existing dropped-namespace warning plus this one. Rare, and both are true.
- **`update-grid!` becomes a validation site for the first time**, for this check only. The gated registry-backed family still runs at creation only.
- **A gap persists until the reference check lands**: a registration/citation mismatch on the four options is silent. Not a regression — it is silent today — but the original ticket claimed to cover it.
- **Testing** follows ADR 0015. Node owns the condition, the four walk positions, dedup, the always-on property, and the `update-grid!` call site (`core_test` is DOM-free and already drives `update-grid!` against a fake api). One browser assertion covers the premise the warning rests on — that AG Grid applies the map key verbatim as a CSS class — which is about AG Grid rather than about us. Dead-code elimination is verified by grepping the `:advanced` build.
- **`validate` gains a test-only `disable!`** beside `reset-warnings!`. `enable!` is one-way and the namespace fixture calls it, so without a counterpart the always-on property — the entire reason the new entry point exists — cannot be expressed as a test.

Added 2026-07-29 with §9-10 (agd-01kyqmb9ssq7):

- **`create-grid!` installs two live-grid checks, so a grid registers four listeners in dev** instead of two, and a grid that does have a `:ref-data` column walks the row model twice at creation instead of once. A grid with no `:ref-data` column never walks it a second time — the target list is empty, so the short-circuit fires before `first-row`. Folding both passes into one installer would have saved the second walk; it was rejected because it renames `install-field-check!` and couples two independently-deletable diagnostics for a dev-only traversal that runs once.
- **A `:ref-data` column whose first sampled row value is not a string is never checked** — including a nil cell on row one with real values below it. Resolved-means-a-verdict-was-reached is doing what it says, and it is what keeps the steady state a set-membership test. Biased toward silence, per ADR 0017 §8.
- **`:detail-grid-options` is still not walked**, so a master/detail `:ref-data` goes unchecked — but only until that grid is created, at which point its own `create-grid!` installs its own checks. Unlike the class-rules gap, this one closes itself.
- **The three enumerations of the family now have seven members**: this ADR's table, `docs/options-and-conversion.md`'s, and `CONTEXT.md`'s **consumer-keyed option** glossary entry, which gains row-data values as a citation site.
- **§9's testing** also follows ADR 0015. Node owns the condition, every silence case, the per-grid period, the col-id keying and the suggestion, against `test-support`'s fake Column and GridApi. The browser suite owns the premise that is about AG Grid — an unmatched key renders the cell blank while the prescribed string key renders the label — plus the one fact a fake api cannot reach: that `create-grid!` installs the check. Dead-code elimination is verified by grepping the `:advanced` build for the `[ag-grid-cljs]` prefix and for `isFieldContainsDots`, whose only surviving occurrence is AG Grid's own method definition.
- **The node suite's AG Grid fakes moved to `ag-grid-cljs.test-support`**, which now holds the `capture` helper *and* `fake-col`/`fake-node`/`fake-api`/`fire!`. Two live-grid check suites read the same fake Column and GridApi; duplicating ~35 lines of them would have been worse than widening that namespace's charter by one section.

## Considered options

- **Suppress camelization for these six props** (emit keyword keys verbatim) — rejected. It makes correct code the default, but it makes `->js` position-aware for the first time, trading a universal law users can hold in their head (ADR 0005 §1-2) for six special cases. It would also silently regress anyone who adapted to the current behaviour by styling `.rowWarning`, with no warning at all. The correct spelling is already available and one character-class away.
- **One keyword-key check over all six options** — rejected on the §3 finding: it fires on `{:agg-funcs {:my-total f} :agg-func :my-total}`, which works.
- **Coercing builders** (`with-row-class-rules` that hyphenates) — not chosen. It would clear ADR 0009's bar, but it fixes one call site and leaves the raw options map, which is the bottom of the API, just as broken. Recorded as a live candidate on the catalog-v2 ticket rather than dismissed.
- **A built-in name table for the reference check** — rejected: version-pinned AG Grid vocabulary, which would have forced the check behind `enable-dev-validations!` and made it invisible by default. The near-match-against-registrations rule gets the same catches without the pin.
- **Keeping the class-rules check in `impl.convert`** — rejected. It is ~5 lines beside the existing data-carrying nudge and it recurses everywhere (including detail-grid options), but it would print camel keys instead of the consumer's kebab, key on the emitted prop name so a `cellClassRules` key inside `:context` would trip it, need hand-gating at the call site to keep a per-map key scan out of production, and separate the two halves of one glossary term across two namespaces.
- **Detecting keyword *values* in the citation positions** — rejected as undecidable where the converter stands. Dashed-ness is not a signal there: `:cell-data-type :date-string`, `:type :right-aligned` and `:cell-renderer :ag-group-cell-renderer` are all dashed built-ins, so a converter-level rule would warn on correct code. The information needed is cross-option, which is what the reference check does with the whole tree in hand.

  > **Narrowed 2026-07-29 (agd-01kyqmb9ssq7).** The rejection is sound, but its scope was stated wider than its evidence. All three examples are positions holding **AG Grid vocabulary**, where a dashed keyword is a built-in name and warning on it is a false positive. In a position holding nothing but the consumer's **own cell values** the argument does not run: there are no built-ins to collide with, so dashed-ness *is* evidence. The rejection therefore covers AG-Grid-vocabulary positions only. `:values` (§10) is the position this distinction was drawn for; it is decided docs-only for a different reason, and the ref-data check's shape is the template if consumer evidence ever asks for a check there.

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
- ADR 0017 §2, §4-5, §7 and its appendix — the live-grid plumbing §9 shares, the first-segment-only rule §9 diverges from, and the upstream-coverage bar §9 clears
- ADR 0021 — rebuild-stable option values (why a `valueFormatter`-closure builder is not the answer to §9's failure)
- ADR 0022 §1, §7 — warning period (why §9's check is once per grid, and why its atom holds resolved rather than warned fields)
- `docs/options-and-conversion.md` — the two **row recipes** §8's amended prescription is relative to

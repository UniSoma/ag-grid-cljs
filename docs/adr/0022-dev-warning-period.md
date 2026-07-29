# 0022. Dev warnings have a period, and `impl.warn` owns it

- Status: accepted, 2026-07-29
- Origin: knot tickets agd-01kyqy2y8c7m, agd-01kyqy3hd3fw (tickets are ephemeral; this record is self-contained)

A dev warning's **period** — how often it repeats — is set by what the warning is a statement *about*. A statement about what the consumer wrote fires once per process; a statement about a relationship between live things fires once per grid. `ag-grid-cljs.impl.warn` owns the prefix, the `goog.DEBUG` gate, the process-wide dedup set and its key space; callers whose truth is grid-scoped keep their own state and use the emit-only entry point.

## Context

Nothing owned dev warnings, so five sites each answered "does this repeat?" separately and arrived at four different answers:

| site | period | keyed by |
|---|---|---|
| `validate/warn-once!` | per process (`defonce`) | `[object-name kebab-key]` |
| `core/warn-once!` | per grid (atom on `GridHandle`) | grid-option key |
| field check | per grid (listener closure) | emitted field string |
| `convert/warn` ×7 | per occurrence | — |
| `with-pagination` | per occurrence | — |

The last two rows are defects, not choices. `convert`'s renderer-XSS nudge fires on **every cell render** of a renderer returning an HTML-looking string. `with-pagination`'s conflict warning fires on **every builder call** — and ADR 0021 exists precisely to make rebuilding the whole options map per render a supported shape, so that is per render, forever, from a public builder.

The key space was also unowned. ADR 0019 §5 deduped the class-rules check by passing the option *keyword* into `validate/warn-once!`'s first slot "where other checks pass strings, so the shared set cannot collide" — a real decision, made by convention, recorded nowhere a new caller would look. Four namespaces sharing that set makes it worse.

## Decision

### 1. The period rule

**A warning about what the consumer wrote fires once per process.** Unknown keys, a namespaced keyword, a non-keyword map key, a CLJS collection in a data-carrying prop, a CLJS set, a class-rule key conversion would rename, both pagination keys at once, an initial-only or unregistered key in an `update-grid!` patch. The authored text does not change within a process, so a second firing is noise.

**A warning about a relationship between live things fires once per grid.** The field check, and only the field check today: "this column's field is not a key in this grid's rows" can be true here and false next door.

This derives ADR 0017 §9 rather than contradicting it. §9's three reasons for rejecting a module-global set for the field check — the "present in this grid's rows" half is inherently per-grid, a global set silences a real bug on a second grid with differently-shaped rows, and `defonce` survives hot reload — all reduce to "this warning's truth is grid-scoped." Where truth is not grid-scoped, the reasons do not apply.

### 2. `impl.warn`, two entry points

- `(warn-once! site discriminator & msg)` — owns the process set. Key is `[site discriminator]`.
- `(warn! & msg)` — prefix and gate only, no dedup, for callers who own their own scope.

Both variadic in the message so concatenation happens *inside* the gate. This is not cosmetic: `update-grid!`'s `doseq` has no outer `goog.DEBUG` guard, so today a production build still builds `(str "grid option " k " is not in the key registry; …")` at four call sites and hands the finished string to a function that discards it.

`reset-warnings!` clears the set. It was a test helper; it is now also the documented answer for a consumer who wants hot-reload re-firing back (see §5). Consumers reach it as `core/reset-dev-warnings!` — the affordance is public because §5 rests an accepted trade-off on it, and `impl.*` is internal (ADR 0006), so pointing a consumer at `impl.warn` would not be an affordance at all.

### 3. Two-part key, site keyword owned by the emitter

`site` is a namespaced keyword naming the check, defined in the namespace that emits it (`::convert/renderer-html`, `::validate/unknown-key`). `discriminator` is the varying part, or `nil` when nothing varies.

Cross-site collision becomes impossible by construction, so ADR 0019 §5's keyword-in-a-string-slot sidestep stops being load-bearing and becomes an ordinary call. The set also becomes readable — at the REPL and in tests — as the pairs of checks that have fired, which is the only structured test seam this work builds. There is no recording atom and no injectable sink: an always-on recorder in the one namespace every warning flows through would grow for the life of a dev session, and a sink is main-source indirection bought for tests alone.

Three sites have nothing to discriminate on and pass `nil`, firing once per process, period: `with-pagination`'s key conflict, `convert`'s CLJS-set passthrough, and the renderer-XSS nudge.

### 4. The renderer-XSS nudge keys on nothing, and stays at render time

`wrap-renderer-fn` is called from `map->js` with both `prop` and the enclosing ColDef map in scope, so four discriminators are reachable and three are wrong. **The renderer fn** is a fresh closure every render for exactly the consumers ADR 0021 designed for, so keying on it dedups nothing. **The returned string** grows the set with row data. **The column's `:field`** is directly rejected by ADR 0019 §5, which chose not to name the column for the class-rules check because "naming it would force it into the dedup key and one typo across ten columns would warn ten times" — one renderer habit across ten columns is the same mistake. **`prop`** is nearly always the literal `"cellRenderer"`, i.e. `nil` wearing a hat.

So: `nil`, once per process. The message names no column today, so a second firing is a byte-identical duplicate.

Moving the check to conversion time — "a bare fn in a `cellRenderer` slot has innerHTML semantics", no return value needed, zero render-time cost — is rejected on this codebase's own criterion: it would fire on every bare-fn renderer including those correctly returning DOM nodes, and ADR 0017 §8 holds that for a diagnostic the developer cannot switch off, a false positive is the only failure that matters.

The per-render `(string? ret)` + `includes?` test therefore stays. A per-wrapper "already checked" flag would reduce the steady state to one boolean read, but it is speculative, dev-only, and negligible against AG Grid's own per-cell work. Nothing here makes it harder to add later.

### 5. `GridHandle` becomes `[api opts]`

The `warned` field encoded a per-grid period for warnings whose truth is process-wide, so the rule removes it. `create-grid!`'s docstring already conceded it was "not part of the public shape" and `CONTEXT.md` already defined the handle as `{:api :opts}` — the record was the thing out of step. A handle carrying an atom can never be `=`, which matters for the reference-consumer bar: Fulcro consumers put the handle in app state.

**The trade-off, taken with eyes open.** Per grid, the handle is recreated on hot reload, so re-introducing an initial-only mistake you just fixed warns again. Per process it does not — ADR 0017 §9's third reason, applied to a warning §9 was not about, and this is the loop where you would most want it (you meet "initial-only" while actively editing an `update-grid!` call). Accepted because the same loss has been live for `validate`'s unknown-key warnings since ADR 0007 with no complaint, and because `reset-warnings!` in a `^:dev/after-load` restores it for a consumer who wants it. The alternative — `core` keeping its own set like the field check does — buys hot-reload re-firing at the cost of an exception to §1 justified by tooling rather than by what the warning says, plus a duplicate warning per grid that names no grid.

### 6. Production shape, and a constraint on what may live here

`impl.warn` is the first dev-diagnostic namespace **reachable from production code**: `impl.convert` is the live conversion boundary and holds 7 of the 13 call sites. `impl.validate` and `impl.registry` vanish under `:advanced` only because nothing outside a `goog.DEBUG` branch names them; that story is not available here.

The set is therefore `(defonce ^:private warned (when ^boolean goog.DEBUG (atom #{})))` — `impl/registry.cljs`'s own pattern, whose docstring already says "nil in production builds (goog.DEBUG false → DCE)". Both functions gate internally.

**`impl.warn` must never require `impl.registry` or `impl.validate`.** ADR 0007 §1's elision depends on the ~600-key registry literal being named only from `goog.DEBUG`-guarded code, and this namespace is named from unguarded code. The dependency edge is `warn ← convert ← validate`. This constraint is not hypothetical: the obvious next tenant is the `levenshtein`/`closest`/`suggest` engine, which is registry-adjacent and would put a did-you-mean pool one careless require away from production.

### 7. The field check keeps its set; the node suite needs a reset

`check-fields!`'s atom is not a dedup set. It holds *resolved* fields — conj'd unconditionally for every unresolved target, including those found present — because it is also the short-circuit that stops `run-field-check!` walking `forEachNode` on every `modelUpdated` and `newColumnsLoaded`, and `newColumnsLoaded` fires on sort and resize. A `warn-once!` owning that set would conj only on warn, leaving present fields permanently unresolved and re-walking the row model on every sort. It calls `warn!`.

Consequence for tests: `cljs.test` runs the whole node suite in one process, so process-wide dedup makes warning-count assertions order-dependent. The shared capture helper (`src/test/ag_grid_cljs/test_support.cljs`) calls `reset-warnings!` on entry. That is the load-bearing reason for the helper; collapsing five copies of a `try`/`finally` is a bonus. The three browser namespaces keep patching `console.warn` directly — `initial_only_test` and `validation_module_test` assert AG Grid's *own* output, which no seam inside `impl.warn` can see.

## Consequences

- Two unbounded warning sources stop: per cell render, and per builder call on a rebuilt options map.
- `GridHandle`'s public shape changes from three fields to two. `->GridHandle` has three call sites (`core.cljs`, `core_test` ×2).
- A consumer with two grids sees one initial-only warning, not two. Hot reload no longer re-fires it; `core/reset-dev-warnings!` is the documented affordance, and the first public API this line of work adds.
- ADR 0019 §5's "`validate` … also already has `warn-once!`" placement clause is retired. Its four other reasons for that placement stand.
- `impl.warn` is the first `impl.*` namespace that survives into production builds as anything other than dead code, and it carries a permanent constraint on its dependencies to keep ADR 0007 §1 true.

## Considered options

- **One period for everything (once per process, keyed by message identity).** Rejected: ADR 0017 §9 already argued this specific rule wrong for the field check, and the argument generalizes to any warning whose truth depends on live state.
- **No rule; each site picks, documented as guidance.** Rejected: this is the status quo minus two accidents, and the accidents happened *because* there was no rule.
- **Dedup on the rendered message string, no key argument.** Rejected: makes the dedup key the message wording, so rewording re-fires and anything volatile in a message never dedups.
- **A macro instead of functions,** so call sites expand to `goog.DEBUG` branches and evaporate entirely in production. Rejected: introducing this repo's first macro namespace to save argument evaluation in dev-shaped code is a cost nothing else here has paid.
- **`impl.dev` rather than `impl.warn`.** Rejected: a bucket name invites unrelated dev machinery, which is precisely the gravity ADR 0019 §5's "the module that already had the helper" reasoning demonstrates.

## References

- ADR 0005 §4-5 (conversion boundary, data-carrying-props nudge), ADR 0007 §1 (production elision), ADR 0008 (options diffing), ADR 0017 §8-9 (false positives are the only failure that matters; per-grid state in the listener closure), ADR 0019 §5 (class-rules placement and dedup), ADR 0021 (rebuild-stable option values)
- `CONTEXT.md`: **Warning period**

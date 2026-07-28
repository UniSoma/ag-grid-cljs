# 0020. The wrapper registers AG Grid's ValidationModule in dev builds

- Status: accepted, 2026-07-28
- Origin: knot ticket agd-01kymx7yf4dy (tickets are ephemeral; this record is self-contained)

`create-grid!` registers AG Grid's own `ValidationModule` once, under `^boolean goog.DEBUG`, before it calls `createGrid`. It is not behind `enable-dev-validations!`. In exchange the wrapper deletes its own deprecation warnings, which were redundant with AG Grid's and strictly narrower. This is the one exception to ADR 0001's "the consumer owns module registration", and it is scoped to *diagnostics* modules in *dev* builds.

## Context

The redundancy audit in ADR 0017's appendix measured the wrapper's deprecation warnings against AG Grid 36.0.2's own. `ValidationService.processOptions` emits `As of v{version}, {name} is deprecated. {message}` from two hand-maintained runtime tables, `GRID_OPTION_DEPRECATIONS` and `COLUMN_DEFINITION_DEPRECATIONS`, through `_warnOnce` — so it dedupes exactly as our `warn-once!` does. Diffing camel names:

| | keys |
| --- | --- |
| AG Grid's runtime deprecation tables | 106 |
| our registry's `:deprecated` | 32 |
| ours minus theirs | 1 — `reactiveCustomComponents` |

The replacement text is the same text from the same upstream source. The gap is explained by origin: our registry records `@deprecated` tsdoc tags found at codegen time, AG Grid's tables are hand-maintained and wider. The single key we uniquely flag is a framework-adapter option with no meaning for a vanilla-core wrapper (ADR 0012).

Deleting our branch on that evidence alone would have been wrong, and the reason is the whole substance of this ADR. The two warnings are **not** reached under the same conditions. Ours needs `enable-dev-validations!`; AG Grid's needs `ValidationModule` registered — a *different* opt-in, which the docs framed as optional advice for a different class of check (types, option dependencies, row models). A consumer who called `enable-dev-validations!` and took the docs at their word would have lost deprecation coverage outright. Fixing the asymmetry in prose is not a fix, because prose can be ignored; the gates have to actually line up.

## Decision

### 1. `create-grid!` registers `ValidationModule` in `goog.DEBUG` builds

Once, before `createGrid`. Always on — **not** gated by `enable-dev-validations!`.

The gating criterion is ADR 0017 §1's: a check needs an opt-in when it can *drift*. `enable-dev-validations!` exists because our checks test consumer keys against a registry pinned to one AG Grid version, so a consumer on a newer AG Grid gets false "unknown option" warnings. AG Grid's validation tables ship inside the consumer's own AG Grid, at whatever version they installed. They cannot drift against it — `ValidationModule` is the most drift-free instrument in the picture, more so than the always-on field check, which at least compares two things that can disagree. By the project's own stated rule it earns always-on, and inheriting `enable-dev-validations!`'s gate would be borrowing a gate for a problem it does not have.

### 2. Registration lives in `create-grid!`, not in a top-level `defonce`

Modules must be registered before the first grid is created. Putting the call inside `create-grid!` makes that ordering constraint structurally impossible to violate rather than merely documented — there is no load-order or `:preloads` question to get wrong. The once-guard is a `defonce`d atom flipped by `compare-and-set!`, so hot reload does not re-register.

`registerModules` is additive and genuinely idempotent (`_registerModule` is a `Set.add` plus a map write), so the guard is cheapness, not correctness. There is no detection of prior consumer registration: reading `ModuleRegistry` state means touching an unstable private surface, and registering twice costs nothing.

### 3. Diagnostics modules are ours to register; capability modules stay the consumer's

`ValidationService` (the sole bean of `ValidationModule`) contains no `throw`. It only logs. Registering it cannot change what the grid does, cannot make a working grid fail, and cannot silently enable a feature the consumer did not ask for. That property — not "it's dev-only", not "it's useful" — is what makes this exception safe, and it is the line for any future case: a module that only observes may be registered on the consumer's behalf in dev; a module that adds capability stays theirs (ADR 0001).

No opt-out argument. The escape hatch is not calling `enable-dev-validations!` and registering what you like yourself; an opt-out for warnings a production build never sees is speculative configurability.

### 4. The wrapper's deprecation warnings are deleted

`check-key!`'s `if` collapses to `when-not (contains? camels prop)`; a known key is now a plain no-op. The **unknown-key** branch and its kebab did-you-mean are untouched, on the two deltas ADR 0017's appendix records: ours names kebab where AG Grid names camel, and ours is position-aware where AG Grid's `colDefPropertyMap` is one flat merged set of leaf and group keys.

Orphans go with it: `block-deprecations`, `:deprs` in both `block-spec` and the inline `grid-spec`, the `deprs` destructuring, and the `:deprs` line of the position-spec comment.

### 5. Registry codegen keeps emitting `:deprecated`

The field now has zero consumers. `docs/reference/ag-grid-options.md` is built by `keyTable`, which reads the in-memory `entry` objects — a *sibling* of `ednEntry`, not a consumer of it — so dropping `:deprecated` from the EDN would leave the markdown byte-identical. ADR 0014 does not feed cljdoc from it either.

Kept anyway on ADR 0007 §3's actual rationale: the registry is dev-warnings + docs authority only and DCEs away entirely, so richness costs nothing but dev-bundle and repo size. It also feeds the unbuilt `(registry/reference-table)` REPL helper noted in ADR 0007 §6. Withholding one field of the extracted shape would be a special case contradicting the shape documented in `registry.cljs` and `extract.mjs`.

## Consequences

- **Deprecation coverage improves rather than relocating**: 32 keys behind an opt-in becomes 106 keys in every dev build.
- **Every dev build now gets AG Grid's type, option-dependency and row-model warnings unbidden**, and an unknown key warns *twice* — ours in kebab, AG Grid's in camel. Accepted. It is the posture the field check and the class-rule check already carry, with a stronger case: this output cannot be wrong about the AG Grid the consumer is running.
- **The blocker on `update-grid!`'s initial-only warning dissolves** (agd-01kymx8m23sj). Under a *gated* registration it would not have: our initial-only warning is always-on, so trading it for a gated upstream warning would still leave a hole.
- **Bundle cost is zero.** `ag-grid-community` ships as one bundled file with `ValidationService` in the same file as `createGrid`, and ADR 0001 already records that consumers ship the whole ~1 MB community bundle regardless of what they register. The `ValidationModule` reference sits inside `goog.DEBUG`, so `:advanced` eliminates it; the once-flag atom is top-level but loses its only reader with the branch, and goes too.
- **`ag-grid-cljs.core` names a module object for the first time.** DCE verification therefore extends to it, and stays the manual `:advanced`-build grep of ADR 0017 and ADR 0019 rather than becoming a CI tripwire: a hit *count* would fire on any AG Grid release that reworded a message, which is noise, not signal. Reproduce with `npx shadow-cljs release dev-app` and `grep -o ValidationModule src/dev/public/js/main.js | wc -l`. As verified on this change: 4 hits, every one inside AG Grid's own bundled npm code (its CJS export map and three message texts naming the module in prose), none a wrapper reference. The decisive check, and the one to prefer on a future bump, is comparing the release bundle against one built from the parent commit — they were **byte-identical** here, which no amount of grepping can be talked out of.
- **Testing.** No node test for the registration: `create-grid!` needs a live element, so `core_test` cannot reach it, and a seam around a two-line idempotent dev-only side effect would cost more than it proves. One **browser** test carries it instead, following ADR 0019's precedent of pinning an AG-Grid-not-us premise with a single browser assertion — a grid with `:enable-range-selection` and no `register!` call, asserting `As of v32.2 … deprecated` reaches `console.warn`. That one test proves the registration happened, that the warning reaches the console, and that the replacement for the deleted branch is real. It goes red on an AG Grid bump that moves the tables, which is the signal we want. The browser tripwire only fails on `console.error`, so upstream *warnings* cannot break the suite.

## Considered options

- **Delete our deprecation branch and say nothing** — rejected: the two gates are independent opt-ins, so this is a silent coverage loss for anyone following the docs.
- **Delete it and promote "register `ValidationModule`" to a documented dev prerequisite** — rejected: it makes the fix a prose instruction the consumer must obey for our deletion to be safe. Docs cannot carry a behavioral guarantee.
- **Keep our branch** — rejected on the measurement: strictly narrower coverage, identical text, plus a registry field to maintain per AG Grid bump.
- **Register it behind `enable-dev-validations!`** — rejected: uniformity for its own sake, again. The gate contains registry drift; this module has no registry of ours to drift. It would also leave the sibling ticket's asymmetry in place.
- **Register at namespace load in a top-level `defonce`** — rejected: a library performing a global side effect on require is exactly what ADR 0001 objects to, and it makes the before-the-first-grid ordering depend on load order.
- **An opt-out argument or dynamic var** — rejected as speculative; nothing here reaches production, and the consumer can already decline every wrapper diagnostic by not calling `enable-dev-validations!`.
- **A new glossary term for the diagnostics/capability distinction** — rejected. It is rationale for one decision with one instance, and `CONTEXT.md` names concepts the project talks with, not the reasons behind single decisions. The **dev validations** entry is edited instead, to say what it no longer covers and who does. A second diagnostics-only module earns the distinction a name.

## References

- ADR 0001 — wrap vanilla core (the "consumer owns module registration" posture this carves an exception out of, and the ~1 MB bundle note)
- ADR 0007 §1, §3, §5-6 — key registry: DCE, the full-rich shape that keeps `:deprecated`, the division of labor this rewrites, the reference table
- ADR 0008 — options diffing (the initial-only warning whose sibling ticket this unblocks)
- ADR 0012 — no framework adapters in v1 (why `reactiveCustomComponents`, our one unique key, is not worth keeping a branch for)
- ADR 0015 — testing split (node vs browser)
- ADR 0017 §1 and appendix — the registry-free/always-on criterion, and the audit that measured this redundancy
- ADR 0019 — consumer-keyed options (the precedent of one browser assertion pinning an AG-Grid-not-us premise)

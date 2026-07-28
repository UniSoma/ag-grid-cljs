---
id: agd-01kyjsex69qt
title: Warn on keyword keys in row/cell class rules (consumer-keyed options)
status: closed
type: bug
priority: 1
mode: afk
created: '2026-07-27T21:59:54.313386852Z'
updated: '2026-07-28T14:42:27.167061924Z'
closed: '2026-07-28T14:42:27.167061924Z'
tags:
- conversion
- dx
acceptance:
- title: check-class-rules! in impl.validate warns on a :row-class-rules / :cell-class-rules keyword key whose name contains a - or carries a namespace; string keys and dashless keywords stay silent
  done: true
- title: The check ignores enable-dev-validations!, and is called under goog.DEBUG guards from both create-grid! and update-grid! (on the patch), reaching :column-defs incl. :children, :default-col-def and :auto-group-column-def
  done: true
- title: Message names the key, the emitted CSS class and the string fix; deduped via warn-once! keyed [option-key class-key]
  done: true
- title: Dead-code-eliminated under :advanced, verified by grepping the build
  done: true
- title: docs/options-and-conversion.md documents the consumer-keyed rule for all six options, strings on both sides for the four reference ones, and the warnings bullet states this check's limited scope
  done: true
- title: docs/adr/0019-consumer-keyed-options.md records the decision, the false-positive finding, the split and the accepted gaps
  done: true
- title: Node tests per the plan (adding a test-only disable!); one browser assertion that AG Grid applies the key verbatim as a CSS class
  done: true
---

## Description

Silent-wrong-output bug class. `map->js` camelizes every keyword map key (convert.cljs), including inside maps whose keys are the consumer's own coined names — a **consumer-keyed option** (CONTEXT.md). `{:row-class-rules {:row-warning f}}` emits the CSS class `"rowWarning"`, which matches no stylesheet rule: no error, no styling, nothing in the console.

ADR 0005 §3's law already covers this — "keyword = AG Grid vocabulary, string = my data, hands off". A CSS class name is consumer vocabulary, so the string form was always the correct spelling. Nothing tells the user that. So the fix teaches an existing rule rather than patching a broken contract, and conversion output is unchanged (warn-only; the open-surface guarantee holds).

Scope is `:row-class-rules` / `:cell-class-rules` only. The other four consumer-keyed options — `:agg-funcs`, `:column-types`, `:data-type-definitions`, `:components` — moved to a separate ticket, because for those a keyword key is *not* a bug: keywords on both sides camelize consistently and work, so a keyword-key warning would fire on correct code (ADR 0017 §8: for an always-on diagnostic, a false positive is the only failure that matters). Their real bug is a *mismatch* between the two sides, which needs cross-reference. Class rules are the one family whose citation lives in a stylesheet the wrapper cannot read, so the keyword-key heuristic is the only instrument available.

Full reasoning, including the accepted gaps, in ADR 0019.

## Design

**Home.** `impl.validate`, not `impl.convert`. It sees the authored EDN so the message can speak kebab (ADR 0017 §3 accepted camel for the field check only because a listener cannot reach the EDN — that constraint does not apply here); `warn-once!` and the dedup set already exist; ADR 0007 §1 already dead-code-eliminates the namespace; and the walk is position-aware, so a map inside `:context` or `:cell-renderer-params` that happens to hold a key spelled `cellClassRules` cannot trip it. It also puts this check in the same namespace as the reference check that follows it.

**Entry point.** New `check-class-rules!`, ungated by `@enabled?` — registry-free, so ADR 0017 §1's criterion for always-on applies. Called under `^boolean goog.DEBUG` guards from both `create-grid!` and `update-grid!`. The call-site guard is load-bearing, not decoration: core.cljs:245-247 documents that keeping `validate` reachable only from `goog.DEBUG` branches is what lets `:advanced` eliminate the namespace and the registry.

`update-grid!` is a call site because neither class-rules key is `:initial?` (registry.cljs:254,384) and `:column-defs` is an ordinary updatable key, so both can first appear at update — and `update-grid!` runs no validation today (core.cljs:373 assumes creation-time covered it). On update, check `new-opts` (the patch), not the merged stash: it is a merge differ, so anything already applied was checked when it arrived.

**Walk.** Self-contained: top-level `:row-class-rules`; `:cell-class-rules` on `:column-defs` (recursing `:children`), `:default-col-def`, `:auto-group-column-def`. Deliberately does *not* refactor `validate-options!` to share its traversal — ~6 lines of duplication is cheaper than restructuring working gated code inside a bug fix, and the reference-check ticket will have a better view of whether one parameterized walk is worth it.

**Condition.** Warn only when conversion changes the name: a `-` in `(name k)`, or a namespace. Never on `:warning` or `"row-warning"`. Single-word CSS class names are common, so warning on every keyword key would fire routinely on working code; the general "class names are strings" rule is taught in the docs instead, where it costs nobody a console line.

**Message.**

    [ag-grid-cljs] :row-class-rules key :row-warning emits the CSS class "rowWarning" — CSS class names are strings, not AG Grid vocabulary. Write "row-warning".

Names what was written, what AG Grid got, and what to write instead. No docs pointer (the fix fits inline). No did-you-mean register — the field check guesses, this one knows. Does not name the column: the class name identifies the mistake, and naming the column would force it into the dedup key, so one typo across ten columns would warn ten times.

**Dedup.** Existing `warn-once!` with signature `[:row-class-rules :row-warning]` — option keyword in slot one, class key in slot two. Existing checks pass strings there, so keywords cannot collide in the shared set. No new state.

**Accepted gaps** (recorded, not bugs to chase):
- CSS is unreadable to us, so this false-positives on a consumer who genuinely styles `.rowWarning`. No instrument can do better; the prior that a CLJS consumer writes kebab class names (plain CSS, Tailwind) is strong.
- `:detail-grid-options {:column-defs [...]}` is not walked. Deferred to the master-detail builder ticket (agd-01kyjsdva3hz), which owns detail-grid options as a whole.
- `warned` is `defonce` and survives hot reload, so re-introducing a fixed key later in a session is met with silence. ADR 0017 §9 rejected this atom for the field check, but that check's state is inherently per-grid; this one is a spelling mistake in the options map, which is exactly what the shared atom already dedupes for unknown keys and deprecations.
- `:ui/row-warning` double-warns: convert.cljs's existing "namespace dropped" plus this one. Rare, and both are true.

## Notes

**2026-07-28T14:42:27.167061924Z**

Split the original six-option scope after finding that a keyword key is CORRECT for the four reference options — keywords on both sides of :agg-funcs/:agg-func camelize consistently and work, so the planned warning would have fired on the spelling CLJS users reach for first. Shipped check-class-rules! in impl.validate for :row-class-rules/:cell-class-rules only (the one family whose citation is a stylesheet the wrapper cannot read): always-on, ungated by enable-dev-validations!, called under goog.DEBUG from create-grid! and update-grid! (on the patch), warning only when conversion changes the name. Reference check for the other four filed as agd-01kymfv9sh68. Reasoning and accepted gaps in ADR 0019. Node suite 83 tests / 258 assertions / 0 failures; DCE verified (0 hits in the :advanced bundle, 2 in the dev build); browser assertion passes (suite has 4 pre-existing failures, identical on clean main).

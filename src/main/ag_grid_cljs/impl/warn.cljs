(ns ag-grid-cljs.impl.warn
  "Dev warnings: the prefix, the goog.DEBUG gate, and the period (ADR 0022).

  A warning's PERIOD — how often it repeats — is set by what it is a statement
  about. A statement about what the consumer WROTE (an unknown key, a keyword
  where a string was meant, two conflicting builder options) is true for the life
  of the process, so `warn-once!` fires it once. A statement about a RELATIONSHIP
  BETWEEN LIVE THINGS (this column's field against this grid's rows) can be true
  for one grid and false for the next, so its caller owns per-grid state and uses
  `warn!`, which only prefixes and gates.

  The key space is `[site discriminator]`. `site` is a namespaced keyword naming
  the check, defined in the namespace that emits it; `discriminator` is the
  varying part, or nil when nothing varies. Cross-site collision is impossible by
  construction — callers do not have to know what other callers key on.

  Both entry points are variadic in the message so `str` runs INSIDE the gate:
  `update-grid!`'s loop has no outer goog.DEBUG guard, so an eagerly built message
  would be assembled and discarded in production builds.

  UNLIKE impl.validate and impl.registry, this namespace is reachable from
  PRODUCTION code — impl.convert is the live conversion boundary and holds most
  of the call sites. So it must never require impl.registry or impl.validate:
  ADR 0007 §1's elision depends on the registry literal being named only from
  goog.DEBUG-guarded code, and nothing here is. The did-you-mean engine is the
  obvious next tenant and is exactly what must not move in (ADR 0022 §6).")

;; nil in production builds (goog.DEBUG false -> DCE), as impl.registry does with
;; the registry literal: a defonce is a statement, not a droppable bare def.
(defonce ^:private warned (when ^boolean goog.DEBUG (atom #{})))

(defn warn!
  "Emit a dev warning. Prefix and goog.DEBUG gate only — no dedup. For callers
  whose warning is grid-scoped and who therefore own their own state."
  [& msg]
  (when ^boolean goog.DEBUG
    (js/console.warn (apply str "[ag-grid-cljs] " msg))))

(defn warn-once!
  "Emit a dev warning at most once per process for `[site discriminator]`.
  `site` is a namespaced keyword naming the check; `discriminator` is the varying
  part (the spelling that was wrong), nil when nothing varies, or a vector when
  more than one thing varies — `validate`'s unknown-key check keys on
  `[object-name k]`, since the same typo in a column and in the grid options are
  two different mistakes."
  [site discriminator & msg]
  (when ^boolean goog.DEBUG
    (let [k [site discriminator]]
      (when-not (contains? @warned k)
        (swap! warned conj k)
        (js/console.warn (apply str "[ag-grid-cljs] " msg))))))

(defn reset-warnings!
  "Clear the process-wide set, so every `warn-once!` check can fire again.

  Two uses. Tests call it between cases: the whole node suite runs in one
  process, so without it a warning-count assertion depends on what ran first.
  Consumers reach it as `core/reset-dev-warnings!`, for a `^:dev/after-load` hook
  that gets hot-reload re-firing back — `defonce` survives a reload, so
  re-introducing a mistake you just fixed is otherwise met with silence
  (ADR 0022 §5)."
  []
  (when ^boolean goog.DEBUG (reset! warned #{})))

(defn fired
  "The set of `[site discriminator]` pairs that have warned (test seam). Lets a
  test assert WHICH check fired without a regex over prose; read the console
  output itself when the message wording is the actual contract."
  []
  (when ^boolean goog.DEBUG @warned))

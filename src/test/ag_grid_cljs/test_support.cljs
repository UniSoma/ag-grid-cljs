(ns ag-grid-cljs.test-support
  "Shared support for the node suite. Required by test namespaces but never run
  as tests itself: shadow's :test build discovers namespaces by the default
  -test$ pattern, so this one is invisible to it (ag-grid-cljs.browser.util is
  the precedent on the browser side).

  Holds the one warning-capture helper. Collapsing the copies of the same
  try/finally is the smaller half of why it is shared; the load-bearing half is
  the reset-warnings! on entry — dedup is process-wide (ADR 0022) and cljs.test
  runs the whole node suite in one process, so a warning-count assertion is
  otherwise order-dependent on whatever ran first.

  The browser suite deliberately keeps its own capture: initial-only-test and
  validation-module-test assert AG Grid's OWN console output, which no seam
  inside impl.warn can see (ADR 0022 §7)."
  (:require [ag-grid-cljs.impl.warn :as warn]))

(defn capture
  "Run `f` with js/console.warn captured; return the vector of warning strings.
  Clears the process-wide dedup set first, so a count assertion over what `f`
  emitted means what it says."
  [f]
  (let [warnings (atom [])
        orig js/console.warn]
    (warn/reset-warnings!)
    (set! js/console.warn (fn [& args] (swap! warnings conj (apply str args))))
    (try (f) (finally (set! js/console.warn orig)))
    @warnings))

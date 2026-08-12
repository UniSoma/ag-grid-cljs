(ns ag-grid-cljs.test-support
  "Shared support for the node suite. Required by test namespaces but never run
  as tests itself: shadow's :test build discovers namespaces by the default
  -test$ pattern, so this one is invisible to it (ag-grid-cljs.browser.util is
  the precedent on the browser side).

  Holds the warning-capture helper and the AG Grid fakes the two live-grid checks
  share. For `capture`, collapsing the copies of the same try/finally is the
  smaller half of why it is shared; the load-bearing half is the reset-warnings!
  on entry — dedup is process-wide (ADR 0022) and cljs.test runs the whole node
  suite in one process, so a warning-count assertion is otherwise order-dependent
  on whatever ran first.

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

;; --- AG Grid fakes ----------------------------------------------------------
;; Node owns our contract given AG Grid's answers, so the Column and GridApi the
;; live-grid checks read are fakes here; the browser suite owns the assertions
;; that are about AG Grid itself (ADR 0015).

(defn fake-col
  "A fake AG Grid Column: `col-def` is the merged ColDef (a JS object), and the
  two dot-notation predicates answer what AG Grid resolved at column-build time.
  The col id defaults to the ColDef's `:colId` or `:field`, as AG Grid's own
  fallback does."
  ([col-def] (fake-col col-def false false))
  ([^js col-def field-dots? tooltip-dots?]
   #js {:getColDef                  (fn [] col-def)
        :getColId                   (fn [] (or (.-colId col-def)
                                               (.-field col-def)))
        :isFieldContainsDots        (fn [] field-dots?)
        :isTooltipFieldContainsDots (fn [] tooltip-dots?)}))

(deftype RowNodeFake [data group])

(defn fake-node
  "A fake RowNode. `data` nil models a CSRM group node; :group true models a
  group row (an SSRM group row carries only its grouping field).

  A class instance, not a #js literal, because AG Grid's RowNode is a class
  and the callback wrap points gate on `cljs.core/object?` — class instances
  are object?-false and are handed to callbacks raw, never beaned. A plain
  literal here would model the wrong side of that gate."
  [data group?]
  (RowNodeFake. data group?))

(defn fake-api
  "A fake GridApi, as `[api calls nodes destroyed?]`. `:columns` nil models the
  pre-colModel.ready window; `nodes` is a mutable atom of what forEachNode
  yields, so a test can land a row after installing and then fire a listener;
  `destroyed?` is a mutable atom answering isDestroyed, so a test can destroy
  the grid after installing. `calls` records the registered listeners and the
  two call counts."
  [{:keys [columns nodes]}]
  (let [nodes (atom (vec nodes))
        destroyed? (atom false)
        calls (atom {:listeners [] :get-columns 0 :for-each-node 0})
        api #js {:isDestroyed
                 (fn [] @destroyed?)
                 :getColumns
                 (fn []
                   (swap! calls update :get-columns inc)
                   (when columns (into-array columns)))
                 :forEachNode
                 (fn [f]
                   (swap! calls update :for-each-node inc)
                   (doseq [n @nodes] (f n)))
                 :addEventListener
                 (fn [event f]
                   (swap! calls update :listeners conj [event f]))}]
    [api calls nodes destroyed?]))

(defn fire!
  "Invoke the listener registered for `event` on a fake api's `calls` record."
  [calls event]
  ((some (fn [[e f]] (when (= e event) f)) (:listeners @calls)) nil))

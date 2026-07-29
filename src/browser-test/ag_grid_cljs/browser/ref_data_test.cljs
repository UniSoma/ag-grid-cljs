(ns ag-grid-cljs.browser.ref-data-test
  "Browser suite (ADR 0015): the one ref-data assertion that is about AG Grid
  rather than about us — that `refData` is looked up by the row value VERBATIM,
  so a camelized keyword key renders the cell BLANK while the string key the
  wrapper prescribes renders the label. That is the premise the ADR 0019 §9
  warning rests on; if AG Grid ever normalised either side, camelization would be
  harmless and the warning would be noise. The warning's own condition, its
  silence cases, its period and its suggestion are the node suite's
  (ag-grid-cljs.impl.ref-data-check-test) — which drives a fake api, so the same
  test also pins the one thing a fake cannot: that `create-grid!` installs the
  check at all."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]))

(defn- start-capture!
  "Begin collecting the wrapper's own console.warn lines. Returns
  `[warnings-atom restore!]`; the capture must outlive the call under test, since
  the warning arrives from an event listener a frame later."
  []
  (let [warnings (atom [])
        orig js/console.warn]
    (set! js/console.warn
          (fn [& args]
            (let [s (apply str args)]
              (when (re-find #"^\[ag-grid-cljs\]" s) (swap! warnings conj s))
              (apply orig args))))
    [warnings (fn [] (set! js/console.warn orig))]))

(deftest ref-data-is-looked-up-by-the-row-value-verbatim
  (testing "kebab-keyed rows: a keyword :ref-data key blanks the cell, a string key renders"
    (let [el (u/mount-el)
          [warnings restore!] (start-capture!)
          h  (grid/create-grid!
              el (-> (grid/options)
                     (grid/with-columns
                       ;; Both columns hold the same value, "in-progress" — the
                       ;; spelling bare clj->js produces and the literal-kebab
                       ;; row recipe commits a consumer to.
                       [{:col-id "kw" :field "status"
                         :ref-data {:in-progress "In Progress"}}
                        {:col-id "str" :field "status"
                         :ref-data {"in-progress" "In Progress"}}])
                     (grid/with-row-id :id)
                     (grid/with-row-data
                       #js [#js {:id 1 :status "in-progress"}])))]
      (async done
             (-> (u/poll-testid el (.cell u/testid "1" "kw"))
                 (.then (fn [kw-cell]
                          (is (some? kw-cell) "the grid rendered")
                          (is (= "" (some-> kw-cell .-textContent))
                              (str "the keyword key emitted refData.inProgress, "
                                   "refData[\"in-progress\"] misses, and AG Grid "
                                   "renders \"\" — the failure this check exists for"))
                          (u/poll-testid el (.cell u/testid "1" "str"))))
                 (.then (fn [str-cell]
                          (restore!)
                          (is (= "In Progress" (some-> str-cell .-textContent))
                              "the prescribed string key matches the row value and renders")
                          ;; The only assertion here that is about US: create-grid!
                          ;; wires the check up. A fake api cannot see that.
                          ;; Count only — the message's wording is node's
                          ;; (ADR 0015), and re-asserting it here would duplicate
                          ;; what ref-data-check-test already pins exactly.
                          (is (= 1 (count (filterv #(re-find #":ref-data has no key" %)
                                                   @warnings)))
                              "installed by create-grid!, and only the broken column warned")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done)))
                 (.catch (fn [e]
                           (restore!)
                           (is false (str "chain rejected: " e))
                           (done))))))))

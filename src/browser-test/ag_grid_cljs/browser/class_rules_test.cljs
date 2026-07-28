(ns ag-grid-cljs.browser.class-rules-test
  "Browser suite (ADR 0015): the one class-rules assertion that is about AG Grid
  rather than about us — that a :row-class-rules key reaches the row element as a
  CSS class VERBATIM, which is the premise the ADR 0019 warning rests on. If AG
  Grid ever normalised those keys, camelization would be harmless and the
  warning would be noise. The warning's own condition, walk, message and dedup
  are the node suite's (ag-grid-cljs.impl.validate-test)."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]))

(deftest row-class-rule-keys-reach-the-dom-verbatim
  (testing "a string key lands as written; a keyword key lands camelized"
    (let [el (u/mount-el)
          h  (grid/create-grid!
              el (-> (grid/options)
                     (grid/with-columns [{:field :name}])
                     (grid/with-row-data #js [#js {:name "Ada"}])
                     (assoc :row-class-rules
                            {"kebab-class" (constantly true)
                             :camel-me     (constantly true)})))]
      (async done
             (-> (u/next-frame)
                 (.then (fn [_]
                          (let [row (.querySelector el ".ag-row")
                                cls (when row (.-classList row))]
                            (is (some? row) "a row rendered")
                            (is (.contains cls "kebab-class")
                                "a string key is applied verbatim")
                            (is (.contains cls "camelMe")
                                "a keyword key is applied as its camelized form")
                            (is (not (.contains cls "camel-me"))
                                (str "AG Grid does NOT recover the authored kebab "
                                     "spelling — this is why :camel-me warns")))
                          (grid/destroy! h)
                          (u/detach! el)
                          (done)))
                 (.catch (fn [e]
                           (is false (str "chain rejected: " e))
                           (done))))))))

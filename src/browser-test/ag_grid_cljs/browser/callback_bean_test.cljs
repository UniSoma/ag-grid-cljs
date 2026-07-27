(ns ag-grid-cljs.browser.callback-bean-test
  "Browser leg of the callback-bean literal-key fallback (ADR 0018, ticket
  agd-01kygja77mxj): a string field renders a kebab-keyed row, and a real
  AG Grid callback reads the same literal key through the params bean."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]))

(defn- poll-testid
  "Promise of the element carrying data-testid `id` under `root`. AG Grid's
  TestIdService stamps test ids on a debounce after gridReady, so a single
  animation frame can win the race; poll briefly instead."
  [root id]
  (js/Promise.
   (fn [resolve _]
     (let [deadline (+ (js/Date.now) 3000)]
       ((fn tick []
          (if-some [n (u/by-testid root id)]
            (resolve n)
            (if (< (js/Date.now) deadline)
              (js/setTimeout tick 25)
              (resolve nil)))))))))

(deftest string-field-kebab-row-reads-through-a-real-callback
  (testing "a kebab-keyed row renders via a string field and a wrapped
            value-getter reads the same key through the fallback"
    (let [el  (u/mount-el)
          h   (grid/create-grid!
               el (-> (grid/options)
                      (grid/with-columns
                        [{:field "first-name"}
                         {:col-id       "greeting"
                          :header-name  "Greeting"
                          :value-getter (fn [p] (str "Hi " (:first-name (:data p))))}])
                      (grid/with-row-id :id)
                      (grid/with-row-data
                        #js [#js {:id 1 "first-name" "Ada"}])))
          api (grid/grid-api h)]
      (async done
             (-> (poll-testid el (.cell u/testid "1" "first-name"))
                 (.then (fn [name-cell]
                          (is (= 1 (.getDisplayedRowCount api)))
                          (is (= "Ada" (some-> name-cell .-textContent))
                              "the string field renders the literal kebab property")
                          (poll-testid el (.cell u/testid "1" "greeting"))))
                 (.then (fn [greet-cell]
                          (is (= "Hi Ada" (some-> greet-cell .-textContent))
                              "a real callback reads the kebab key through the bean")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done))))))))

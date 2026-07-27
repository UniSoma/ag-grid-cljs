(ns ag-grid-cljs.browser.row-recipes-test
  "The two documented row recipes (ticket agd-01kygjg6avt2), run as written in
  docs/options-and-conversion.md § If your rows are CLJS data. Each recipe is a
  pairing — row spelling plus the matching column :field spelling — so each
  test asserts the rendered cell AND a wrapped callback reading the same key.

  This suite exists to fail when the article's snippets stop working: the
  conversion call is invoked here verbatim rather than a hand-built #js row
  standing in for its output (that lookup-law proof is callback-bean-test)."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [cljs.test :refer [deftest is testing async]]))

(defn- greeting-col
  "A column whose value comes from a wrapped callback rather than a field, so
  the callback read is proven independently of AG Grid's field resolution."
  []
  {:col-id       "greeting"
   :header-name  "Greeting"
   :value-getter (fn [p] (str "Hi " (:first-name (:data p))))})

(defn- check-recipe
  "Mount `opts`, assert the row renders `\"Ada\"` under `field-col-id` and that
  the callback column agrees, then tear down."
  [opts field-col-id render-msg done]
  (let [el  (u/mount-el)
        h   (grid/create-grid! el opts)
        api (grid/grid-api h)]
    (-> (u/poll-testid el (.cell u/testid "1" field-col-id))
        (.then (fn [name-cell]
                 (is (= 1 (.getDisplayedRowCount api)))
                 (is (= "Ada" (some-> name-cell .-textContent)) render-msg)
                 (u/poll-testid el (.cell u/testid "1" "greeting"))))
        (.then (fn [greet-cell]
                 (is (= "Hi Ada" (some-> greet-cell .-textContent))
                     "a wrapped callback reads :first-name from the same row")
                 (grid/destroy! h)
                 (u/detach! el)
                 (done))))))

(deftest camel-keyed-rows-pair-with-keyword-fields
  (testing "clj->js with kebab->camel as :keyword-fn renders under {:field :first-name}"
    (async done
           (check-recipe
            (-> (grid/options)
                (grid/with-columns [{:field :first-name} (greeting-col)])
                (grid/with-row-id :id)
                (grid/with-row-data
                  (clj->js [{:id 1 :first-name "Ada"}]
                           :keyword-fn grid/kebab->camel)))
            "firstName"
            "a keyword field renders the camelized property"
            done))))

(deftest literal-kebab-rows-pair-with-string-fields
  (testing "bare clj->js renders under {:field \"first-name\"}"
    (async done
           (check-recipe
            (-> (grid/options)
                (grid/with-columns [{:field "first-name"} (greeting-col)])
                (grid/with-row-id :id)
                (grid/with-row-data (clj->js [{:id 1 :first-name "Ada"}])))
            "first-name"
            "a string field renders the literal kebab property"
            done))))

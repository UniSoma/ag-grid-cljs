(ns ag-grid-cljs.browser.runner
  "Headless runner-ns for the committed browser suite (ADR 0015). shadow's
  :browser-test target calls (init) on load; init runs every
  ag-grid-cljs.browser.*-test namespace via cljs.test in the real AG Grid
  runtime, then stashes the cljs.test summary on js/window where the Playwright
  driver (test/browser/run.mjs) reads it.

  cljs.test's own console output is kept (Playwright pipes it into the CI log);
  the driver's pass/fail verdict comes from window.__agTestSummary."
  {:dev/always true}
  (:require [cljs.test :as t]
            [shadow.test :as st]
            [shadow.test.env :as env]
            [shadow.dom :as dom]))

;; Captured at ns-load: the real cljs.test/report multimethod. run-all-tests
;; set!s cljs.test/report to our env :report-fn for the duration of the run, so
;; calling the var from inside report would recurse — call the captured
;; multimethod instead to keep the normal console output.
(def ^:private default-report t/report)

(defn- stash-summary! [{:keys [test pass fail error]}]
  (set! (.-__agTestSummary js/window)
        #js {:done     true
             :test     test
             :pass     pass
             :fail     fail
             :error    error
             :failures (+ fail error)}))

(defn- report [m]
  (default-report m)
  (when (= :end-run-tests (:type m))
    (stash-summary! m)))

(defn start []
  (-> (env/get-test-data) (env/reset-test-data!))
  (set! (.-__agTestSummary js/window) #js {:done false})
  (st/run-all-tests (assoc (t/empty-env) :report-fn report)))

(defn stop [done] (done))

(defn ^:export init []
  (dom/append [:div#test-root])
  (start))

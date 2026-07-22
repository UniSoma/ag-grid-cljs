(ns ag-grid-cljs.render-test
  "Contract tests for the cell-renderer helper (ticket agd-01ky0ed8adbf).
  DOM-free: what must hold for AG Grid to treat the helper's output as a
  component class; actual rendering is covered by the headless check."
  (:require [ag-grid-cljs.impl.convert :as convert]
            [ag-grid-cljs.render :as render]
            [cljs.test :refer [deftest is testing]]))

(defn- converted-class
  "Run a helper-produced renderer through the options converter and pull the
  cellRenderer back out, exactly as create-grid! would hand it to AG Grid."
  [r]
  (unchecked-get (convert/->js {:cell-renderer r}) "cellRenderer"))

(deftest class-survives-conversion-as-component
  (let [klass (converted-class
               (render/renderer {:init    (fn [state _] (reset! state :gui))
                                 :get-gui (fn [state] @state)}))]
    (testing "raw wrapping defeats the converter's fn auto-wrap"
      (is (fn? klass)))
    (testing "AG Grid's component detection: prototype && 'getGui' in prototype"
      (is (some? (.-prototype klass)))
      (is (fn? (.. klass -prototype -getGui)))
      (is (fn? (.. klass -prototype -init)))
      (is (fn? (.. klass -prototype -refresh)))
      (is (fn? (.. klass -prototype -destroy))))))

(deftest lifecycle-fns-receive-state-and-bean-params
  (let [seen  (atom nil)
        klass (converted-class
               (render/renderer {:init    (fn [state params]
                                            (reset! seen params)
                                            (reset! state (:row-index params)))
                                 :get-gui (fn [state] @state)}))
        inst  (new klass)]
    (.init inst #js {:rowIndex 3 :value "x"})
    (testing "params arrive as a lazy kebab bean"
      (is (= 3 (:row-index @seen)))
      (is (= "x" (:value @seen))))
    (testing "get-gui reads the same per-instance state"
      (is (= 3 (.getGui inst))))))

(deftest state-is-per-instance
  (let [klass (converted-class
               (render/renderer {:init    (fn [state params]
                                            (reset! state (:value params)))
                                 :get-gui (fn [state] @state)}))
        a     (new klass)
        b     (new klass)]
    (.init a #js {:value "a"})
    (.init b #js {:value "b"})
    (is (= "a" (.getGui a)))
    (is (= "b" (.getGui b)))))

(deftest init-return-is-swallowed
  ;; AG Grid calls .then on any non-null init return (deferred-init promise
  ;; protocol) — the wrapper must not leak the user fn's return value.
  (let [klass (converted-class
               (render/renderer {:init    (fn [state _] (reset! state :gui))
                                 :get-gui (fn [state] @state)}))
        inst  (new klass)]
    (is (nil? (.init inst #js {})))))

(deftest refresh-contract
  (testing "absent :refresh -> false (grid re-inits)"
    (let [klass (converted-class
                 (render/renderer {:init    (fn [state _] (reset! state :gui))
                                   :get-gui (fn [state] @state)}))
          inst  (new klass)]
      (.init inst #js {})
      (is (false? (.refresh inst #js {})))))
  (testing "truthy :refresh return is coerced to boolean true"
    (let [klass (converted-class
                 (render/renderer {:init    (fn [state _] (reset! state :gui))
                                   :get-gui (fn [state] @state)
                                   :refresh (fn [_ _] :updated)}))
          inst  (new klass)]
      (.init inst #js {})
      (is (true? (.refresh inst #js {}))))))

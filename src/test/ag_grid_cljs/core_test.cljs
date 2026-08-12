(ns ag-grid-cljs.core-test
  "Contract tests for the GridHandle and the retargeted runtime channels
  (ticket agd-01ky5hj2mbj5). DOM-free: create-grid! itself needs a live DOM
  element, so these tests construct a handle over a fake GridApi and verify the
  accessor and channels dispatch onto it."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.impl.convert :as convert]
            [ag-grid-cljs.impl.warn :as warn]
            [ag-grid-cljs.react :as react]
            [ag-grid-cljs.render :as render]
            [ag-grid-cljs.test-support :refer [capture]]
            [cljs.test :refer [deftest is testing]]))

(defn- fake-api
  "A stand-in GridApi that records the (method, args) calls made against it."
  []
  (let [calls (atom [])]
    (js-obj
     "calls"          calls
     "setGridOption"  (fn [k v] (swap! calls conj [:set-grid-option k v]))
     "applyTransaction" (fn [tx] (swap! calls conj [:apply-transaction tx]) #js {:add [] :update [] :remove []})
     "destroy"        (fn [] (swap! calls conj [:destroy])))))

(defn- calls-of [api] @(unchecked-get api "calls"))

(defn- handle
  "A GridHandle over a fresh fake api with the given stashed EDN opts."
  ([] (handle {}))
  ([opts] (grid/->GridHandle (fake-api) opts)))

(defn- set-grid-option-calls [h]
  (filterv (fn [[m]] (= m :set-grid-option)) (calls-of (grid/grid-api h))))

(deftest grid-api-accessor
  (testing "grid-api pulls the raw GridApi back out of the handle"
    (let [api (fake-api)
          h   (grid/->GridHandle api {:column-defs []})]
      (is (identical? api (grid/grid-api h))))))

(deftest handle-is-a-comparable-value
  ;; ADR 0022 §5: the handle holds no atom, so two handles over the same grid and
  ;; the same applied opts are =. Load-bearing for the reference-consumer bar —
  ;; Fulcro consumers put the handle in app state, which diffs by =.
  (let [api (fake-api)]
    (is (= {:api api :opts {:pagination true}}
           (into {} (grid/->GridHandle api {:pagination true})))
        "the handle carries the documented two fields and nothing else")
    (is (= (grid/->GridHandle api {:pagination true})
           (grid/->GridHandle api {:pagination true})))
    (is (not= (grid/->GridHandle api {:pagination true})
              (grid/->GridHandle api {:pagination false})))))

(deftest set-rows!-targets-the-handle
  (let [h    (handle)
        rows #js [#js {:id 1}]]
    (grid/set-rows! h rows)
    (is (= [[:set-grid-option "rowData" rows]] (calls-of (grid/grid-api h))))))

(deftest transact!-targets-the-handle
  (let [h (handle)]
    (testing "the tx map is forward-converted before it hits the api"
      (grid/transact! h {:add [#js {:id 1}]})
      (let [[[method tx]] (calls-of (grid/grid-api h))]
        (is (= :apply-transaction method))
        (is (array? (unchecked-get tx "add")))))))

(deftest destroy!-targets-the-handle
  (let [h (handle)]
    (grid/destroy! h)
    (is (= [[:destroy]] (calls-of (grid/grid-api h))))))

;; --- update-grid! PATCH differ (ADR 0008) -----------------------------------

(deftest update-grid!-applies-only-changed-updatable-keys
  (testing "a changed updatable key produces exactly one setGridOption"
    (let [h (handle {:pagination false})]
      (grid/update-grid! h {:pagination true})
      (is (= [[:set-grid-option "pagination" true]] (set-grid-option-calls h)))))
  (testing "an unchanged key present in new-opts produces no call"
    (let [h (handle {:pagination true})]
      (grid/update-grid! h {:pagination true})
      (is (= [] (set-grid-option-calls h)))))
  (testing "keys absent from new-opts produce no call"
    (let [h (handle {:pagination false :quick-filter-text "x"})]
      (grid/update-grid! h {:pagination true})
      (is (= [[:set-grid-option "pagination" true]] (set-grid-option-calls h)))))
  (testing "several changed updatable keys each produce one call"
    (let [h (handle {:pagination false :quick-filter-text "x"})]
      (grid/update-grid! h {:pagination true :quick-filter-text "ada"})
      (is (= #{[:set-grid-option "pagination" true]
               [:set-grid-option "quickFilterText" "ada"]}
             (set (set-grid-option-calls h)))))))

(deftest update-grid!-initial-only-warns-once-and-is-ignored
  (let [h (handle {:context {:a 1}})
        w (capture #(do (grid/update-grid! h {:context {:a 2}})
                        (grid/update-grid! h {:context {:a 3}})))]
    (is (= [] (set-grid-option-calls h)) "initial-only key never reaches the api")
    (is (= 1 (count w)) "warns once per key across successive updates")
    (is (re-find #":context is initial-only" (first w)))))

(deftest update-grid!-initial-only-warns-once-per-process-not-per-handle
  ;; ADR 0022 §5: the warning is a statement about what the consumer WROTE, not
  ;; about this grid, so the period is per process. A consumer with two grids
  ;; making the same mistake sees one line, and the handle needs no atom to hold
  ;; the set — which is what makes it a comparable value.
  (let [w (capture #(do (grid/update-grid! (handle {:context {:a 1}}) {:context {:a 2}})
                        (grid/update-grid! (handle {:context {:a 1}}) {:context {:a 2}})))]
    (is (= 1 (count w)) "two handles, same initial-only key, one warning")
    (is (contains? (warn/fired) [::grid/initial-only :context])
        "the check that fired is readable as a pair, without a regex over prose")))

(deftest update-grid!-row-data-warns-and-is-ignored
  (let [h (handle {})
        w (capture #(grid/update-grid! h {:row-data #js [#js {:id 1}]}))]
    (is (= [] (set-grid-option-calls h)) "the data channel owns :row-data")
    (is (= 1 (count w)))
    (is (re-find #":row-data is owned by the data channel" (first w)))))

(deftest update-grid!-unclassified-applies-and-warns
  (let [h (handle {})
        w (capture #(grid/update-grid! h {:totally-bogus-xyz 7}))]
    (is (= [[:set-grid-option "totallyBogusXyz" 7]] (set-grid-option-calls h)))
    (is (= 1 (count w)))
    (is (re-find #"not in the key registry" (first w)))))

(deftest update-grid!-column-defs-ships-whole-value
  (let [h (handle {:column-defs [{:field :a}]})]
    (grid/update-grid! h {:column-defs [{:field :a} {:field :b}]})
    (let [[[method prop val]] (set-grid-option-calls h)]
      (is (= :set-grid-option method))
      (is (= "columnDefs" prop))
      (is (array? val))
      (is (= 2 (.-length val)) "the whole new column-defs value is forward-converted and shipped")
      (is (= "b" (unchecked-get (aget val 1) "field"))))))

(deftest update-grid!-checks-class-rule-keys-on-the-patch
  ;; :column-defs is an ordinary updatable key, so class rules can first arrive
  ;; at update; update-grid! ran no validation before ADR 0019.
  (let [h (handle {:column-defs [{:field :a}]})
        w (capture #(grid/update-grid!
                     h {:column-defs [{:field :a
                                       :cell-class-rules {:via-update (constantly true)}}]}))]
    (is (= 1 (count (filter #(re-find #"viaUpdate" %) w)))
        "a keyword class-rule key arriving at update warns")))

;; --- builder catalog v1 (ADR 0009) ------------------------------------------

(defn- get-row-id-fn
  "Convert opts through the boundary and pull out the resulting getRowId JS fn."
  [opts]
  (unchecked-get (convert/->js opts) "getRowId"))

(deftest with-row-id-keyword-reads-field-and-string-coerces
  (testing "a keyword reads that field off the raw JS row and str-coerces"
    (let [f (get-row-id-fn (grid/with-row-id {} :id))]
      (is (= "7" (f #js {:data #js {:id 7}})) "numeric id coerced to string")))
  (testing "the field name is kebab->camel'd like every other key"
    (let [f (get-row-id-fn (grid/with-row-id {} :first-name))]
      (is (= "Ada" (f #js {:data #js {:firstName "Ada"}}))))))

(deftest with-row-id-keyword-follows-the-callback-bean-lookup-law
  ;; ADR 0018 §1/§4: camel when present, literal otherwise — same law as the
  ;; callback beans, but on the raw JS row with no bean allocation.
  (testing "a literal dashed row key resolves when the camel spelling is absent"
    (let [f (get-row-id-fn (grid/with-row-id {} :record-id))]
      (is (= "r1" (f #js {:data #js {"record-id" "r1"}})))))
  (testing "camel keeps priority when both spellings are present"
    (let [f (get-row-id-fn (grid/with-row-id {} :record-id))]
      (is (= "camel" (f #js {:data #js {:recordId "camel" "record-id" "literal"}})))
      (is (= "false" (f #js {:data #js {:recordId false "record-id" "literal"}}))
          "presence, not truthiness, decides")))
  (testing "heterogeneous rows resolve independently of order"
    (let [f (get-row-id-fn (grid/with-row-id {} :record-id))]
      (is (= ["camel" "literal"]
             [(f #js {:data #js {:recordId "camel"}})
              (f #js {:data #js {"record-id" "literal"}})]))))
  (testing "presence is own-property: inherited members don't shadow literal keys"
    (let [f (get-row-id-fn (grid/with-row-id {} :value-of))]
      (is (= "v" (f #js {:data #js {"value-of" "v"}}))))))

(deftest with-row-id-fn-receives-bean-and-string-coerces
  (testing "a fn receives the kebab-bean params and its return is str-coerced"
    (let [f (get-row-id-fn (grid/with-row-id {} (fn [p] (:id (:data p)))))]
      (is (= "7" (f #js {:data #js {:id 7}}))))))

(deftest with-row-id-raw-fn-receives-raw-params-and-string-coerces
  ;; The documented hot-path idiom, which raised a TypeError on the first row
  ;; until construction moved to the boundary (ADR 0021 §4).
  (testing "(raw f) opts out of the bean: the fn sees the JS params object"
    (let [f (get-row-id-fn (grid/with-row-id {} (grid/raw (fn [^js p] (.-id (.-data p))))))]
      (is (= "7" (f #js {:data #js {:id 7}})) "raw params, still str-coerced"))))

(defn- row-id-of
  "A top-level-def-d row-id fn: the consumer's half of rebuild stability."
  [p]
  (:id (:data p)))

(deftest with-row-id-is-rebuild-stable-in-every-input-shape
  ;; ADR 0021 §4: the builder assocs the consumer's own input, so what the
  ;; differ compares is the input rather than a freshly minted closure. Each
  ;; input is rebuilt per call, the way a render-driven consumer rebuilds it.
  (doseq [[label mk-id] [["keyword"                #(keyword "id")]
                         ["camel-fallback keyword" #(keyword "record-id")]
                         ["top-level fn"           (constantly row-id-of)]
                         ["(raw f)"                #(grid/raw row-id-of)]]]
    (testing label
      (is (= (grid/with-row-id {} (mk-id))
             (grid/with-row-id {} (mk-id)))))))

(deftest with-selection-bundles-rowselection-object-and-coerces-mode
  (testing ":mode is coerced to the v32.2 string; friendly keys pass through"
    (is (= {:row-selection {:mode "multiRow" :header-checkbox true}}
           (grid/with-selection {} {:mode :multiple :header-checkbox true}))))
  (testing ":single -> singleRow"
    (is (= "singleRow" (get-in (grid/with-selection {} {:mode :single}) [:row-selection :mode]))))
  (testing ":mode defaults to :multiple when omitted"
    (is (= "multiRow" (get-in (grid/with-selection {} {}) [:row-selection :mode]))))
  (testing "an explicit AG Grid mode string passes through untouched"
    (is (= "singleRow" (get-in (grid/with-selection {} {:mode "singleRow"}) [:row-selection :mode])))))

(deftest with-pagination-enables-and-bundles-page-sizing
  (testing "no config just enables pagination"
    (is (= {:pagination true} (grid/with-pagination {}))))
  (testing "page-size and selector are written"
    (is (= {:pagination true :pagination-page-size 25 :pagination-page-size-selector [25 50 100]}
           (grid/with-pagination {} {:page-size 25 :page-size-selector [25 50 100]}))))
  (testing "auto-page-size wins over page-size (mutual exclusion) and warns"
    (let [out (atom nil)
          w   (capture #(reset! out (grid/with-pagination {} {:auto-page-size true :page-size 25})))]
      (is (= {:pagination true :pagination-auto-page-size true} @out)
          ":page-size is dropped when :auto-page-size is on")
      (is (= 1 (count w)))
      (is (re-find #"mutually exclusive" (first w)))))
  (testing "auto-page-size alone does not warn"
    (is (= [] (capture #(grid/with-pagination {} {:auto-page-size true})))))
  (testing "the conflict warning is bounded across a rebuilt options map"
    ;; agd-01kyqy2y8c7m / ADR 0022 §1. Builders run on every render for a
    ;; consumer who rebuilds the whole map — the shape ADR 0021 exists to
    ;; support — so before this warning had a period it fired per render.
    (let [w (capture #(dotimes [_ 200]
                        (grid/with-pagination {} {:auto-page-size true :page-size 25})))]
      (is (= 1 (count w)) "200 builder calls, one warning"))))

(deftest with-infinite-datasource-bundles-row-model-and-datasource
  (testing "row-model-type + datasource are set; cache sizing is optional"
    (let [gr   (fn [_])
          opts (grid/with-infinite-datasource {} gr)]
      (is (= "infinite" (:row-model-type opts)))
      (is (= gr (get-in opts [:datasource :get-rows])))
      (is (not (contains? opts :cache-block-size)))))
  (testing "cache sizing is written when supplied"
    (let [opts (grid/with-infinite-datasource {} (fn [_]) {:cache-block-size 50 :max-blocks-in-cache 4})]
      (is (= 50 (:cache-block-size opts)))
      (is (= 4 (:max-blocks-in-cache opts))))))

(deftest with-infinite-datasource-getrows-follows-callback-contract
  (testing "getRows sees kebab-bean params and calls the raw :success callback"
    (let [captured (atom nil)
          gr       (fn [params]
                     (is (= 0 (:start-row params)) "params arrive as a kebab-bean")
                     ((:success params) #js {:rowData (into-array [#js {:id 1}]) :rowCount 1}))
          js-opts  (convert/->js (grid/with-infinite-datasource {} gr))
          get-rows (unchecked-get (unchecked-get js-opts "datasource") "getRows")]
      (get-rows #js {:startRow 0 :endRow 100 :success (fn [r] (reset! captured r))})
      (is (= 1 (unchecked-get @captured "rowCount")))
      (is (array? (unchecked-get @captured "rowData"))))))

;; --- rebuild stability (ADR 0021) -------------------------------------------

(defn- on-cell-clicked
  "A top-level-def-d callback: the consumer's half of the rebuild-stability
  contract. A fresh inline lambda per render is theirs to keep stable."
  [_e]
  nil)

(defn- render-cell
  "A top-level-def-d render fn: the consumer's half for the renderer helpers.
  An inline (fn [params] ...) written per render is a genuinely new value."
  [p]
  (str (:value p)))

(defn- cell-init [state p] (reset! state (:value p)))
(defn- cell-gui [state] @state)

(defn- rebuilt-opts
  "An options map rebuilt from scratch by the same fn on every call, the shape a
  render-driven consumer produces. `:context`, `:get-row-id` and the
  `:cell-renderer` are the values that carry the test — all three were fresh
  objects per rebuild before ADR 0021, while the top-level callback and the
  plain-map `:column-defs` were already =."
  ([] (rebuilt-opts "ada"))
  ([filter-text]
   (-> (grid/options)
       (grid/with-columns [{:field :id}
                           {:field :first-name
                            :cell-renderer (render/dom-renderer render-cell)}])
       (grid/with-row-id :id)
       (assoc :quick-filter-text filter-text
              :context (grid/raw {:tenant "acme" :roles #{:admin}})
              :on-cell-clicked on-cell-clicked))))

(deftest builders-and-renderer-helpers-are-rebuild-stable
  ;; ADR 0021 §5 gives ADR 0009's admission bar a second clause: a public fn
  ;; contributing an option value must produce = output for = input. All eight
  ;; catalog entries plus the three renderer helpers (ADR 0011), each called
  ;; twice with the same arguments.
  (let [rows     #js [#js {:id 1}]
        get-rows (fn [_])]
    (doseq [[label build]
            [["(options)"                #(grid/options)]
             ["(options base)"           #(grid/options {:pagination true})]
             ["with-columns"             #(grid/with-columns {} [{:field :id}])]
             ["with-row-data"            #(grid/with-row-data {} rows)]
             ["with-row-id"              #(grid/with-row-id {} :id)]
             ["with-selection"           #(grid/with-selection {} {:mode :multiple})]
             ["with-pagination"          #(grid/with-pagination {} {:page-size 25})]
             ["with-cell-selection"      #(grid/with-cell-selection {} {:handle {:mode "fill"}})]
             ["with-infinite-datasource" #(grid/with-infinite-datasource {} get-rows {:cache-block-size 50})]
             ["renderer"                 #(render/renderer {:init cell-init :get-gui cell-gui})]
             ["dom-renderer"             #(render/dom-renderer render-cell)]
             ["react-renderer"           #(react/react-renderer render-cell)]
             ["portal-renderer"          #(react/portal-renderer render-cell)]]]
      (testing label
        (is (= (build) (build)))))))

(deftest update-grid!-is-clean-on-a-rebuilt-options-map
  (testing "rebuilding the whole map with the same inputs is a no-op diff"
    (let [h (handle (rebuilt-opts))
          w (capture #(grid/update-grid! h (rebuilt-opts)))]
      (is (= [] (set-grid-option-calls h))
          "every rebuilt value is = to the stashed one, so nothing ships")
      (is (= [] w)
          "no initial-only warning for :context, which did not semantically change")))
  (testing "positive control: one changed key produces exactly one call"
    (let [h (handle (rebuilt-opts))
          w (capture #(grid/update-grid! h (rebuilt-opts "grace")))]
      (is (= [[:set-grid-option "quickFilterText" "grace"]] (set-grid-option-calls h)))
      (is (= [] w)))))

(deftest update-grid!-stash-reflects-applied-state
  (testing "the returned handle's stash merges present new keys so later diffs stay minimal"
    (let [h0 (handle {:pagination false})
          h1 (grid/update-grid! h0 {:pagination true})]
      (is (= true (get-in h1 [:opts :pagination])))
      ;; re-applying the same value against the merged stash is a no-op
      (grid/update-grid! h1 {:pagination true})
      (is (= [[:set-grid-option "pagination" true]] (set-grid-option-calls h1))
          "only the first, genuinely-changing update called the api"))))

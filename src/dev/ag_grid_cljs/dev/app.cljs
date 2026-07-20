(ns ag-grid-cljs.dev.app
  "WALKING SKELETON dev app (ticket agd-01ky0ed83xww) — not library code.
  Proves: builder-produced EDN options -> conversion contract -> createGrid."
  (:require [ag-grid-cljs.core :as grid]
            ["ag-grid-community" :refer [AllCommunityModule]]))

;; Consumer owns module registration (must run before the first grid).
(defonce _modules (grid/register! AllCommunityModule))

;; Rows are JS by contract — camelCase props stay untouched.
(def row-data
  #js [#js {:firstName "Ada"    :lastName "Lovelace"  :born 1815 :salary 120000}
       #js {:firstName "Alan"   :lastName "Turing"    :born 1912 :salary 110000}
       #js {:firstName "Grace"  :lastName "Hopper"    :born 1906 :salary 115000}
       #js {:firstName "Edsger" :lastName "Dijkstra"  :born 1930 :salary 105000}
       #js {:firstName "Barbara" :lastName "Liskov"   :born 1939 :salary 125000}])

(def opts
  (-> (grid/options)
      (grid/with-default-col-def {:sortable true :flex 1})
      (grid/with-columns
        [;; keyword in value position: :first-name -> "firstName"
         {:field :first-name}
         {:field :last-name}
         {:field :born}
         {:field  :salary
          ;; fn survives conversion; params arrive as a lazy kebab bean —
          ;; (:col-def p) reads the camelCase colDef prop, nested access works.
          :value-formatter
          (fn [p]
            (str "$" (:value p) " (" (name (-> p :col-def :field)) ")"))}])
      (grid/with-row-data row-data)
      ;; keyword enum value: :auto-height -> "autoHeight"
      (assoc :dom-layout :auto-height)))

(defonce api* (atom nil))

(defn ^:export init []
  (let [el (js/document.getElementById "app")]
    (reset! api* (grid/create-grid el opts))
    (js/console.log "[skeleton] grid mounted, displayed rows:"
                    (.getDisplayedRowCount ^js @api*))))

(ns ag-grid-cljs.dev.app
  "WALKING SKELETON dev app — not library code.
  Proves: builder-produced EDN options -> conversion contract -> createGrid
  (ticket agd-01ky0ed83xww) and the three cell-renderer styles
  (ticket agd-01ky0ed8adbf): plain fn shorthand, hiccup dom-renderer,
  React local-root renderer."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.render :as render]
            [ag-grid-cljs.react :as agr]
            ["ag-grid-community" :refer [AllCommunityModule]]
            ["react" :as react]))

;; Consumer owns module registration (must run before the first grid).
(defonce _modules (grid/register! AllCommunityModule))

;; Rows are JS by contract — camelCase props stay untouched.
(def row-data
  #js [#js {:firstName "Ada"    :lastName "Lovelace"  :born 1815 :salary 120000}
       #js {:firstName "Alan"   :lastName "Turing"    :born 1912 :salary 110000}
       #js {:firstName "Grace"  :lastName "Hopper"    :born 1906 :salary 115000}
       #js {:firstName "Edsger" :lastName "Dijkstra"  :born 1930 :salary 105000}
       #js {:firstName "Barbara" :lastName "Liskov"   :born 1939 :salary 125000}])

;; Style 1 — plain fn shorthand (AG Grid's ICellRendererFunc): the converter
;; auto-wraps it, params arrive as a lazy kebab bean, a string return is used
;; as the cell's HTML. No helper needed.
(defn born-renderer [p]
  (str "★ " (:value p)))

;; Style 2 — dom-renderer: a component class under the hood
;; (init/getGui/refresh), refresh swaps content in place. DOM building is
;; BYO: any hiccup->DOM fn composes here; plain createElement shown.
(defn- el [tag style-str text]
  (let [e (js/document.createElement tag)]
    (set! (.-cssText (.-style e)) style-str)
    (when text (set! (.-textContent e) text))
    e))

(defn salary-renderer [p]
  (let [salary (:value p)
        pct    (* 100 (/ (- salary 100000) 30000))
        root   (el "div" "display:flex;align-items:center;gap:6px" nil)
        track  (el "div" "width:60px;height:8px;background:#e0e0e0;border-radius:4px" nil)
        bar    (el "div" (str "width:" pct "%;height:100%;background:#4caf50;border-radius:4px") nil)
        label  (el "span" "" (str "$" salary))]
    (.setAttribute root "class" "salary-cell")
    (.appendChild track bar)
    (.appendChild root track)
    (.appendChild root label)
    root))

;; Style 3 — React component in a per-cell local root: local state (the
;; click count) survives grid refreshes because refresh re-renders into the
;; same root instead of re-initing.
(defn- counter-fc [props]
  (let [[n set-n] (react/useState 0)]
    (react/createElement
     "button"
     #js {:className "counter-btn"
          :onClick   #(set-n (inc n))}
     (str (unchecked-get props "name") ": " n " clicks"))))

(defn actions-renderer [p]
  (react/createElement counter-fc #js {:name (-> p :data :first-name)}))

(def opts
  (-> (grid/options)
      (grid/with-default-col-def {:sortable true :flex 1})
      (grid/with-columns
        [{:field :first-name}
         {:field :last-name}
         {:field         :born
          :cell-renderer born-renderer}
         {:field         :salary
          :cell-renderer (render/dom-renderer salary-renderer)}
         {:header-name   "Actions"
          :cell-renderer (agr/react-renderer actions-renderer)}])
      (grid/with-row-data row-data)
      (assoc :dom-layout :auto-height)))

(defonce api* (atom nil))

(defn ^:export init []
  (let [el (js/document.getElementById "app")]
    (reset! api* (grid/create-grid el opts))
    (js/console.log "[skeleton] grid mounted, displayed rows:"
                    (.getDisplayedRowCount ^js @api*))))

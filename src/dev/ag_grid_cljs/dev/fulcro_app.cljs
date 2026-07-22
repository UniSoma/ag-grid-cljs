(ns ag-grid-cljs.dev.fulcro-app
  "WALKING SKELETON dev app — not library code (ticket agd-01ky0ed8766f).
  Proves the Fulcro reference-consumer bar with zero library support:

  - mount-point pattern: the grid lives in a Fulcro component that renders
    a stable div and blocks React diffing (shouldComponentUpdate false);
    Fulcro db->UI refreshes re-render the page around the grid, never
    through it.
  - explicit data channel: mutations update the normalized Fulcro db and
    push the same delta through grid/transact! (AG Grid row transactions,
    matched by :get-row-id) — no options diffing, grid state (scroll,
    selection, focus) survives.
  - transact!-from-a-cell: the Actions column is a react-renderer cell in a
    detached local root (no React context), so it calls comp/transact! with
    an EXPLICIT app reference (the APP defonce)."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.react :as agr]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [goog.object :as gobj]
            ["ag-grid-community" :refer [AllCommunityModule]]
            ["react" :as react]))

;; Consumer owns module registration (must run before the first grid).
(defonce _modules (grid/register! AllCommunityModule))

(defonce APP (app/fulcro-app))
(defonce handle* (atom nil))

;; --- Fulcro db is the source of truth; rows derive from entities -------------

(defn person->row
  "Fulcro entity -> JS row (rows are JS by contract). Row :id doubles as
  AG Grid's row id via :get-row-id."
  [{:person/keys [id name salary]}]
  #js {:id (str id) :name name :salary salary})

(def seed
  (vec (for [i (range 1 201)]
         {:id i :name (str "Person " i) :salary (+ 60000 (* 250 (mod i 40)))})))

;; --- mutations: swap the db, push the delta through the data channel ---------

(defmutation give-raise [{:person/keys [id]}]
  (action [{:keys [state]}]
          (let [person (-> (swap! state update-in [:person/id id :person/salary] + 1000)
                           (get-in [:person/id id]))]
            (when-let [handle @handle*]
              (grid/transact! handle {:update [(person->row person)]})))))

(defmutation add-person [_params]
  (action [{:keys [state]}]
          (let [id     (inc (reduce max 0 (keys (get @state :person/id))))
                person {:person/id id :person/name (str "New hire " id) :person/salary 90000}]
            (swap! state #(-> %
                              (assoc-in [:person/id id] person)
                              (update :root/people (fnil conj []) [:person/id id])))
            (when-let [handle @handle*]
              (grid/transact! handle {:add [(person->row person)] :add-index 0})))))

;; --- transact! from a cell: detached local root, explicit app reference ------

(defn raise-cell [p]
  (let [id (js/parseInt (-> p :data :id) 10)]
    (react/createElement
     "button"
     #js {:className "raise-btn"
          :onClick   #(comp/transact! APP [(give-raise {:person/id id})])}
     "+$1k")))

(defn grid-opts [rows]
  (-> (grid/options)
      (grid/with-default-col-def {:sortable true :flex 1})
      (grid/with-columns
        [{:field :name}
         {:field :salary}
         {:header-name   "Actions"
          :cell-renderer (agr/react-renderer raise-cell)}])
      (grid/with-row-data rows)
      (assoc :get-row-id    (fn [p] (-> p :data :id))
             :row-selection {:mode                   :single-row
                             :checkboxes             false
                             :enable-click-selection true})))

;; --- mount-point component ----------------------------------------------------

(defsc GridHost
  "Stable div the grid mounts into. shouldComponentUpdate false pins it:
  Fulcro refreshes never re-render below this point; all post-mount data
  flows through the explicit channel."
  [this {:keys [rows]}]
  {:shouldComponentUpdate (fn [_ _ _] false)
   :componentDidMount
   (fn [this]
     (let [el     (gobj/get this "grid-el")
           handle (grid/create-grid! el (grid-opts (:rows (comp/props this))))]
       (reset! handle* handle)
       (set! (.-agApi js/window) (grid/grid-api handle))))   ; hook for the headless check
   :componentWillUnmount
   (fn [_this]
     (when-let [handle @handle*]
       (grid/destroy! handle)
       (reset! handle* nil)))}
  (dom/div {:style {:height "320px"}
            :ref   (fn [r] (when r (gobj/set this "grid-el" r)))}))

(def ui-grid-host (comp/factory GridHost))

;; --- Fulcro app ----------------------------------------------------------------

(defsc Person [_ _]
  {:query         [:person/id :person/name :person/salary]
   :ident         :person/id
   :initial-state (fn [{:keys [id name salary]}]
                    {:person/id id :person/name name :person/salary salary})})

(defsc Root [this {:root/keys [people]}]
  {:query         [{:root/people (comp/get-query Person)}]
   :initial-state (fn [_] {:root/people (mapv #(comp/get-initial-state Person %) seed)})}
  (dom/div
   (dom/h3 "ag-grid-cljs — walking skeleton (Fulcro reference consumer)")
   ;; db-derived summary: proves Fulcro refreshes flow around the grid
   (dom/p {:id "summary"}
          (str (count people) " people · payroll $"
               (reduce + 0 (map :person/salary people))))
   (dom/button {:id      "add-person"
                :onClick #(comp/transact! this [(add-person {})])}
               "Add person")
   ;; full-swap path of the data channel: with :get-row-id set, AG Grid
   ;; diffs the new array by id — grid state survives the swap too
   (dom/button {:id      "sync-rows"
                :onClick #(grid/set-rows! @handle*
                                          (into-array (map person->row people)))}
               "Set rows from db")
   (ui-grid-host {:rows (into-array (map person->row people))})))

(defn ^:export init []
  (app/mount! APP Root "app"))

(ns ag-grid-cljs.dev.enterprise-app
  "WALKING SKELETON dev app — not library code (ticket agd-01ky0ed8fw7v).
  Proves the wrapper imposes nothing that blocks Enterprise:
  register enterprise modules via core/register!, install a license via
  enterprise/set-license-key!, then exercise cell range selection with the
  fill handle — the `with-range-fill` inspiration case — from the builder API.

  The license key is inlined at compile time from AG_GRID_LICENSE (see
  ag-grid-cljs.dev.license); with no key AG Grid still runs Enterprise but
  logs a license console error (the CI path)."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.enterprise :as ent]
            ["ag-grid-enterprise" :refer [AllEnterpriseModule LicenseManager]])
  (:require-macros [ag-grid-cljs.dev.license :refer [license-key]]))

(def ^:private license (license-key))

;; Install the license BEFORE any grid is created (blank on CI → error path).
(when (seq license)
  (ent/set-license-key! license))

;; Consumer owns module registration; AllEnterpriseModule pulls in Community
;; plus every Enterprise feature (CellSelectionModule included).
(defonce _modules (grid/register! AllEnterpriseModule))

(def row-data
  #js [#js {:firstName "Ada"     :lastName "Lovelace" :born 1815 :salary 120000}
       #js {:firstName "Alan"    :lastName "Turing"   :born 1912 :salary 110000}
       #js {:firstName "Grace"   :lastName "Hopper"   :born 1906 :salary 115000}
       #js {:firstName "Edsger"  :lastName "Dijkstra" :born 1930 :salary 105000}
       #js {:firstName "Barbara" :lastName "Liskov"   :born 1939 :salary 125000}])

(def opts
  (-> (grid/options)
      (grid/with-default-col-def {:sortable true :flex 1})
      (grid/with-columns
        [{:field :first-name}
         {:field :last-name}
         {:field :born}
         {:field :salary}])
      (grid/with-row-data row-data)
      ;; The Enterprise feature under test: cell range selection + fill handle,
      ;; expressed through the builder API (v32.2+ cellSelection object form).
      (grid/with-cell-selection {:handle {:mode "fill"}})
      (assoc :dom-layout :auto-height)))

(defonce api* (atom nil))

(defn ^:export init []
  (let [el      (js/document.getElementById "app")
        api     (grid/create-grid el opts)
        details (.getLicenseDetails LicenseManager (or license ""))]
    (reset! api* api)
    ;; Expose facts for the headless probe / manual inspection.
    (set! (.-__agEnterprise js/window)
          #js {:rows          (.getDisplayedRowCount ^js api)
               :cellSelection (boolean (.getGridOption ^js api "cellSelection"))
               :licensePresent (boolean (seq license))
               :licenseValid  (boolean (.-valid details))
               :licenseMissing (boolean (.-missing details))})
    (js/console.log "[enterprise-skeleton] mounted; license valid:"
                    (.-valid details) "cellSelection:"
                    (.getGridOption ^js api "cellSelection"))))

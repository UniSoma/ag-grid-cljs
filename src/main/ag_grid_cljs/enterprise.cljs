(ns ag-grid-cljs.enterprise
  "Opt-in AG Grid Enterprise support. Requires the `ag-grid-enterprise` peer
  package (in addition to `ag-grid-community`). This namespace exists so that
  the core wrapper never imports `ag-grid-enterprise` — consumers who only use
  Community pay nothing.

  Enterprise MODULES are registered by the consumer through `core/register!`
  (the consumer owns module registration), e.g.

      (require '[ag-grid-cljs.core :as grid]
               '[ag-grid-cljs.enterprise :as ent]
               '[\"ag-grid-enterprise\" :refer [AllEnterpriseModule]])

      (ent/set-license-key! \"<key>\")     ; before the first grid
      (grid/register! AllEnterpriseModule)"
  (:require ["ag-grid-enterprise" :refer [LicenseManager]]))

(defn set-license-key!
  "Install the AG Grid Enterprise license key. Must run before the first grid
  is created. With no key (or an invalid one) Enterprise features still work,
  but AG Grid logs a license console error and renders a watermark."
  [key]
  (.setLicenseKey LicenseManager key))

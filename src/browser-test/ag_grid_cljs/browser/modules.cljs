(ns ag-grid-cljs.browser.modules
  "One-time module registration + license install for the browser suite
  (ADR 0015). Registers AllEnterpriseModule — a superset of Community, so the
  community and Enterprise tests share one runtime — and installs the
  compile-time AG_GRID_LICENSE key if it was set; unset (CI) is the no-license
  path AG Grid Enterprise still runs under, whose console errors the Playwright
  driver allowlists."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.enterprise :as ent]
            ["ag-grid-community" :refer [setupAgTestIds]]
            ["ag-grid-enterprise" :refer [AllEnterpriseModule]])
  (:require-macros [ag-grid-cljs.dev.license :refer [license-key]]))

(def license (license-key))

(defonce _init
  (do
    (when (seq license) (ent/set-license-key! license))
    (grid/register! AllEnterpriseModule)
    ;; AG Grid's first-party test hook: emit data-testid on grid elements so the
    ;; suite selects via agTestIdFor, not version-brittle internal classes (ADR 0015 §2)
    (setupAgTestIds)
    true))

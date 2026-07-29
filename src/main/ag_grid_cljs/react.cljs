(ns ag-grid-cljs.react
  "React cell-renderer helper — walking-skeleton first cut (ticket
  agd-01ky0ed8adbf). Optional namespace: only consumers who require it need
  react/react-dom on their npm classpath; the core stays framework-agnostic.

  Mounts a local React root per cell (createRoot in init, unmount in
  destroy). renders go through flushSync so the cell has content
  synchronously when AG Grid attaches it — createRoot renders are async by
  default and would flash empty cells."
  (:require [ag-grid-cljs.impl.convert :as convert]
            ;; required for its :renderer construct method, the class builder
            [ag-grid-cljs.render]
            ["react-dom" :refer [flushSync]]
            ["react-dom/client" :refer [createRoot]]))

(defn- react-lifecycle [render-fn]
  {:init    (fn [state params]
              (let [el   (js/document.createElement "span")
                    root (createRoot el)]
                (flushSync #(.render root (render-fn params)))
                (reset! state {:el el :root root})))
   :get-gui (fn [state] (:el @state))
   :refresh (fn [state params]
              (flushSync #(.render (:root @state) (render-fn params)))
              true)
   :destroy (fn [state] (.unmount (:root @state)))})

;; This namespace owns its own construction tag, so `convert` never requires
;; `react` — that is what keeps react-dom off the classpath of consumers who
;; never require this namespace (ADR 0021 §4). The render fn is the payload for
;; the same reason it is in `dom-renderer`: the lifecycle map is four fresh
;; closures per call. Construction then goes through the `:renderer` method
;; rather than a fn exported from `render`, keeping one owner for the class.
(defmethod convert/construct :react-renderer [_ render-fn]
  (convert/construct :renderer (react-lifecycle render-fn)))

(defn react-renderer
  "(fn [params-bean] react-element) -> cellRenderer class, deferred to the
  conversion boundary (ADR 0021 §4).
  refresh re-renders into the same root (returns true), so React component
  local state survives value refreshes."
  [render-fn]
  (convert/deferred :react-renderer render-fn))

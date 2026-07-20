(ns ag-grid-cljs.react
  "React cell-renderer helper — walking-skeleton first cut (ticket
  agd-01ky0ed8adbf). Optional namespace: only consumers who require it need
  react/react-dom on their npm classpath; the core stays framework-agnostic.

  Mounts a local React root per cell (createRoot in init, unmount in
  destroy). renders go through flushSync so the cell has content
  synchronously when AG Grid attaches it — createRoot renders are async by
  default and would flash empty cells."
  (:require [ag-grid-cljs.render :as render]
            ["react-dom" :refer [flushSync]]
            ["react-dom/client" :refer [createRoot]]))

(defn react-renderer
  "(fn [params-bean] react-element) -> cellRenderer class (raw).
  refresh re-renders into the same root (returns true), so React component
  local state survives value refreshes."
  [render-fn]
  (render/renderer
   {:init    (fn [state params]
               (let [el   (js/document.createElement "span")
                     root (createRoot el)]
                 (flushSync #(.render root (render-fn params)))
                 (reset! state {:el el :root root})))
    :get-gui (fn [state] (:el @state))
    :refresh (fn [state params]
               (flushSync #(.render (:root @state) (render-fn params)))
               true)
    :destroy (fn [state] (.unmount (:root @state)))}))

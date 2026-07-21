(ns ag-grid-cljs.render
  "Cell-renderer helpers — walking-skeleton first cut (ticket agd-01ky0ed8adbf).
  Namespace layout and naming are provisional until the namespace-layout
  decision lands.

  AG Grid detects a component class via `candidate.prototype && 'getGui' in
  candidate.prototype`, so every helper here returns the class wrapped in
  (raw ...) — the converter's fn auto-wrapping would otherwise strip the
  prototype and silently degrade the class to a function renderer."
  (:require [ag-grid-cljs.impl.convert :as convert]))

;; No DOM-building engine lives here: consumers bring their own (any
;; hiccup->DOM fn composes with the render fn). Anything that isn't a
;; js/Node renders as text — string means text everywhere in the wrapper;
;; HTML-string semantics stays behind the bare-fn vanilla escape hatch.

(defn- ->node [x]
  (if (instance? js/Node x)
    x
    (js/document.createTextNode (str x))))

;; --- lifecycle-map renderer ---------------------------------------------------

(defn renderer
  "Low-level helper: lifecycle map -> cellRenderer component class (raw).
  Each grid-created instance gets a fresh state atom, passed to every
  lifecycle fn in place of `this`:

    {:init    (fn [state params] ...)   ; params is a lazy kebab bean
     :get-gui (fn [state] element)      ; required
     :refresh (fn [state params] bool)  ; optional; absent -> false (re-init)
     :destroy (fn [state] ...)}         ; optional"
  [{:keys [init get-gui refresh destroy]}]
  (let [ctor  (fn []
                (this-as ^js t (set! (.-agCljsState t) (atom nil)))
                ;; a JS constructor returning an object hijacks `new`;
                ;; return nil so `new` yields the instance itself
                nil)
        proto (.-prototype ctor)]
    (set! (.-init proto)
          (fn [params]
            (this-as ^js t (init (.-agCljsState t) (convert/params-bean params)))
            ;; AG Grid treats a non-null init return as a deferred-init
            ;; promise and calls .then on it — swallow the user fn's return
            nil))
    (set! (.-getGui proto)
          (fn [] (this-as ^js t (get-gui (.-agCljsState t)))))
    (set! (.-refresh proto)
          (fn [params]
            (this-as ^js t
                     (if refresh
                       (boolean (refresh (.-agCljsState t) (convert/params-bean params)))
                       false))))
    (set! (.-destroy proto)
          (fn [] (this-as ^js t (when destroy (destroy (.-agCljsState t))))))
    (convert/raw ctor)))

;; --- DOM renderer -------------------------------------------------------------

(defn dom-renderer
  "High-level helper: (fn [params-bean] js/Node | string) -> cellRenderer
  class (raw). A string renders as a text node — never HTML. The render
  result lives inside a <span> container so refresh can swap content in
  place (returns true)."
  [render-fn]
  (renderer
   {:init    (fn [state params]
               (let [container (js/document.createElement "span")]
                 (.appendChild container (->node (render-fn params)))
                 (reset! state container)))
    :get-gui (fn [state] @state)
    :refresh (fn [state params]
               (let [container @state]
                 (set! (.-textContent container) "")
                 (.appendChild container (->node (render-fn params)))
                 true))}))

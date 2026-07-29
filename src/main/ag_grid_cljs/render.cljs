(ns ag-grid-cljs.render
  "Cell-renderer helpers — walking-skeleton first cut (ticket agd-01ky0ed8adbf).
  Namespace layout and naming are provisional until the namespace-layout
  decision lands.

  AG Grid detects a component class via `candidate.prototype && 'getGui' in
  candidate.prototype`, so the class crosses the conversion boundary raw — the
  converter's fn auto-wrapping would otherwise strip the prototype and silently
  degrade the class to a function renderer.

  Every helper here returns a deferred value (ADR 0021 §4) holding the
  consumer's own input: the class is built when the converter reaches it, so two
  calls with the same input are `=` and a rebuilt options map does not re-apply
  `:column-defs`. A fresh class object reaches AG Grid only when the diff fired,
  which is exactly when re-creating cell components is acceptable."
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

(defn- lifecycle->class
  "The construction every tier ends at: lifecycle map -> component class."
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
    ctor))

(defmethod convert/construct :renderer [_ lifecycle]
  (lifecycle->class lifecycle))

(defn renderer
  "Low-level helper: lifecycle map -> cellRenderer component class, deferred.
  Each grid-created instance gets a fresh state atom, passed to every
  lifecycle fn in place of `this`:

    {:init    (fn [state params] ...)   ; params is a lazy kebab bean
     :get-gui (fn [state] element)      ; required
     :refresh (fn [state params] bool)  ; optional; absent -> false (re-init)
     :destroy (fn [state] ...)}         ; optional"
  [lifecycle]
  (convert/deferred :renderer lifecycle))

;; --- DOM renderer -------------------------------------------------------------

(defn- dom-lifecycle [render-fn]
  {:init    (fn [state params]
              (let [container (js/document.createElement "span")]
                (.appendChild container (->node (render-fn params)))
                (reset! state container)))
   :get-gui (fn [state] @state)
   :refresh (fn [state params]
              (let [container @state]
                (set! (.-textContent container) "")
                (.appendChild container (->node (render-fn params)))
                true))})

;; The render fn is the tag's payload, not the lifecycle map: these three
;; closures are fresh objects every call, so deferring at the `renderer` level
;; would leave the value non-= even for a perfectly stable render fn
;; (ADR 0021 §4).
(defmethod convert/construct :dom-renderer [_ render-fn]
  (lifecycle->class (dom-lifecycle render-fn)))

(defn dom-renderer
  "High-level helper: (fn [params-bean] js/Node | string) -> cellRenderer
  class, deferred. A string renders as a text node — never HTML. The render
  result lives inside a <span> container so refresh can swap content in
  place (returns true)."
  [render-fn]
  (convert/deferred :dom-renderer render-fn))

(ns ag-grid-cljs.browser.util
  "Shared helpers for the committed browser suite (ADR 0015): a sized, disposed
  mount container, and the gesture bridge the Playwright driver services so a
  cljs.test test can ask for a real mouse primitive (e.g. a wheel scroll) and
  await it."
  (:require ["ag-grid-community" :refer [agTestIdFor]]))

(def testid
  "AG Grid's first-party test-id computers (ADR 0015 §2): (.headerCell testid
  col-id), (.cell testid row-id col-id), (.fillHandle testid), etc. Each returns
  the data-testid string setupAgTestIds stamps onto that element."
  agTestIdFor)

(defn by-testid
  "querySelector `root` for the element carrying data-testid `id`
  (an agTestIdFor result)."
  [root id]
  (.querySelector root (str "[data-testid=" (pr-str id) "]")))

(defn mount-el
  "Create a fixed-size grid container attached to the document (real CSS layout,
  so AG Grid's row virtualization behaves). Returns the element."
  ([] (mount-el 300))
  ([height-px]
   (let [el (js/document.createElement "div")]
     (set! (.-cssText (.-style el))
           (str "width:600px;height:" height-px "px"))
     (.appendChild js/document.body el)
     el)))

(defn detach!
  "Remove a mount container from the document."
  [el]
  (when (.-parentNode el) (.removeChild js/document.body el)))

(defn next-frame
  "Promise resolved on the next animation frame — lets AG Grid flush a
  virtualization/scroll update before an assertion reads back the DOM."
  []
  (js/Promise. (fn [resolve] (js/requestAnimationFrame (fn [_] (resolve))))))

(defn request-gesture!
  "Post a gesture request onto window for the Playwright driver and return a
  promise resolved when the driver acks. `gesture` is an EDN map, e.g.
  {:type \"wheel\" :selector \"[data-testid=...]\" :dy 400}; it crosses to the
  driver as plain JSON."
  [gesture]
  (js/Promise.
   (fn [resolve _reject]
     (set! (.-__agGestureDone js/window) (fn [result] (resolve result)))
     (set! (.-__agGesture js/window) (clj->js gesture)))))

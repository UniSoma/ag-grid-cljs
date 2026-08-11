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

(defn poll-testid
  "Promise of the element carrying data-testid `id` under `root`. AG Grid's
  TestIdService stamps test ids on a debounce after gridReady, so a single
  animation frame can win the race; poll briefly instead."
  [root id]
  (js/Promise.
   (fn [resolve _]
     (let [deadline (+ (js/Date.now) 3000)]
       ((fn tick []
          (if-some [n (by-testid root id)]
            (resolve n)
            (if (< (js/Date.now) deadline)
              (js/setTimeout tick 25)
              (resolve nil)))))))))

(defn poll-until
  "Promise resolved once `pred` returns truthy, polled every 25ms until the
  deadline (default 2s) — then resolved anyway, so the assertion that follows
  reports the real shortfall instead of hanging the suite.

  Use this rather than a fixed frame count whenever the thing being awaited is
  an AG Grid event: AG Grid dispatches `cellValueChanged` and friends
  asynchronously, in a batch, and whether one animation frame is enough for the
  flush to land varies with machine load."
  ([pred] (poll-until pred 2000))
  ([pred timeout-ms]
   (js/Promise.
    (fn [resolve _]
      (let [deadline (+ (js/Date.now) timeout-ms)]
        ((fn tick []
           (if (or (pred) (>= (js/Date.now) deadline))
             (resolve nil)
             (js/setTimeout tick 25)))))))))

(defn await-microtask
  "Promise resolved at end of the current microtask queue — after the react
  cell-render flush lands (ag-grid-cljs.react batches cell renders into one
  flushSync per microtask: content lands before paint, not on the call stack)."
  []
  (js/Promise. (fn [resolve] (js/queueMicrotask (fn [] (resolve))))))

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

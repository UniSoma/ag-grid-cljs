(ns ag-grid-cljs.bench.browser-grid
  "Realistic browser measurements for the callback-bean literal-key fallback
  (ADR 0018 §7, ticket agd-01kygja77mxj): initial render plus 100k-row sort
  and quick-filter on a value-getter column, with the per-cell read going
  through (a) a raw JS getter, (b) the pre-fallback mechanical bean, and
  (c, d) the shipped fallback bean over camel and kebab rows.

  Not library code and not on the test path: `bb bench-browser` compiles this
  build in release mode and drives it with test/browser/bench.mjs. Recorded
  results live in docs/research/key-transform-benchmarks.md."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.impl.bean :as bean]
            [ag-grid-cljs.impl.convert :as convert]
            ["ag-grid-community" :refer [AllCommunityModule]]))

(defonce ^:private _modules (grid/register! AllCommunityModule))

(def ^:private row-count 100000)

(defn- make-rows
  "100k rows with a well-spread sortable value. `key-style` picks the property
  spelling: :camel -> firstName, :kebab -> \"first-name\"."
  [key-style]
  (let [arr #js []]
    (dotimes [i row-count]
      (let [v (str "name" (mod (* i 2654435761) 99991))]
        (.push arr (if (= key-style :camel)
                     #js {:id i :firstName v}
                     #js {:id i "first-name" v}))))
    arr))

(defn- mechanical-bean
  "The shipped params-bean as it stood before the fallback landed: mechanical
  kebab->camel lookup, no per-object resolver, no :transform."
  [o]
  (bean/bean o
             :prop->key (comp keyword convert/camel->kebab)
             :key->prop convert/lookup-prop
             :recursive true))

(defn- mechanical-wrap
  "Faithful clone of convert's pre-fallback auto-wrap: bean the object args
  with the mechanical bean, forward-convert the return. Keeps the pre/post
  comparison honest — both sides pay the same wrapper overhead."
  [f]
  (grid/raw
   (fn [& args]
     (convert/->js (apply f (map (fn [a] (if (object? a) (mechanical-bean a) a)) args))))))

(def ^:private read-first-name (fn [p] (:first-name (:data p))))

(def ^:private variants
  [{:label "raw JS getter, camel rows"
    :rows  :camel
    :vg    (grid/raw (fn [^js p] (.. p -data -firstName)))}
   {:label "auto-wrap, mechanical bean (pre-fallback), camel rows"
    :rows  :camel
    :vg    (mechanical-wrap read-first-name)}
   {:label "auto-wrap, fallback bean (shipped), camel rows"
    :rows  :camel
    :vg    read-first-name}
   {:label "auto-wrap, fallback bean (shipped), kebab rows"
    :rows  :kebab
    :vg    read-first-name}
   {:label "auto-wrap, fallback bean + :value-cache true, camel rows"
    :rows  :camel
    :vg    read-first-name
    :opts  {:value-cache true}}])

(defn- now [] (js/performance.now))

(defn- run-variant
  "Mount a 100k-row grid whose second column reads through `vg`, measure
  initial render (createGrid -> firstDataRendered), then the synchronous
  sort and quick-filter calls, tear down, and resolve."
  [{:keys [label rows vg opts]} pass]
  (js/Promise.
   (fn [resolve _]
     (let [el (js/document.createElement "div")]
       (set! (.-cssText (.-style el)) "width:800px;height:400px")
       (.appendChild js/document.body el)
       (let [t0 (now)
             on-rendered
             (fn [^js e]
               (let [api       (.-api e)
                     render-ms (- (now) t0)
                     t1        (now)
                     _         (.applyColumnState
                                api #js {:state #js [#js {:colId "vg" :sort "asc"}]
                                         :defaultState #js {:sort nil}})
                     sort-ms   (- (now) t1)
                     t2        (now)
                     _         (.setGridOption api "quickFilterText" "name123")
                     filter-ms (- (now) t2)]
                 (js/console.log
                  (str "RESULT|pass " pass "|" label
                       "|render " (.toFixed render-ms 1)
                       " ms|sort " (.toFixed sort-ms 1)
                       " ms|quickfilter " (.toFixed filter-ms 1) " ms"))
                 (.destroy api)
                 (.removeChild js/document.body el)
                 (resolve nil)))]
         (grid/create-grid!
          el
          (merge opts
                 {:column-defs [{:field :id}
                                {:col-id "vg" :header-name "vg" :value-getter vg}]
                  :row-data    (make-rows rows)
                  :on-first-data-rendered (grid/raw on-rendered)})))))))

(defn- settle
  "Give the page a GC (exposed by the driver via --js-flags=--expose-gc) and
  a beat between variants, so one variant's garbage is not billed to the next."
  []
  (js/Promise.
   (fn [resolve _]
     (when (exists? js/gc) (js/gc))
     (js/setTimeout resolve 300))))

(defn ^:export init []
  ;; Pass 1 warms module setup, the JIT, and the transform memo; record pass 2.
  (-> (reduce (fn [p [pass v]]
                (-> p (.then settle) (.then (fn [_] (run-variant v pass)))))
              (js/Promise.resolve nil)
              (for [pass [1 2] v variants] [pass v]))
      (.then (fn [_]
               (js/console.log "BENCH DONE")
               (set! (.-__benchDone js/window) true)))))

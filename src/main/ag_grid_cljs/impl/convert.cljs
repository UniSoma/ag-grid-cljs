(ns ag-grid-cljs.impl.convert
  "Forward EDN->JS options conversion per the conversion contract
  (ticket agd-01ky0eck96vn). Walking-skeleton cut: namespace layout and
  naming are provisional until the namespace-layout decision lands."
  (:require [clojure.string :as str]
            [cljs-bean.core :as bean]))

;; --- key transforms ---------------------------------------------------------

(defn kebab->camel
  "Pure mechanical transform: :row-data -> \"rowData\". Already-camel
  input passes unchanged (no segments to join)."
  [s]
  (let [[head & tail] (str/split s #"-")]
    (apply str head (map (fn [seg]
                           (if (seq seg)
                             (str (str/upper-case (subs seg 0 1)) (subs seg 1))
                             seg))
                         tail))))

(defn camel->kebab
  "Reverse transform for callback-params beans: \"rowIndex\" -> \"row-index\"."
  [s]
  (str/lower-case (str/replace s #"([a-z0-9])([A-Z])" "$1-$2")))

;; --- dev warnings -----------------------------------------------------------

(defn- warn [& msg]
  (when ^boolean goog.DEBUG
    (js/console.warn (apply str "[ag-grid-cljs] " msg))))

(def ^:private data-carrying-props
  ;; JS-by-contract nudge (contract rule 5)
  #{"rowData" "pinnedTopRowData" "pinnedBottomRowData" "context"})

;; --- raw escape hatch -------------------------------------------------------

(deftype Raw [x])

(defn raw
  "Sole escape hatch: the converter emits x untouched — no recursion,
  no renaming, no function wrapping."
  [x]
  (->Raw x))

(defn raw? [x]
  (instance? Raw x))

;; --- forward conversion -----------------------------------------------------

(declare ->js)

(defn params-bean
  "Lazy kebab-keyed view over a JS callback-params object. A view, not a
  copy: only accessed keys pay conversion; the underlying JS object is
  reachable via cljs-bean.core/object."
  [o]
  (bean/bean o
             :prop->key (comp keyword camel->kebab)
             :key->prop (comp kebab->camel name)
             :recursive true))

(defn- wrap-fn
  "Auto-wrap a user fn found in the options tree: JS-object args arrive as
  lazy kebab beans, the return value runs through the forward converter.
  (raw f) opts out entirely."
  [f]
  (fn [& args]
    (->js (apply f (map (fn [a] (if (object? a) (params-bean a) a)) args)))))

(defn- wrap-renderer-fn
  "Like wrap-fn, but dev-warns when the fn returns an HTML-looking string:
  vanilla AG Grid injects a function renderer's string return as innerHTML
  (documented vanilla behavior — the bare fn is the escape hatch with
  vanilla semantics). Structured cells belong in the renderer helpers."
  [f]
  (let [wrapped (wrap-fn f)]
    (fn [& args]
      (let [ret (apply wrapped args)]
        (when (and ^boolean goog.DEBUG (string? ret) (str/includes? ret "<"))
          (warn "cell renderer fn returned an HTML-looking string; AG Grid "
                "injects it via innerHTML (XSS risk with untrusted data). "
                "Return a DOM node, or use the renderer helpers for "
                "string-means-text semantics."))
        ret))))

(defn- renderer-prop? [prop]
  (or (= prop "cellRenderer") (.endsWith ^string prop "CellRenderer")))

(defn- key->prop [k]
  (when (namespace k)
    (warn "namespaced keyword " k " converts by name only; namespace dropped"))
  (kebab->camel (name k)))

(defn- map->js [m]
  (reduce-kv
   (fn [o k v]
     (let [prop (cond
                  (keyword? k) (key->prop k)
                  (string? k)  k
                  :else        (do (warn "non-keyword/string map key " (pr-str k) " stringified")
                                   (str k)))]
       (when (and (contains? data-carrying-props prop)
                  (coll? v) (not (raw? v)))
         (warn prop " received a CLJS collection; row data is JS by contract — "
               "pass a JS array (or wrap with raw / clj->js if intentional)"))
       (unchecked-set o prop (if (and (renderer-prop? prop) (fn? v) (not (raw? v)))
                               (wrap-renderer-fn v)
                               (->js v)))
       o))
   #js {}
   m))

(defn ->js
  "Type-driven recursion (contract rule 2): CLJS maps -> JS objects,
  CLJS sequentials -> JS arrays, keywords -> camelized strings,
  fns auto-wrapped, everything else untouched."
  [x]
  (cond
    (raw? x)        (.-x ^Raw x)
    (map? x)        (map->js x)
    (keyword? x)    (do (when (namespace x)
                          (warn "namespaced keyword " x " converts by name only; namespace dropped"))
                        (kebab->camel (name x)))
    (set? x)        (do (warn "CLJS set passed through unconverted (did you mean a vector?)")
                        x)
    (sequential? x) (let [a #js []]
                      (doseq [v x] (.push a (->js v)))
                      a)
    (fn? x)         (wrap-fn x)
    :else           x))

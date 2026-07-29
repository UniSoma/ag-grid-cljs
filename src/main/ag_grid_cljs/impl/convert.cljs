(ns ag-grid-cljs.impl.convert
  "Forward EDN->JS options conversion per the conversion contract
  (ticket agd-01ky0eck96vn). Walking-skeleton cut: namespace layout and
  naming are provisional until the namespace-layout decision lands."
  (:require [clojure.string :as str]
            [ag-grid-cljs.impl.bean :as bean]))

;; --- key transforms ---------------------------------------------------------

(defn kebab->camel
  "Pure mechanical transform: :row-data -> \"rowData\".

  Hot path (ADR 0018 §7): a dashless input — already-camel keys, and every
  common callback key — returns the argument itself, with no split and no
  intermediate strings."
  [^string s]
  (if (== -1 (.indexOf s "-"))
    s
    (let [segs (.split s "-")
          n    (alength segs)]
      (loop [i 1, acc (aget segs 0)]
        (if (< i n)
          (let [^string seg (aget segs i)]
            (recur (inc i)
                   (if (pos? (.-length seg))
                     (str acc (.toUpperCase (.charAt seg 0)) (subs seg 1))
                     acc)))
          acc)))))

(def ^:private ^js lower-only-re
  ;; A string made only of these characters is already its own lower-case form
  ;; and cannot match the camel-boundary regex, so camel->kebab is identity on
  ;; it. Deliberately ASCII-only: anything else (including non-ASCII
  ;; lower-case) takes the slow path and keeps its current result.
  #"^[a-z0-9_$-]*$")

(defn camel->kebab
  "Reverse transform for callback-params beans: \"rowIndex\" -> \"row-index\".

  Hot path (ADR 0018 §7): an already-lower-case input — every dashless
  callback key, and kebab-keyed consumer data — returns the argument itself,
  with no regex replacement and no lower-casing."
  [^string s]
  (if (.test lower-only-re s)
    s
    (str/lower-case (str/replace s #"([a-z0-9])([A-Z])" "$1-$2"))))

;; --- dev warnings -----------------------------------------------------------

(defn- warn [& msg]
  (when ^boolean goog.DEBUG
    (js/console.warn (apply str "[ag-grid-cljs] " msg))))

(def ^:private row-props
  ;; JS-by-contract nudge (contract rule 5)
  #{"rowData" "pinnedTopRowData" "pinnedBottomRowData"})

(def ^:private data-carrying-props
  (conj row-props "context"))

(defn- warn-cljs-collection
  "Nudge for a data-carrying prop handed a CLJS collection. Rows and context
  get different messages because they have different answers: rows are JS by
  contract (agd-01kygjg6avt2 — the code lives in the article, since a recipe
  is a row-shape/field pairing that does not compress into one line), while
  context does convert, just lossily."
  [prop]
  (if (contains? row-props prop)
    (warn prop " received a CLJS collection; row data is JS by contract — "
          "pass a JS array of JS objects. See \"Options and conversion\" for "
          "the two CLJS→JS row recipes.")
    (warn prop " received a CLJS collection; it converts through the boundary "
          "(keys camelize, keyword values become strings). Wrap with raw to "
          "get the CLJS value back unchanged in callbacks.")))

;; --- raw escape hatch -------------------------------------------------------

(deftype Raw [x tag]
  ;; Rebuild-stability contract (ADR 0021): a value the wrapper manufactures is
  ;; = to itself given = inputs, so a consumer who rebuilds the whole options map
  ;; per render gets a clean diff out of update-grid!'s = comparison (ADR 0008).
  ;; Without this, = fell through to identity and (raw m) was never = to (raw m).
  IEquiv
  (-equiv [_ other]
    (and (instance? Raw other)
         (= tag (.-tag ^Raw other))
         ;; = and not identical?: this is what makes two rebuilt (raw {...})
         ;; values equal. For wrapped fns and JS objects = degrades to identity
         ;; anyway, which is the strongest answer available for them.
         (= x (.-x ^Raw other))))

  IHash
  ;; From the tag ALONE, deliberately: the natural (hash [x tag]) would route a
  ;; wrapped JS object or function through goog/getUid, which MUTATES it with a
  ;; closure_uid_ property — and the values we wrap include consumer renderer
  ;; classes and callbacks. Colliding hashes are legal; mutating a consumer's
  ;; value to compute one is not. Hashing a Raw is off the hot path (it happens
  ;; only when an options map carrying one is itself hashed).
  (-hash [_] (hash tag)))

(defn raw
  "Sole escape hatch: the converter emits x untouched — no recursion,
  no renaming, no function wrapping."
  [x]
  (->Raw x nil))

(defn raw? [x]
  (instance? Raw x))

;; --- deferred values (ADR 0021 §4) ------------------------------------------
;; A helper that would mint a fresh closure or class per call stashes its INPUT
;; here instead and lets the boundary construct the real value, so equal inputs
;; give = options maps. Internal to impl: public raw keeps its single arity and
;; its verbatim meaning.

(defn deferred
  "Stash x under an internal construction tag. ->js runs `construct` on it when
  the conversion boundary reaches the wrapper."
  [tag x]
  (->Raw x tag))

(defmulti construct
  "Build the JS value a deferred input stands for. Open dispatch rather than a
  case so the renderer helpers can register their own construction and this
  namespace never has to require `render` or `react` — `react` is optional
  precisely so core consumers need not install react-dom. Load order is safe by
  construction: a tagged value can only exist if the namespace that mints it was
  loaded. :row-id is the exception that registers here, since its construction
  needs this namespace's own key transform and fn wrapper."
  (fn [tag _x] tag))

(defmethod construct :default [_ x] x)

;; --- forward conversion -----------------------------------------------------

(declare ->js)

;; --- callback-bean lookup ---------------------------------------------------

(def ^:private prop-cache-limit
  ;; Explicit bound (agd-01kygjftnhwa). Entries are dashed lookup keys, which in
  ;; practice are the keywords written in consumer callbacks — but a caller can
  ;; also reach here with a keyword built from runtime data, e.g.
  ;; (get (:data p) (keyword col-id)), so the bound is what keeps this small.
  512)

(def ^:private prop-cache (js/Map.))

(defn- cached-camel
  "kebab->camel for a dashed lookup key, memoized by name under an explicit
  bound. The reverse direction (prop->key) stays uncached: it receives
  arbitrary JS property names.

  Keying by name rather than by keyword identity is deliberate: dev builds do
  not hoist keyword constants, so an identity-keyed cache would miss on every
  lookup in exactly the build consumers develop against."
  [^string s]
  (let [hit (.get prop-cache s)]
    (if (undefined? hit)
      (let [prop (kebab->camel s)]
        ;; Reset rather than stop caching at the bound: keys derived from
        ;; runtime data must not be able to fill it once and permanently
        ;; starve the real lookup sites.
        (when (>= (.-size prop-cache) prop-cache-limit)
          (.clear prop-cache))
        (.set prop-cache s prop)
        prop)
      hit)))

(defn lookup-prop
  "kebab->camel over a callback-bean lookup key, memoized via cached-camel.
  Dashless keys — :value, :data, :node, :api, :id — return their name
  directly and never touch the cache."
  [k]
  (let [s (name k)]
    (if (== -1 (.indexOf s "-"))
      s
      (cached-camel s))))

(defn- bean-key->prop
  "Object-local lookup resolver (ADR 0018 §1): a keyword resolves to its
  camelized property when that property is present on o, otherwise to its
  literal name. Presence, not truthiness — a present nil/false/undefined
  camel value still wins. Dashless keys are their own camel form and skip
  both the transform and the presence test. Presence is own-property
  (Object.hasOwn), so inherited members — Object.prototype.valueOf under
  :value-of, toString under :to-string — cannot shadow a literal data key."
  [o]
  (fn [k]
    (let [s (name k)]
      (if (== -1 (.indexOf s "-"))
        s
        (let [camel (cached-camel s)]
          (if ^boolean (js/Object.hasOwn o camel) camel s))))))

(declare params-bean)

(def ^:private bean-prop->key (comp keyword camel->kebab))

(def ^:private nested-bean-cache
  ;; Nested-bean memo keyed weakly by the wrapped JS object (ADR 0018 §8:
  ;; bean identity is an implementation detail; equivalent views suffice).
  ;; The win is nested row objects, which are stable across the many callback
  ;; calls a sort or filter makes — without this every (:data p) rebuilds a
  ;; resolver closure and a bean per call (measured 3x on a 100k-row sort).
  ;; Scoped to transform-reached objects only: root callback params are fresh
  ;; per call, and millions of dead WeakMap keys cost more than they save.
  ;; Entries die with their objects; lookups still test presence per read,
  ;; so caching changes cost, not semantics.
  (js/WeakMap.))

(defn- bean-transform [x]
  (when (object? x)
    (or (.get nested-bean-cache x)
        (let [b (params-bean x)]
          (.set nested-bean-cache x b)
          b))))

(defn params-bean
  "Lazy kebab-keyed view over a JS callback object. A view, not a copy: only
  accessed keys pay conversion; the underlying JS object is reachable via
  ag-grid-cljs.impl.bean/object.

  Lookups follow the callback-bean law (ADR 0018): camel-first,
  literal-second, decided per object. The :transform hands every recursively
  reached plain object — nested objects and object elements inside arrays —
  its own object-aware bean, so each one tests presence on itself."
  [o]
  (bean/bean o
             :prop->key bean-prop->key
             :key->prop (bean-key->prop o)
             :recursive true
             :transform bean-transform))

(defn- wrap-arg [a]
  (if (object? a) (params-bean a) a))

(defn- wrap-fn
  "Auto-wrap a user fn found in the options tree: JS-object args arrive as
  lazy kebab beans, the return value runs through the forward converter.
  Fixed 0–3 arities avoid rest/map/apply allocation for common AG Grid
  callbacks; larger arities retain the same variadic behavior.
  (raw f) opts out entirely."
  [f]
  (fn
    ([] (->js (f)))
    ([a] (->js (f (wrap-arg a))))
    ([a b] (->js (f (wrap-arg a) (wrap-arg b))))
    ([a b c] (->js (f (wrap-arg a) (wrap-arg b) (wrap-arg c))))
    ([a b c & more]
     (->js (apply f (wrap-arg a) (wrap-arg b) (wrap-arg c)
                  (map wrap-arg more))))))

(defn- row-id-keyword-fn
  "getRowId over the raw JS row, following the callback-bean lookup law (ADR
  0018 §4): the camelized property when present on the row, the literal name
  otherwise — presence, not truthiness, and own-property so inherited members
  cannot shadow a literal data key. Reads the row directly, so the per-row hot
  path allocates no bean."
  [k]
  (let [literal (name k)
        camel   (kebab->camel literal)]
    (if (identical? literal camel)
      (fn [^js params] (str (unchecked-get (.-data params) literal)))
      (fn [^js params]
        (let [data (.-data params)]
          (str (unchecked-get data (if ^boolean (js/Object.hasOwn data camel)
                                     camel
                                     literal))))))))

(defmethod construct :row-id [_ id]
  (cond
    (keyword? id) (row-id-keyword-fn id)
    ;; (raw f): raw JS params, still str-coerced. A tagged Raw bypasses ->js's
    ;; generic fn auto-wrapping, so both fn branches state their own marshalling.
    (raw? id)     (let [f (.-x ^Raw id)]
                    (fn [params] (str (f params))))
    ;; What the converter would have applied to a bare fn value in the options
    ;; map: kebab-bean args, return through ->js (a no-op on the coerced string).
    :else         (wrap-fn (fn [params] (str (id params))))))

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
         (warn-cljs-collection prop))
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
    (raw? x)        (let [tag (.-tag ^Raw x)]
                      ;; The untagged case — every consumer (raw v), and the
                      ;; callback-return path this runs on per call (ADR 0005
                      ;; §7) — stays a direct field read, not a dispatch.
                      (if (nil? tag)
                        (.-x ^Raw x)
                        (construct tag (.-x ^Raw x))))
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

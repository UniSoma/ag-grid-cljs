(ns ag-grid-cljs.browser.react-context-test
  "Browser suite (ADR 0015): the react tier's context story (ticket
  agd-01kzva5kq1j5), pinned with a raw createContext — no design-system dep.

  Two contracts:

  1. A per-cell react-renderer root is DETACHED: a context-consuming component
     in a bare cell does not see an app-level provider value, even when the
     grid itself is rendered under that provider. The provider-wrap pattern
     (framework-composition.md \"Design-system components in cells\") is what
     delivers a value into the cell.

  2. The dev diagnostic: an uncaught cell render error warns once per process
     per distinct message via warn-once! (site ::react/cell-render-error), with
     the provider-shaped hint when the message matches the
     'Provider was not found' class — and the cell observably paints empty."
  (:require [ag-grid-cljs.core :as grid]
            [ag-grid-cljs.react :as agr]
            [ag-grid-cljs.impl.warn :as warn]
            [ag-grid-cljs.browser.modules]
            [ag-grid-cljs.browser.util :as u]
            [clojure.string :as str]
            [cljs.test :refer [deftest is testing async]]
            ["react" :as react]
            ["react-dom/client" :refer [createRoot]]))

(def ^:private ctx (react/createContext "detached-default"))

(defn- ctx-probe
  "Function component: renders useContext's value into a span classed by
  props.probe, so the test reads which value actually reached the cell."
  [props]
  (react/createElement "span" #js {:className (unchecked-get props "probe")}
                       (react/useContext ctx)))

(def ^:private bare-cell
  (agr/react-renderer
   (fn [_p] (react/createElement ctx-probe #js {:probe "bare-probe"}))))

;; the wrap pattern, exactly as documented: a namespace-level def whose render
;; fn wraps its output in the provider the cell content needs
(def ^:private wrapped-cell
  (agr/react-renderer
   (fn [_p]
     (react/createElement (.-Provider ctx) #js {:value "from-wrap"}
                          (react/createElement ctx-probe #js {:probe "wrap-probe"})))))

(defn- grid-host
  "React FC: a sized container whose useEffect creates a two-column grid (bare
  cell + provider-wrapped cell) and whose cleanup destroys it."
  [_props]
  (let [ref (react/useRef nil)]
    (react/useEffect
     (fn []
       (let [h (grid/create-grid!
                (.-current ref)
                (-> (grid/options)
                    (grid/with-columns
                      [{:header-name "Bare" :col-id "bare" :cell-renderer bare-cell}
                       {:header-name "Wrapped" :col-id "wrapped" :cell-renderer wrapped-cell}])
                    (grid/with-row-id :id)
                    (grid/with-row-data #js [#js {:id "1"}])))]
         (fn [] (grid/destroy! h))))
     #js [])
    (react/createElement "div" #js {:ref   ref
                                    :style #js {:width "600px" :height "300px"}})))

(deftest bare-cell-is-detached-wrap-delivers
  (testing "an app-level provider value does not reach a bare cell; the wrap
            pattern's value does"
    (let [el        (u/mount-el)
          host-root (createRoot el)]
      ;; the grid is rendered UNDER an app-level provider — the strongest form
      ;; of the detachment claim
      (.render host-root
               (react/createElement (.-Provider ctx) #js {:value "from-app"}
                                    (react/createElement grid-host)))
      (async done
             (-> (u/poll-until #(and (.querySelector el ".bare-probe")
                                     (.querySelector el ".wrap-probe")))
                 (.then (fn [_]
                          (is (= "detached-default"
                                 (some-> (.querySelector el ".bare-probe") .-textContent))
                              "bare cell sees the createContext default, not the app-level value")
                          (is (= "from-wrap"
                                 (some-> (.querySelector el ".wrap-probe") .-textContent))
                              "provider-wrapped cell sees the wrap's value")
                          (.unmount host-root)
                          (u/next-frame)))
                 (.then (fn [_]
                          (u/detach! el)
                          (done))))))))

(def ^:private boom-msg
  "BoomProvider was not found in component tree (browser-suite probe)")

(defn- boom [_props] (throw (js/Error. boom-msg)))

(def ^:private throwing-cell
  (agr/react-renderer (fn [_p] (react/createElement boom))))

(deftest uncaught-cell-error-warns-once-with-provider-hint
  (testing "two throwing cells => ONE dev warning carrying the provider hint,
            and the cells paint empty"
    (let [el        (u/mount-el)
          warns     (atom [])
          orig-warn (.-warn js/console)
          relevant  #(filter (fn [w] (str/includes? w "react-renderer cell threw")) %)
          _         (set! (.-warn js/console)
                          (fn [& args] (swap! warns conj (apply str args))))
          h         (grid/create-grid!
                     el
                     (-> (grid/options)
                         (grid/with-columns
                           [{:field :id}
                            {:header-name "Boom" :col-id "boom" :cell-renderer throwing-cell}])
                         (grid/with-row-id :id)
                         (grid/with-row-data #js [#js {:id "1"} #js {:id "2"}])))]
      (async done
             (-> (u/poll-until #(seq (relevant @warns)))
                 ;; a frame for the second row's cell to render (and NOT warn)
                 (.then (fn [_] (u/next-frame)))
                 (.then (fn [_]
                          (set! (.-warn js/console) orig-warn)
                          (let [hits (relevant @warns)]
                            (is (= 1 (count hits))
                                "warn-once!: one warning for two cells throwing the same error")
                            (is (some-> (first hits) (str/includes? "[ag-grid-cljs]"))
                                "carries the library prefix")
                            (is (some-> (first hits) (str/includes? "Design-system components in cells"))
                                "provider-shaped message appends the wrap-pattern hint"))
                          (is (contains? (warn/fired)
                                         [:ag-grid-cljs.react/cell-render-error boom-msg])
                              "fired under the ::react/cell-render-error site, keyed by message")
                          (is (= "" (some-> (u/by-testid el (.cell u/testid "1" "boom"))
                                            .-textContent))
                              "the throwing cell paints empty — React unmounted its root")
                          (grid/destroy! h)
                          (u/detach! el)
                          (done))))))))

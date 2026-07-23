(ns build
  (:require [clojure.data.xml :as xml]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'io.github.unisoma/ag-grid-cljs)
(def version "0.1.0-SNAPSHOT")
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn- pom-basis
  "A basis whose :deps are empty, so the published pom declares zero Maven
  dependencies (ADR 0016 decision 1)."
  []
  (b/create-basis {:root nil :user nil :project nil :extra {:deps {}} :aliases []}))

(defn- scm-tag
  "The built commit SHA for a -SNAPSHOT version, else v<version> (ADR 0016
  decision 1)."
  []
  (if (str/ends-with? version "-SNAPSHOT")
    (b/git-process {:git-args "rev-parse HEAD"})
    (str "v" version)))

(defn- pom-path []
  (b/pom-path {:lib lib :class-dir class-dir}))

(defn- expand-empty-elements!
  "cljdoc's Jsoup parser drops self-closing empty elements (e.g. an
  <scm/>-style block), so re-serialize the pom and rewrite every <foo/> as
  <foo></foo> — data.xml's emitter self-closes empties on its own, so the
  rewrite is what actually guarantees no element collapses (ADR 0016
  decision 1)."
  [path]
  (let [pom (xml/indent-str (xml/parse-str (slurp path)))]
    (spit path (str/replace pom #"<([^\s/>]+)([^>]*?)\s*/>" "<$1$2></$1>"))))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis (pom-basis)
                :scm {:url "https://github.com/UniSoma/ag-grid-cljs"
                      :connection "scm:git:git://github.com/UniSoma/ag-grid-cljs.git"
                      :developerConnection "scm:git:ssh://git@github.com/UniSoma/ag-grid-cljs.git"
                      :tag (scm-tag)}
                :pom-data [[:licenses
                            [:license
                             [:name "MIT License"]
                             [:url "https://opensource.org/licenses/MIT"]]]]})
  (expand-empty-elements! (pom-path))
  ;; source-only: no AOT, no CLJS->JS compile
  (b/copy-dir {:src-dirs ["src/main"] :target-dir class-dir})
  ;; LICENSE + THIRD-PARTY.md at the jar root
  (b/copy-file {:src "LICENSE" :target (str class-dir "/LICENSE")})
  (b/copy-file {:src "THIRD-PARTY.md" :target (str class-dir "/THIRD-PARTY.md")})
  (b/jar {:class-dir class-dir :jar-file jar-file}))

(defn install [_]
  (jar nil)
  (b/install {:basis (pom-basis)
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy [_]
  (jar nil)
  (dd/deploy {:installer :remote
              :artifact jar-file
              :pom-file (pom-path)}))

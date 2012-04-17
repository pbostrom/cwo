(defn cljs-home [path]
    (if-let [home (get (System/getenv) "CLOJURESCRIPT_HOME")]
          (str home path)
          (throw (Exception. "You must set the $CLOJURESCRIPT_HOME variable!"))))

(defproject codegroup "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [aleph "0.2.1-beta1"]
                 [compojure "1.0.1"]
                 [hiccup "1.0.0-beta1"]
                 [crate "0.1.0-alpha2"]
                 [domina "1.0.0-beta1"]
                 [ring "1.1.0-RC1"]]
  :plugins [[lein-cljsbuild "0.1.2"]]
  :extra-classpath-dirs ~(map cljs-home ["/lib/*" "/src/clj" "/src/cljs"])
  :cljsbuild {
    :builds [{:source-path "src-cljs"
              :compiler {:libs ["goog/dom/query.js"]
                         :output-to "resources/public/js/bootstrap.js"
                         :optimizations :whitespace
                         :pretty-print true}}]}
  :main cwo.server)

(defproject cwo "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [aleph "0.3.0-alpha2"]
                 [noir "1.3.0-beta4"]
                 [compojure "1.0.1"]
                 [hiccup "1.0.0-beta1"]
                 [enlive "1.0.0"]
                 [crate "0.1.0-alpha2"]
                 [clojail "0.6.0"]
;                 [org.python/jython-standalone "2.5.2"]
                 [ring "1.1.0-RC1"]]
  ;:plugins [[lein-cljsbuild "0.1.2"]]
  :plugins [[lein-cljsbuild "0.2.1"]]
  :cljsbuild {
    :builds [{:source-path "src-cljs"
              :compiler {:output-to "resources/public/js/cljs-compiled.js"
;                         :optimizations :whitespace
                         ;:optimizations :advanced
                         :pretty-print true}}]}
  :jvm-opts ["-Djava.security.policy=example.policy""-Xmx80M"]
  :main cwo.server)

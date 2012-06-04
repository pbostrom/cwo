(defproject codegroup "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [aleph "0.3.0-alpha2"]
                 [noir "1.3.0-beta4"]
                 [compojure "1.0.1"]
                 [hiccup "1.0.0-beta1"]
                 [enlive "1.0.0"]
                 [crate "0.1.0-alpha2"]
                 [clojail "0.5.1"]
                 [ring "1.1.0-RC1"]]
  :plugins [[lein-cljsbuild "0.1.2"]]
  :cljsbuild {
    :builds [{:source-path "src-cljs"
              :compiler {:output-to "resources/public/js/cljs-compiled.js"
                         :optimizations :whitespace
                         :pretty-print true}}]}
  :jvm-opts ["-Djava.security.policy=example.policy""-Xmx80M"]
  :main cwo.server)

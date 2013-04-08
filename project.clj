(defproject cwo "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/core.logic "0.8.0-rc2"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/core.cache "0.6.3"]
                 [aleph "0.3.0-beta11"]
                 [compojure "1.1.3"]
                 [enlive "1.0.1"]
                 [clojail "1.0.5"]
                 [clj-http "0.4.3"]
                 [cheshire "5.0.1"]
                 [midje "1.4.0"]
                 [org.flatland/useful "0.9.3"]
                 [ring "1.1.5"]
                 [com.cemerick/piggieback "0.0.4"]
                 [crate "0.2.4"]]
  :plugins [[lein-cljsbuild "0.3.0"]]
  :pedantic :warn
  :cljsbuild
  {:builds
   [{:source-paths ["src-cljs"],
     :compiler
     {:pretty-print true,
      :output-to "resources/public/js/cljs-compiled.js",
      :externs
      ["externs/jqconsole-2.7.js"
       "externs/twitter-bootstrap.js"
       "externs/jquery-1.7.externs.js"],
      :optimizations :whitespace}}]}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :jvm-opts ["-Djava.security.policy=example.policy""-Xmx80M"]
  :vimclojure-opts {:repl true}
  :main cwo.server)

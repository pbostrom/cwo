(defproject cwo "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.logic "0.7.5"]
                 [aleph "0.3.0-beta3"]
                 [compojure "1.1.3"]
                 [enlive "1.0.0"]
                 [crate "0.1.0-alpha2"]
                 [clojail "1.0.1"]
                 [clj-http "0.4.3"]
                 [midje "1.4.0"]
;                 [org.python/jython-standalone "2.5.2"]
                 [ring "1.1.5"]]
  :plugins [[lein-cljsbuild "0.3.0"]]
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
      ;:optimizations :whitespace
      :optimizations :advanced}}]}
  :jvm-opts ["-Djava.security.policy=example.policy""-Xmx80M"]
  :vimclojure-opts {:repl true}
  :main cwo.server)

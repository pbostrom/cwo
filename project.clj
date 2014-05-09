(defproject cwo "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2156"]
                 [org.clojure/core.logic "0.8.4"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/tools.reader "0.7.10"]
                 [org.clojure/tools.trace "0.7.5"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.3.6-SNAPSHOT"]
                 [aleph "0.3.0-beta11"]
                 [compojure "1.1.3"]
                 [com.taoensso/carmine "2.1.3"]
                 [enlive "1.1.5"]
                 [clojail "1.0.6"]
                 [clj-http "0.7.3"]
                 [cheshire "5.0.1"]
                 [midje "1.4.0"]
                 [org.flatland/useful "0.9.3"]
                 [ring "1.1.5"]
;                 [mavericklou/oauth-clj "0.1.4.1"]
                 [oauth-clj "0.1.8"]
                 [overtone/at-at "1.2.0"]
                 [clj-time "0.6.0"]
                 [sonian/carica "1.0.3"]
                 [crate "0.2.4"]]
  :plugins [[lein-cljsbuild "1.0.0-alpha2"]]
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.3"]]
                   :resource-paths ["dev-config"]}
             :prod {:resource-paths ["config"]
                    :dependencies [[org.clojure/tools.nrepl "0.2.3"]]}}
  :pedantic :warn
  :cljsbuild
  {:builds
   {:prod {:source-paths ["src-cljs"],
           :compiler
           {:pretty-print false
            :output-to "resources/public/js/cljs-compiled.js"
            :externs
            ["externs/jqconsole-2.7.7.min.js"
             "externs/twitter-bootstrap.js"
             "externs/jquery-1.8.js"]
            :optimizations :advanced}}
    :dev {:source-paths ["src-cljs" "cljs-repl"]
           :compiler
           {:pretty-print true
            :output-to "resources/public/js/cljs-compiled.js"
            :optimizations :whitespace}}}}
  :jvm-opts ["-Djava.security.policy=example.policy" "-Xmx256M"]
  :main cwo.server)

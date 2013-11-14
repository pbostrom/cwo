(defproject cwo "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1913"]
                 [org.clojure/core.logic "0.8.4"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/tools.reader "0.7.8"]
                 [org.clojure/tools.trace "0.7.5"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [aleph "0.3.0-beta11"]
                 [compojure "1.1.3"]
                 [com.taoensso/carmine "2.1.3"]
                 [enlive "1.0.1"]
                 [clojail "1.0.6"]
                 [clj-http "0.7.3"]
                 [cheshire "5.0.1"]
                 [midje "1.4.0"]
                 [org.flatland/useful "0.9.3"]
                 [ring "1.1.5"]
                 [mavericklou/oauth-clj "0.1.4.1"]
                 [overtone/at-at "1.2.0"]
                 [clj-time "0.6.0"]
                 [crate "0.2.4"]]
  :plugins [[lein-cljsbuild "0.3.3"]]
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.1"]]}}
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
  :jvm-opts ["-Djava.security.policy=example.policy" "-Xmx128M"]
  :main cwo.server)

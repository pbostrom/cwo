(ns cwo.socket
  (:use [cwo.utils :only (jq jslog)]))

(def sock (atom nil))

(defn eval-clj [expr sb]
  (.send @sock (pr-str [:eval-clj [expr sb]])))

(defn subscribe [handle]
  (.text (jq "#others-repl span.jqconsole-header > span") (str handle "'s REPL\n"))
  (.send @sock (pr-str [:subscribe handle])))

(defn transfer [handle]
  (.send @sock (pr-str [:transfer handle])))

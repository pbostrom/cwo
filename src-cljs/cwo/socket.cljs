(ns cwo.socket
  (:use [cwo.utils :only (jq jslog)]))

(def sock (atom nil))

(defn eval-clj [expr sb]
  (.send @sock (pr-str [:eval-clj [expr sb]])))

(defn subscribe [handle]
  (.send @sock (pr-str [:subscribe handle])))

(defn transfer [handle]
  (.send @sock (pr-str [:transfer handle])))

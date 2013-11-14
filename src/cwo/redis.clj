(ns cwo.redis
  (:require [taoensso.carmine :as car :refer (wcar)]))

;(def server1-conn {:pool {<opts>} :spec {<opts>}})
(defmacro wcar* [& body] `(car/wcar {} ~@body))

(defn set [k v]
  (println "redis set: " k v)
  (wcar* (car/set k v)))

(defn get [k]
  (wcar* (car/get k)))

(defn hset [k f v]
  (wcar* (car/hset k f v)))

(defn incr [k]
  (wcar* (car/incr k)))

(defn all-keys []
  (wcar* (car/keys "*")))

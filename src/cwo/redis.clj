(ns cwo.redis
  (:require [taoensso.carmine :as car :refer (wcar)]))

;(def server1-conn {:pool {<opts>} :spec {<opts>}})
(defmacro wcar* [& body] `(car/wcar {} ~@body))

(defn set [sesh-id hist]
  (println "redis set: " sesh-id hist)
  (wcar* (car/set sesh-id hist)))

(defn get [sesh-id]
  (wcar* (car/get sesh-id)))

(defn hset [k f v]
  (wcar* (car/hset k f v)))

(defn incr [k]
  (wcar* (car/incr k)))

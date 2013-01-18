(ns cwo.mongo
  (:require [monger.core :as mg]
            [monger.operators :refer [$set $unset]]
            [monger.collection :as mc]))

(defn connect! []
  (mg/connect!)
  (mg/set-db! (mg/get-db "cwo")))

(defn reset-db! []
  (mc/remove "users"))

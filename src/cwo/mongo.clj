(ns cwo.monger 
  (:require [monger.core :as mg])
  (:import [com.mongodb MongoOptions ServerAddress]))

(defn connect! []
  (mg/connect!)
  (mg/set-db! (mg/get-db "cwo")))

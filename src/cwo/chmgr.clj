(ns cwo.chmgr
  (:require [lamina.core :as lamina]))

(def channels (atom {}))
(def handles (atom {}))

(defn get-ch [sesh-id]
  (if-let [ch (@channels sesh-id)]
    (ch :master)
    (let [newch (lamina/channel* :grounded? true :permanent? true)]
      (swap! channels assoc sesh-id {:master newch})
      newch)))

;(defn set-handle [sesh-id]

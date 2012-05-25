(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]))

(def channels (atom {}))
(def handles (atom {}))

(defn get-ch [sesh-id]
  (if-let [ch (@channels sesh-id)]
    (ch :master)
    (let [newch (lamina/channel* :grounded? true :permanent? true)]
      (swap! channels assoc sesh-id {:master newch})
      newch)))

(defn register []
  (let [sesh-id (session/get "sesh-id")
        handle (session/get "handle")
        ch (get-ch sesh-id)]
    (when handle (swap! handles assoc handle ch))
    ch))

;(defn set-handle [sesh-id]

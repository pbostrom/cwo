(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]))

(def channels (atom {}))
(def handles (atom {}))

(def global-ch (lamina/channel* :permanent? true))

(defn get-ch [sesh-id]
  (if-let [ch (@channels sesh-id)]
    (ch :master)
    (let [newch (lamina/channel* :grounded? true :permanent? true)]
      (lamina/siphon global-ch newch)
      (swap! channels assoc sesh-id {:master newch})
      newch)))

(defn register []
  (let [handle (session/get "handle")
        ch (get-ch (session/get "sesh-id"))]
    (when handle 
      (swap! handles assoc handle ch)
      (lamina/enqueue global-ch handle))
    ch))

;(defn set-handle [sesh-id]

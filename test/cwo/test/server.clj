(ns cwo.test.server
  (:require [midje.sweet :refer [fact background]]
            [cwo.server :as server]
            [lamina.core :as lamina]
            [noir.session :as session :refer [*noir-session*]]
            [cwo.mongo :as mg]
            [cwo.chmgr :as chmgr]))


;(background (around :facts (with-redefs [*noir-session* (atom {"sesh-id" mock-sesh-id})] ?form)))
(fact "socket handler" (fn? (server/get-handler)) => true)

(defn srv-cmd 
  "Sends a command to the websocket handler"
  [socket cmd]
  (lamina/enqueue (:srv socket) (pr-str cmd)))

(defn init-client [id handler]
  (let [client (apply assoc {:id id} (interleave [:srv :cl] (lamina/channel-pair)))]
    (with-redefs [*noir-session* (atom {"sesh-id" (:id client)})]
      (handler (:srv client) nil)
      (fact (str "Init client" id) (session/get "sesh-id") => (:id client)))
    client))

(mg/connect!)
(mg/reset-db!)

(let [handler (server/get-handler)
      client1 (init-client "777" handler)
      client2 (init-client "888" handler)]
  
  (srv-cmd client1 [:login "joe"])

  (srv-cmd client2 [:eval-clj ["(range 10)" :you]])
  (take 3 (lamina/lazy-channel-seq (:cl client2))))

;  
;  (lamina/enqueue (:ch client2) (pr-str [:login "joe"]))
;  (lamina/enqueue (:ch client1) (pr-str [:subscribe "joe"]))

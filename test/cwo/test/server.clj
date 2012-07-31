(ns cwo.test.server
  (:require [midje.sweet :refer [fact]]
            [cwo.server :as server]
            [lamina.core :as lamina :refer [receive filter* channel?]]
            [lamina.viz :as lv]
            [noir.session :as session :refer [*noir-session*]]
            [cwo.mongo :as mg]
            [cwo.models.user :as user]
            [cwo.chmgr :as chmgr]))

(defn srv-cmd 
  "Sends a command to the websocket handler"
  [socket cmd]
  (lamina/enqueue (:cl socket) (pr-str cmd)))

(defn init-client [id handler]
  (let [client (apply assoc {:id id} (interleave [:srv :cl] (lamina/channel-pair)))]
    (with-redefs [*noir-session* (atom {"sesh-id" (:id client)})]
      (handler (:srv client) nil)
      (fact (str "Init client" id) (session/get "sesh-id") => (:id client)))
    client))

(mg/connect!)
(mg/reset-db!)

(def handler (server/get-handler)) 
(def client1 (init-client "777" handler)) 
(def client2 (init-client "888" handler)) 
(def hdl2 "joe")  

(fact "client1 websocket uplink exists" 
  (channel? (:srv client1)) => true)
(srv-cmd client2 [:login hdl2])
(fact "client2 handle is registered" 
  (user/get-handle (:id client2)) => hdl2)
(srv-cmd client1 [:subscribe "joe"])

(defn get-msg [ch filt]
  (receive (filter* #(chmgr/cmd? % filt) ch) (fn [msg] msg)))

(srv-cmd client2 [:eval-clj ["(range 10)" :you]])
(let [hist-msg (get-msg (:cl client1) :hist)] 
  (fact "Subscribe eval history"
    (read-string (second (read-string hist-msg))) => ["(range 10)" "(0 1 2 3 4 5 6 7 8 9)"]))

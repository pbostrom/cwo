(ns cwo.test.server
  (:require [midje.sweet :refer [fact]]
            [cwo.server :as server]
            [lamina.core :as lamina :refer [receive filter* channel?]]
            [lamina.viz :as lv]
            [noir.session :as session :refer [*noir-session*]]
            [cwo.mongo :as mg]
            [monger.collection :as mgc]
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

(defn pop-msg
 "Decodes first message in client channel"
  [client]
  (read-string (receive (:cl client) (fn [x] x))))

(defn contains-all? [coll keyz]
  (reduce #(and %1 (contains? coll %2)) true keyz))

;(mg/connect!)
;(mg/reset-db!)

(def handler (server/get-handler)) 
(def client1 (init-client "777" handler)) 
(def client2 (init-client "888" handler)) 
(def hdl1 "bob")
(def hdl2 "joe")
(def expr "(range 10)")
(def result (pr-str (range 10)))

(def msg-store1 (atom #{}))
(def msg-store2 (atom #{}))

(lamina/receive-all (:cl client1) #(swap! msg-store1 conj (read-string %)))
(lamina/receive-all (:cl client2) #(swap! msg-store2 conj (read-string %)))

(fact "client1 websocket uplink exists" 
  (channel? (:srv client1)) => true)

(fact "Received initial handles list" 
  (contains? @msg-store2 [:inithandles nil]) => true)

(srv-cmd client2 [:login hdl2])
(fact "Received adduser msg" 
  (contains? @msg-store2 [:adduser ["#others-list" hdl2]]) => true)

(srv-cmd client2 [:eval-clj [expr :you]])
(fact "Received eval result" 
  (contains? @msg-store2 [:result (pr-str [:you result])]) => true)

(srv-cmd client1 [:subscribe hdl2])
(fact "client1 received eval history from hdl2"
  (contains? @msg-store1 [:hist (pr-str [expr result])]) => true)

(srv-cmd client1 [:login hdl1])
(fact "client2 received update after client1 login"
  (contains-all? @msg-store2 [[:adduser ["#home-peer-list" "bob"]]
                              [:adduser ["#others-list" "bob"]]
                              [:rmanonsub nil]]) => true)
(srv-cmd client2 [:transfer hdl1])
;@msg-store1
;@msg-store2

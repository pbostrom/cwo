(ns cwo.test.server
  (:require [midje.sweet :refer [fact background]]
            [cwo.server :as server]
            [lamina.core :as lamina]
            [noir.session :as session :refer [*noir-session*]]
            [cwo.chmgr :as chmgr]))

(def mock-sesh-id "999")
(defn setup-socket [])

(defn verify-subscribe []
  (let [mock-ws (lamina/channel)]
    
    ))

(defn create-two-clients []
  )

(background (around :facts (with-redefs [*noir-session* (atom {"sesh-id" mock-sesh-id})] ?form)))
(fact "socket handler" (fn? (server/get-handler)) => true)
(fact "session id" (session/get "sesh-id") => mock-sesh-id) 
(fact "subscribe" (verify-subscribe) => true)

(let [handler (server/get-handler)
      client1 {:id "777" :ch (lamina/channel)}
      client2 {:id "888" :ch (lamina/channel)}]
  (with-redefs [*noir-session* (atom {"sesh-id" (:id client1)})]
    (handler client1 nil)
    (fact "client1 session id" (session/get "sesh-id") => (:id client1)))
  (with-redefs [*noir-session* (atom {"sesh-id" (:id client2)})]
    (handler client2 nil)
    (fact "client2 session id" (session/get "sesh-id") => (:id client2))))


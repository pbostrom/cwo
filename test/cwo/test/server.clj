(ns cwo.test.server
  (:require [midje.sweet :refer [fact]]
            [cwo.server :as server]
            [lamina.core :as lamina :refer [receive filter* channel?]]
            [lamina.viz :as lv]
            [noir.session :as session :refer [*noir-session*]]
            [cwo.mongo :as mg]
            [cwo.models.user :as user]
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

(defn get-msg [ch filt]
  (receive (filter* #(chmgr/cmd? % filt) ch) (fn [msg] msg)))

(mg/connect!)
(mg/reset-db!)

(def handler (server/get-handler)) 
(def client1 (init-client "777" handler)) 
(def client2 (init-client "888" handler)) 
(def hdl1 "bob")
(def hdl2 "joe")
(def expr "(range 10)")
(def result (pr-str (range 10)))

(fact "client1 websocket uplink exists" 
  (channel? (:srv client1)) => true)

(srv-cmd client2 [:login hdl2])
(get-msg (:cl client2) :dump)
(let [[_ dump-msg] (read-string (get-msg (:cl client2) :dump))]
  (fact "client2 handle is registered" 
        (read-string dump-msg) => hdl2)) 

; subscribe to hdl2's REPL
(srv-cmd client1 [:subscribe hdl2])


; send an expr to :you repl for evaluation
(srv-cmd client2 [:eval-clj [expr :you]])

(let [[_ result-msg] (read-string (get-msg (:cl client2) :result))] 
  (fact "Eval result from :you REPL is received on channel"
    (read-string result-msg) => [:you result]))

(let [hist-msg (get-msg (:cl client1) :hist)] 
  (fact "Verify expr/result history pair from subscribed REPL"
    (read-string (second (read-string hist-msg))) => [expr result]))

(srv-cmd client1 [:login hdl1])

(user/get-)

;(let [hist-msg (get-msg (:cl client2) :)])

(ns cwo.server
  (:require [compojure.core :refer [GET] :as cmpj]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.session :as session]
            [ring.middleware.reload :as reload]
            [aleph.http :as aleph]
            [overtone.at-at :as at-at]
            [cwo.chmgr :as chmgr]
            [cwo.twitter :as twitter]
            [cwo.routes :as rts]))

(defn proto-handlers [app-state]
  "Returns map with http and websocket handlers closed over app-state atom."
  {:ws (fn [webch handshake]
         (let [sesh-id (get-in handshake [:cookies "ring-session" :value])]
           (chmgr/init-socket app-state sesh-id webch)))
   :http (fn [request]
           (rts/app-routes (assoc request :app-state app-state)))})

; Combine routes for Websocket, compojure, and static resources
(defn all-handlers [handlers]
  (cmpj/routes 
   (GET "/socket" [] (cookies/wrap-cookies (aleph/wrap-aleph-handler (:ws handlers))))
   (session/wrap-session (:http handlers) {:cookie-attrs {:max-age 2600000}})
   (route/resources "/")
   (route/not-found "Not Found")))

(defn system []
  (let [app-state (atom {:handles (ref {})
                         :twitter-repls {}})]
    {:handlers (-> app-state proto-handlers all-handlers)
     :app-state app-state
     :thread-pool (at-at/mk-pool)
     :twitter-repl #(twitter/twitter-repl app-state)}))

(defn cycle-task [f tp period]
  (at-at/every period f tp :fixed-delay true))

(defn start-aleph [handlers port]
  (aleph/start-http-server
      (aleph/wrap-ring-handler handlers) {:port port :websocket true}))

(defn stop-system [sys]
  ((:stop-aleph sys))
  (at-at/stop-and-reset-pool! (:thread-pool sys)))

; Add aleph handler and start server
(defn -main []
  (let [port 8080
        {:keys [twitter-repl thread-pool] :as sys} (system)
        stop-aleph (start-aleph (:handlers sys) port)]
    (println "server started on port" port)
    (cycle-task twitter-repl thread-pool 65000)
    (println "Twitter REPL started")
    (assoc sys :stop-aleph stop-aleph)))

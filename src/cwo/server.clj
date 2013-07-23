(ns cwo.server
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.session :as session]
            [ring.middleware.reload :as reload]
            [aleph.http :as aleph]
            [cwo.chmgr :as chmgr]
            [cwo.routes :as routes]))

(def debug-state (atom nil))

(defn gen-handlers []
  "Returns a websocket handler with a session store atom."
  (let [app-state (atom {:handles (ref {})})] 
    (reset! debug-state app-state)
    {:ws (fn [webch handshake]
           (let [sesh-id (get-in handshake [:cookies "ring-session" :value])]
             (chmgr/init-socket app-state sesh-id webch)))
     :http (fn [request]
             (routes/app-routes (assoc request :app-state app-state)))}))

(def handlers (gen-handlers))

; Combine routes for Websocket, compojure, and static resources
(defroutes master-handler
  (GET "/socket" [] (cookies/wrap-cookies (aleph/wrap-aleph-handler (:ws handlers))))
  (session/wrap-session (:http handlers) {:cookie-attrs {:max-age 2600000}})
  (route/resources "/")
  (route/not-found "Not Found"))

; Add aleph handler and start server
(defn -main []
  (let [port 8080]
    (aleph/start-http-server 
      (aleph/wrap-ring-handler master-handler) {:port port :websocket true})
    (println "server started on port" port)))

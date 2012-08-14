(ns cwo.server
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.middleware.file :as ring-file]
            [ring.middleware.reload :as reload]
            [aleph.http :as aleph]
            [noir.server :as noir]
            [noir.session :as session]
            [cwo.chmgr :as chmgr]
            [cwo.mongo :as mg]
            [cwo.wastemgt :as wastemgt])
  (:gen-class))

; Need user.dir for Java policy file
(noir/add-middleware ring-file/wrap-file (System/getProperty "user.dir"))

; Load noir views and generate handler
(noir/load-views-ns 'cwo.views.noir)
(def noir-handler (noir/gen-handler {:mode :dev :ns 'cwo}))

(defn get-handler []
  "Returns a websocket handler with a session store atom."
  (let [session-store (atom {:handles {}})]
    ;TODO: consider a "store" protocol... user-store (mongo), session-store (in-memory ref/atom)
    (fn [webch handshake]
      (chmgr/init-socket (session/get "sesh-id") session-store webch))))

; wrap socket handler twice to conform to ring and include noir session info
(def wrapped-socket-handler (session/wrap-noir-session 
                              (aleph/wrap-aleph-handler (get-handler))))

; Combine routes for Websocket, noir, and static resources
(defroutes master-handler
  (GET "/socket" [] wrapped-socket-handler)
  noir-handler
  (route/resources "/"))

; Add aleph handler and start server
(defn -main []
  (let [port 8080]
    (aleph/start-http-server 
      (aleph/wrap-ring-handler master-handler) {:port port :websocket true})
    (println "server started on port" port)
    (mg/connect!)
    ;TODO: remove for production
    (mg/reset-db!)
    (wastemgt/start)))

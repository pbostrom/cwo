(ns cwo.server
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.reload :as reload]
            [aleph.http :as aleph]
            [cwo.chmgr :as chmgr]
            [cwo.views.noir :as views]
            [cwo.wastemgt :as wastemgt])
  (:gen-class))

; Need user.dir for Java policy file
;(noir/add-middleware ring-file/wrap-file (System/getProperty "user.dir"))

; Load noir views and generate handler

(def debug-state (atom nil))

(defn debug-reset [] (reset! @debug-state {:handles (ref {})}))

(defn gen-handlers []
  "Returns a websocket handler with a session store atom."
  (let [app-state (atom {:handles (ref {})})] 
    (reset! debug-state app-state)
    {:ws (fn [webch handshake]
           (println "WS handshake:" handshake)
           (chmgr/init-socket "1234" app-state webch))
     :http (fn [request]
             (views/app-routes (assoc request :app-state app-state)))})) ;FIXME: grab session id from handshake

(def handlers (gen-handlers))

; Combine routes for Websocket, noir, and static resources
(defroutes master-handler
  (GET "/socket" [] (aleph/wrap-aleph-handler (:ws handlers)))
  (cookies/wrap-cookies (:http handlers))
  (route/resources "/")
  (route/not-found "Not Found"))

; Add aleph handler and start server
(defn -main []
  (let [port 8080]
    (aleph/start-http-server 
      (aleph/wrap-ring-handler master-handler) {:port port :websocket true})
    (println "server started on port" port)))

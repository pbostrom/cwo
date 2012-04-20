(ns cwo.server
  (:use [compojure.core :only (defroutes GET)])
  (:require [compojure.route :as route]
            [ring.middleware.file :as ring-file]
            [aleph.core]
            [aleph.http :as aleph]
            [lamina.core :as lamina]
            [noir.server :as server])
  (:gen-class))

(def broadcast-channel (lamina/permanent-channel))

(defn socket-handler [ch handshake]
  (println (str "msg: " ch))
  (lamina/siphon ch broadcast-channel)
  (lamina/siphon broadcast-channel ch))

; Need user.dir for Java policy file
(server/add-middleware ring-file/wrap-file (System/getProperty "user.dir"))

; Load noir views and get handler
(server/load-views "src/cwo/views")
(def noir-handler (server/gen-handler {:mode :dev :ns 'cwo}))

; Define routes for Websocket, noir, and static resources routes
(defroutes handler
  (GET "/socket" [] (aleph/wrap-aleph-handler socket-handler))
  noir-handler
  (route/resources "/"))

; Add aleph handler and start server
(defn -main []
  (let [port 8080]
    (aleph/start-http-server 
      (aleph/wrap-ring-handler handler) {:port port :websocket true})
    (println "server started on port" port)))

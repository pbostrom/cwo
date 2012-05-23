(ns cwo.server
  (:use [compojure.core :only (defroutes GET)])
  (:require [compojure.route :as route]
            [ring.middleware.file :as ring-file]
            [ring.middleware.reload :as reload]
            [aleph.core]
            [aleph.http :as aleph]
            [lamina.core :as lamina]
            [noir.server :as noir]
            [noir.session :as session]
            [cwo.user :as user])
  (:gen-class))

(def broadcast-channel (lamina/permanent-channel))

(def user-channels (atom {}))

(defn socket-handler [ch handshake]
  (let [req-handle (:handle (:params handshake))
        user (user/get-user)]
    (println user "requests socket for handle" req-handle)
    (if (= user req-handle) ;TODO check for nil user and req-handle here
      (let [user-ch (lamina/permanent-channel)]
        (println user "creates new socket for writing")
        (swap! user-channels assoc user user-ch)
        (lamina/siphon ch user-ch)
        (lamina/on-closed ch #(swap! user-channels dissoc user)))
      (lamina/siphon (@user-channels req-handle) ch))));TODO check for nil channel here

(defn debug-socket-handler [ch handshake]
  (println "DEBUG2:" (:handle (:params handshake)) (user/get-user))
  (lamina/siphon ch broadcast-channel)
  (lamina/siphon broadcast-channel ch)
  (lamina/on-closed ch #(println "be closed")))

; Need user.dir for Java policy file
(noir/add-middleware ring-file/wrap-file (System/getProperty "user.dir"))

; Load noir views and generate handler
(noir/load-views-ns 'cwo.views.noir)
(def noir-handler (noir/gen-handler {:mode :dev :ns 'cwo}))

; wrap socket handler twice to conform to ring and include noir session info
(def wrapped-socket-handler (session/wrap-noir-session (aleph/wrap-aleph-handler socket-handler)))

; Combine routes for Websocket, noir, and static resources
(defroutes master-handler
  (GET "/socket/:handle" [handle] wrapped-socket-handler)
  noir-handler
  (route/resources "/"))

; Add aleph handler and start server
(defn -main []
  (let [port 8080]
    (aleph/start-http-server 
      (aleph/wrap-ring-handler master-handler) {:port port :websocket true})
    (println "server started on port" port)))

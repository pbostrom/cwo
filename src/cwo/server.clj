(ns cwo.server
  (:use [compojure.core :only (defroutes GET)])
  (:require [compojure.route :as route]
            [ring.middleware.file :as ring-file]
            [ring.middleware.reload :as reload]
            [aleph.core]
            [aleph.http :as aleph]
            [lamina.core :as lamina]
            [noir.server :as noir]
            [cwo.user :as user])
  (:gen-class))

(def broadcast-channel nil)

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
        (lamina/on-closed ch #(println "be closed dissoc" user)))
      (lamina/siphon (@user-channels req-handle) ch)) ;TODO check for nil channel here
    ))

(defn debug-socket-handler [ch handshake]
  (lamina/receive ch
    (fn [name]
      (println "DEBUG:" name)
      (lamina/siphon (lamina/map* #(str name ": " %) ch) broadcast-channel)
      (lamina/siphon broadcast-channel ch))))

; Need user.dir for Java policy file
(noir/add-middleware ring-file/wrap-file (System/getProperty "user.dir"))

; Load noir views and get handler
(noir/load-views-ns 'cwo.views.noir)
(def noir-handler (noir/gen-handler {:mode :dev :ns 'cwo}))

; Define routes for Websocket, noir, and static resources routes
(defroutes handler
  (GET "/socket/:handle" [handle] (aleph/wrap-aleph-handler socket-handler))
;  (GET "/socket/pbostrom" [] (aleph/wrap-aleph-handler debug-socket-handler)
;       (println "websocket for handle"))
  noir-handler
  (route/resources "/"))

; Add aleph handler and start server
(defn -main []
  (let [port 8080]
    (aleph/start-http-server 
      (aleph/wrap-ring-handler handler) {:port port :websocket true})
    (println "server started on port" port)))

(ns cwo.server
  (:use [compojure.core :only (defroutes GET)])
  (:require [compojure.route :as route]
            [ring.middleware.file :as ring-file]
            [ring.middleware.reload :as reload]
            [aleph.http :as aleph]
            [lamina.core :as lamina]
            [noir.server :as noir]
            [noir.session :as session]
            [cwo.user :as user]
            [cwo.chmgr :as chmgr])
  (:gen-class))

(defn socket-handler [webch handshake]
  (println handshake)
  (when-let [req-handle (:handle (:params handshake))]
    (let [user (user/get-user)
          reqch (chmgr/register req-handle)]
      (println user "requests socket for handle" req-handle)
      (if (= user req-handle)
        (do
          (println user "activates socket for writing")
          (lamina/siphon webch reqch)
          (lamina/on-closed webch (fn []
                                 (println user "deactivates socket")
                                 (lamina/enqueue reqch ":close"))))
        (do
          (println user "subscribes to" req-handle "socket")
          (lamina/siphon reqch webch))))))

(defn new-socket-handler [webch handshake]
  (let [ch (chmgr/register)]
    (lamina/siphon webch ch)
    (lamina/siphon ch webch)
    (lamina/enqueue ch (pr-str (keys @chmgr/handles)))))

; Need user.dir for Java policy file
(noir/add-middleware ring-file/wrap-file (System/getProperty "user.dir"))

; Load noir views and generate handler
(noir/load-views-ns 'cwo.views.noir)
(def noir-handler (noir/gen-handler {:mode :dev :ns 'cwo}))

; wrap socket handler twice to conform to ring and include noir session info
(def wrapped-socket-handler (session/wrap-noir-session (aleph/wrap-aleph-handler new-socket-handler)))

; Combine routes for Websocket, noir, and static resources
(defroutes master-handler
;  (GET "/socket/:handle" [handle] wrapped-socket-handler)
  (GET "/socket" [] wrapped-socket-handler)
  noir-handler
  (route/resources "/"))

; Add aleph handler and start server
(defn -main []
  (let [port 8080]
    (aleph/start-http-server 
      (aleph/wrap-ring-handler master-handler) {:port port :websocket true})
    (println "server started on port" port)))

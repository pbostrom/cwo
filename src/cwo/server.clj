(ns cwo.server
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.file :as ring-file]
            [ring.middleware.reload :as reload]
            [aleph.http :as aleph]
            [cwo.chmgr :as chmgr]
            [cwo.views.noir :as views]
            [cwo.wastemgt :as wastemgt])
  (:gen-class))

; Need user.dir for Java policy file
;(noir/add-middleware ring-file/wrap-file (System/getProperty "user.dir"))

; Load noir views and generate handler

(def debug-store (atom nil))

(defn debug-reset [] (reset! @debug-store {:handles (ref {})}))

(defn gen-ws-handler []
  "Returns a websocket handler with a session store atom."
  (let [session-store (atom {:handles (ref {})})] 
    (reset! debug-store session-store)
    ;TODO: consider a "store" protocol... user-store (mongo), session-store (in-memory ref/atom)
    (fn [webch handshake]
      (println "WS handshake:" handshake)
      (chmgr/init-socket "1234" session-store webch)))) ;FIXME: grab session id from handshake

(fn [sesh-id]
  (let [session-store nil] 
    (when-let [cc (session-store sesh-id)]
      (:handle @cc))))

; wrap socket handler twice to conform to ring and include noir session info
(def wrapped-socket-handler (aleph/wrap-aleph-handler (gen-ws-handler)))

(def some-state {:a 1 :b 2})

(defn wrap-state [handler appstate]
  (fn [request]
    (handler (assoc request :appstate appstate))))

; Combine routes for Websocket, noir, and static resources
(defroutes master-handler
  (GET "/socket" [] wrapped-socket-handler)
  (wrap-state views/root some-state)
  (route/resources "/")
  (route/not-found "Not Found"))

; Add aleph handler and start server
(defn -main []
  (let [port 8080]
    (aleph/start-http-server 
      (aleph/wrap-ring-handler master-handler) {:port port :websocket true})
    (println "server started on port" port)))

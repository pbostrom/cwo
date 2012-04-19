(ns cwo.server
  (:use compojure.core
        aleph.core
        aleph.http
        lamina.core
        [ring.middleware reload]
        [ring.middleware.file :only [wrap-file]])
  (:require [compojure.route :as route]
            [noir.server :as server])
;            [cwo.views :as views])
  (:gen-class))

(def broadcast-channel (permanent-channel))

(defn chat-handler [ch handshake]
  (println (str "msg: " ch))
  (siphon ch broadcast-channel)
  (siphon broadcast-channel ch))
;  (receive ch
;           (fn [msg]
;             (println (str "handler called on channel: " msg))
;             (siphon (map* #(str %) ch) broadcast-channel)
;             (enqueue broadcast-channel msg)
;             (siphon ch @broadcast-channel)
;             (siphon @broadcast-channel ch))))

; Load noir views
(server/add-middleware wrap-file (System/getProperty "user.dir"))
(server/load-views "src/cwo/views")
(def noir-handler (server/gen-handler {:mode :dev :ns 'cwo}))

; Define route table
(defroutes handler
;  (GET "/" [] (views/layout views/main-view))
  (GET "/socket" [] (wrap-aleph-handler chat-handler))
  noir-handler
;  (POST "/eval-clj" [expr :as {session :session}] (rpc/eval-clj expr session))
  (route/resources "/"))
  ;(route/not-found (views/layout [:p "aww... this doesn't exist"])))

;(def noir-handler (server/gen-handler {:mode :dev :ns 'cwo}))

; Add ring middlewarez
;(def app
;  (-> (routes noir-handler handler)
;    (wrap-params)
;    (wrap-reload '(cwo.server cwo.views))
;    (wrap-session)))

; Add aleph handler and start server
(defn -main []
  (start-http-server 
    (wrap-ring-handler handler) {:port 8080 :websocket true})
  (println "server started"))

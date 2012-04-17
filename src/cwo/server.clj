(ns cwo.server
  (:use compojure.core
        aleph.core
        aleph.http
        lamina.core
        [ring.middleware reload params session])
  (:require [compojure.route :as route]
            [cwo.views :as views]
            [cwo.rpc :as rpc])
  (:gen-class))

(def broadcast-channel (permanent-channel))

(defn chat-handler [ch handshake]
  (receive ch
           (fn [name]
             (println (str "handler called on channel: " ch))
             (siphon (map* #(str name ": " %) ch) broadcast-channel)
             (siphon broadcast-channel ch))))
;             (siphon ch broadcast-channel))))

; Define route table
(defroutes handler
  (GET "/" [] (views/layout views/main-view))
  (GET "/socket" [] (wrap-aleph-handler chat-handler))
  (POST "/eval-clj" [expr :as {session :session}] (rpc/eval-clj expr session))
  (route/resources "/")
  (route/not-found (views/layout [:p "aww... this doesn't exist"])))

; Add ring middlewarez
(def app
  (-> handler
    (wrap-params)
    (wrap-session)
    (wrap-reload '(cwo.server cwo.views cwo.rpc))))

; Add aleph handler and start server
(defn -main []
  (start-http-server 
    (wrap-ring-handler app) {:port 8080 :websocket true})
  (println "server started"))

(ns cwo.server
  (:use compojure.core, aleph.core, aleph.http, lamina.core, ring.middleware.reload)
  (:require [cwo.views :as views]
            [tryclj.views :as tcviews])
  (:gen-class))

(def broadcast-channel (permanent-channel))

(defn chat-handler [ch handshake]
  (receive ch
           (fn [name]
             (println (str "handler called on channel: " ch))
             (siphon (map* #(str name ": " %) ch) broadcast-channel)
             (siphon broadcast-channel ch))))
;             (siphon ch broadcast-channel))))




(defroutes my-app 
  (GET "/" [] (views/layout views/main-view))
  (GET "/socket" [] (wrap-aleph-handler chat-handler))
  (POST "/eval.clj" [expr] (tcviews/eval-view))
  (route/resources "/")
  (route/not-found (views/layout [:p "aww... this doesn't exist"])))

(defn -main []
  (start-http-server 
    (wrap-ring-handler 
      (wrap-reload my-app '(cwo.server cwo.views))) {:port 8080 :websocket true})
  (println "server started"))

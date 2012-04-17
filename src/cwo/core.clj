(ns cwo.core
  (use lamina.core
       aleph.http
       compojure.core))

(defn stream-something [ch]
  (future
    (dotimes [i 100]
      (enqueue ch (str i "\n")))
    (close ch)))

(defn aleph-handler [request]
  (println "In handler")
  (let [stream (channel)]
    (stream-something stream)
    {:status 200
     :headers {"content-type" "text/plain"}
     :body stream}))

(defn some-ring-handler [request]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "from ring, plain."})

(defroutes app-routes
  (GET "/" []  (do (println "some") some-ring-handler))
  (GET "/events" [] (do (println "aleph") (wrap-aleph-handler aleph-handler))))


(defn -main
  []
  (println "Listening via aleph's http server at port 8090")
  (start-http-server (wrap-ring-handler app-routes) {:port 8090}))

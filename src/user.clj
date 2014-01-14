(ns user
  (:require [cemerick.austin.repls]
            [cwo.server :as server :refer [init-brepl]]))

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
    (constantly (server/system))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system server/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
    (fn [s] (when s (server/stop s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn start-brepl []
  (cemerick.austin.repls/cljs-repl (:brepl system)))

(defn reset-brepl []
  (alter-var-root #'system assoc :brepl (init-brepl)))

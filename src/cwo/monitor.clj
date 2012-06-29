(ns cwo.monitor
  (:import (java.util.concurrent Executors TimeUnit)))

(defn monitor-loop []
 ; clean up inactive connections
  ; update clients with last activity info
  )

(defn start []
  (let [executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleAtFixedRate executor monitor-loop 0 30 TimeUnit/SECONDS))) 

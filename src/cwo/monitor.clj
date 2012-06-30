(ns cwo.monitor
  (:require [cwo.chmgr :as chmgr])
  (:import (java.util.concurrent Executors TimeUnit)))

(defn monitor []
  ; clean up inactive connections
  ; update clients with last activity info
  (doseq [[k v] @chmgr/sesh-id->cc]
    (let [{:keys [you]} v
          last-act (ms-since @(:ts you))
          sec (mod (.toSeconds TimeUnit/MILLISECONDS last-act) 60)
          mins (mod (.toMinutes TimeUnit/MILLISECONDS last-act) 60)
          hr (mod (.toHours TimeUnit/MILLISECONDS last-act) 60)]
      (println (format "h %d m %02d s %02d" hr mins sec)))))

(defn start []
  (let [executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleAtFixedRate executor monitor 0 30 TimeUnit/SECONDS))) 

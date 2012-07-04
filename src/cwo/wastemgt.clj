(ns cwo.wastemgt
  (:require [cwo.chmgr :as chmgr])
  (:import (java.util.concurrent Executors TimeUnit)))

(defn monitor 
  "Clean up inactive connections"
  []
  (doseq [[k v] @chmgr/sesh-id->cc]
    (let [{:keys [you]} v
          last-act (chmgr/ms-since @(:ts you))
          sec (mod (.toSeconds TimeUnit/MILLISECONDS last-act) 60)
          mins (mod (.toMinutes TimeUnit/MILLISECONDS last-act) 60)
          hr (mod (.toHours TimeUnit/MILLISECONDS last-act) 60)]
      (when (> hr 8)
        (println "Removing session data for" k)
        (chmgr/recycle! k)))));remove old session data   

(defn start []
  (let [executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleAtFixedRate executor monitor 0 30 TimeUnit/MINUTES))) 

(ns cwo.scratch
  (:require [cwo.chmgr :as chm])
  (:use [lamina.core]
        [lamina.viz]))

(def flag (atom true))

(defn init []
  (def p1 (channel* :grounded? true :permanent? true))
  (def p2 (channel* :grounded? true :permanent? true))
  (def t1 (channel))
  (def t2 (channel))
  (def t3 (take-while* (fn [msg] @flag) p1))
  (siphon t3 p2)
  (receive-all p2 println))

(defn get-cc [handle]
  (@chm/sesh-id->cc (@chm/handle->sesh-id handle)))

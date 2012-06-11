(ns cwo.scratch
  (:require [cwo.chmgr :as chm])
  (:use [lamina.core]
        [lamina.viz]))

(def flag (atom true))

(defn init []
  (def p1 (channel* :permanent? true))
  (def p2 (channel* :grounded? true :permanent? true))
  (def t1 (fork p1))
  (def t2 (channel))
  (siphon t1 t2))

(defn get-cc [handle]
  (@chm/sesh-id->cc (@chm/handle->sesh-id handle)))

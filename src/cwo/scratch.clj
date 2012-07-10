(ns cwo.scratch
  (:require [cwo.chmgr :as chm])
  (:use [lamina.core]
        [lamina.viz]))

(def flag (atom true))

(def b (channel))
(def myat (atom {"a" {:b b :c 2} :x {:y 3 :z 4}}))

(defn testat []
  (let [{sal :b} (@myat "a")]
    (close sal)
    (swap! myat (fn [m] (reduce #(apply assoc-in %1 %2) m
                           {[:a :b] nil,
                            [:a :c] nil,
                            [:x :y] nil})))))

(defn init []
  (def p1 (channel* :permanent? true))
  (def p2 (channel* :grounded? true :permanent? true))
  (def t1 (fork p1))
  (def t2 (channel))
  (siphon t1 t2))

(defn route? [dst]
  (when (not (contains? #{:prompt} dst))
    (println "Unsupported route filter!"))
  (let [dst-pre (first (name dst))]
    (fn [msg]
      (println msg ":" dst-pre)
      (.startsWith msg (str "{:" dst-pre)))))

(defn get-cc [handle]
  (@chm/sesh-id->cc (@chm/handle->sesh-id handle)))


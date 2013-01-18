(ns cwo.scratch
  (:require [cwo.chmgr :as chm]
            [cwo.views.enlive :as el]
            [net.cgrand.enlive-html :as html])
  (:use [lamina.core]
        [lamina.viz]))

(def flag (atom true))

(defn somf [x] 
  (if flag x :y))

(def b (channel))
(def perm (channel* :grounded? true :permanent? true))
(siphon perm b)

(close b)
;(view-graph perm)

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

(def ar (ref 1))
(let [da-ar ar] 
  (dosync 
    (alter ar inc)
    da-ar))
ar
(defn fmap [m f]
  (into {} (for [[k v] m] [k (f v)])))

(ns cwo.sandbox
  (:require [clojail.testers :refer [secure-tester-without-def blanket]]
            [clojail.core :refer [sandbox]]))

(def cwo-clojure-tester
  (blanket secure-tester-without-def
        "cwo" "noir" "aleph" "lamina" "ring" "clojail" "compojure"))

(defn make-sandbox []
  (sandbox cwo-clojure-tester
           :timeout 2000
           :init '(do (require '[clojure.repl :refer [doc]]))))


(defn chmgr
  []
  (atom {}))


(def x (ref 0))
(def y (ref 0))
(def a (agent nil))

(defn alter-and-send-side-effects
  "Alters refs then sends println actions to agent with new values"
  []
 (dosync 
   (let [newx (alter x inc)]) 
   (send a (fn [_] (println "x is" newx)))))

; Multiple threads will call be calling
(alter-and-send-side-effects)

(def z (ref [:a :c]))

(dosync (alter z into [:b :c]))

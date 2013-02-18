(ns cwo.sandbox
  (:require [clojail.testers :refer [secure-tester-without-def blanket]]
            [clojail.core :refer [sandbox]]
            [clojure.core.logic]))

(def cwo-clojure-tester
  (conj secure-tester-without-def
        (blanket "cwo" "noir" "aleph" "lamina" "ring" "clojail" "compojure")))

(defn make-sandbox []
  (sandbox cwo-clojure-tester
           :timeout 2000
           :init '(do (require '[clojure.repl :refer [doc]]))))

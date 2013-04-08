(ns cwo.sandbox
  (:require [clojail.testers :refer [secure-tester-without-def blanket
                                     blacklist-symbols blacklist-packages]]
            [clojail.core :refer [sandbox]]
            [clojure.tools.macro]
            [clojure.core.logic]
            [clojure.core.logic.fd]))

(def cwo-clojure-tester
  (conj secure-tester-without-def
        (blanket "cwo" "noir" "aleph" "lamina" "ring" "clojail" "compojure")
        (blacklist-packages ["java.net"])
        (blacklist-symbols '#{read-string read})))

(defn make-sandbox []
  (sandbox cwo-clojure-tester
           :timeout 2000
           :init '(do (require '[clojure.repl :refer [doc]]))))

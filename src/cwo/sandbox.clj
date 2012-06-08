(ns cwo.sandbox
  (:use [clojail.testers :only [secure-tester-without-def]]
        [clojail.core :only [sandbox]]))

(def try-clojure-tester
  (into secure-tester-without-def
        #{'tryclojure.core}))

(defn make-sandbox []
  (sandbox try-clojure-tester
           :timeout 2000
           :init '(do (use '[clojure.repl :only [doc]])
                      (future (Thread/sleep 600000)
                              (-> *ns* .getName remove-ns)))))

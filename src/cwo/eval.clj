(ns cwo.eval
  (:use [clojail.testers :only [secure-tester-without-def]]
        [clojail.core :only [sandbox]]
        [clojure.stacktrace :only [root-cause]])
  (:require [noir.session :as session])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(defn eval-form [form sbox]
  (with-open [out (StringWriter.)]
    (let [result (sbox form {#'*out* out})]
      (println "out" out)
      (println "result" result)
      {:expr form
       :result [out result]})))

(defn eval-string [expr sbox]
  (let [form (binding [*read-eval* false] (read-string expr))]
    (eval-form form sbox)))

(def try-clojure-tester
  (into secure-tester-without-def
        #{'tryclojure.core}))

(defn make-sandbox []
  (sandbox try-clojure-tester
           :timeout 2000
           :init '(do (use '[clojure.repl :only [doc]])
                      (future (Thread/sleep 600000)
                              (-> *ns* .getName remove-ns)))))

(defn init-sb! []
  (println "new sb")
  (let [sb (make-sandbox)]
    (session/put! "sb" sb)
    sb))

(defn eval-request [expr]
  (try
    (eval-string expr (or (session/get "sb") (init-sb!)))
    (catch TimeoutException _
      {:error true :message "Execution Timed Out!"})
    (catch Exception e
      {:error true :message (str (root-cause e))})))

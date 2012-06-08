(ns cwo.eval
  (:use [clojail.testers :only [secure-tester-without-def]]
        [clojail.core :only [sandbox]]
        [clojure.stacktrace :only [root-cause]])
  (:require [noir.session :as session])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(def sesh-id->sb (atom {}))

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

(defn init-sb! [sbk]
  (println "new sb")
  (let [sb (make-sandbox)]
    (swap! sesh-id->sb assoc-in [
    (session/put! "sb" sb)
    sb))

(defn get-sb [sb]
  (let [sesh-id (session/get "sesh-id")]
    (get-in @sesh-id->sb [sesh-id sb])))

(defn eval-request [expr sb]
  (try
    (eval-string expr (get-sb sb))
    (catch TimeoutException _
      {:error true :message "Execution Timed Out!"})
    (catch Exception e
      {:error true :message (str (root-cause e))})))

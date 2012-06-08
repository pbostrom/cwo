(ns cwo.eval
  (:use [clojure.stacktrace :only [root-cause]])
  (:require [noir.session :as session]
            [cwo.chmgr :as chmgr])
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

(defn get-sb [sb]
  (println sb)
  (let [sesh-id (session/get "sesh-id")]
    (get-in @chmgr/sesh-id->cc [sesh-id sb])))

(defn eval-request [expr sb]
  (try
    (eval-string expr (get-sb sb))
    (catch TimeoutException _
      {:error true :message "Execution Timed Out!"})
    (catch Exception e
      {:error true :message (str (root-cause e))})))

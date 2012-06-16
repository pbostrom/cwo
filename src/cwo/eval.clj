(ns cwo.eval
  (:use [clojure.stacktrace :only [root-cause]])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(defn eval-expr [expr sb]
  (try
    (with-open [out (StringWriter.)]
      (let [result (sb expr {#'*out* out})]
        (println "out" out)
        (println "result" result)
        {:expr expr
         :result [out result]}))
    (catch TimeoutException _
      {:error true :message "Execution Timed Out!"})
    (catch Exception e
      {:error true :message (str (root-cause e))})))

;(defn eval-py [expr sb]
;  (with-open [out (StringWriter.)]
;  (def interp (org.python.util.InteractiveInterpreter.))
;  (.runsource interp expr)
;    (let )))


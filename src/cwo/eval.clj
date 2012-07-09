(ns cwo.eval
  (:require [clojure.stacktrace :refer [root-cause]])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(defn eval-expr [expr sb]
  (try
    (with-open [out (StringWriter.)]
      (let [expr (binding [*read-eval* false] (read-string expr))
            result (sb expr {#'*out* out})]
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


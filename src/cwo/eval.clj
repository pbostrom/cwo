(ns cwo.eval
  (:require [clojure.stacktrace :refer [root-cause]])
  (:require [cwo.utils :refer [safe-read-str]])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(defn eval-expr
  "Read an expression, then evaluate in the specified repl, i.e. :you|oth"
  [expr sb]
  (try
    (with-open [out (StringWriter.)]
      (let [expr (safe-read-str expr)
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


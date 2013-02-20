(ns cwo.eval
  (:require [clojure.stacktrace :refer [root-cause]])
  (:require [cwo.utils :refer [safe-read-str]])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(defn eval-expr
  "Evaluate expression in the specified repl, i.e. :you|:oth"
  [expr sb]
  (try
    (with-open [out (StringWriter.)]
      (let [result (sb expr {#'*out* out})]
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

(def fee :bee)
(def boo :baa)
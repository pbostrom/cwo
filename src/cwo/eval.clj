(ns cwo.eval
  (:require [clojure.stacktrace :refer [root-cause]])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(defn- eval-expr-hlpr
  "Evaluate expression in the specified sandbox, catching eval exceptions"
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

(defn eval-expr
  "Evaluate expression in the specified sandbox"
  [expr sb]
  (let [{:keys [result error message] :as res} (eval-expr-hlpr expr sb)]
    (if error
      res
      (let [[out res] result]
        (str out (pr-str res))))))

;(defn eval-py [expr sb]
;  (with-open [out (StringWriter.)]
;  (def interp (org.python.util.InteractiveInterpreter.))
;  (.runsource interp expr)
;    (let )))

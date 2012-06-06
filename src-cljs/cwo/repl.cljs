(ns cwo.repl
  (:require [cwo.ajax :as ajax])
  (:use [cwo.utils :only (jq)]))

(def others-repl (-> (jq "#others-repl") (.jqconsole "Another's REPL\n" "=> " " ")))

(def your-repl (-> (jq "#your-repl") (.jqconsole "Your Clojure REPL\n" "=> " " ")))

(defn paren-match? [expr]
  (>=
    (count (filter #(= % ")") expr))
    (count (filter #(= % "(") expr))))

(defn expr-indent [expr]
  (let [lines (js->clj (.split expr "\n"))
        line (.trim jq (last lines))
        offset (if (= (count lines) 1) 2 0)
        indent-vec (reduce 
                     (fn [v x]
                       (let [idx (first v)
                             stack (second v)]
                         (cond 
                           (= x "(") [(inc idx) (cons idx stack)]
                           (= x ")") [(inc idx) (rest stack)]
                           true [(inc idx) stack]))) 
                     [0 []] (seq line))
        indent-val (+ (first (second indent-vec)) 2 offset)]
    indent-val))

(defn console-write [repl output]
  (if (:error output)
    (.Write repl (str (:message output) "\n") "jqconsole-error")
    (.Write repl (str output "\n") "jqconsole-output")))

(defn init-repl [repl]
  (let [handler (fn hdlr [expr]
                  (if expr
                    (console-write repl (ajax/eval-clojure expr)))
                  (.Prompt repl true hdlr (fn [expr]
                                            (if (paren-match? expr)
                                              false
                                              (expr-indent expr)))))]
    (.SetIndentWidth repl 1)
    (handler nil)))

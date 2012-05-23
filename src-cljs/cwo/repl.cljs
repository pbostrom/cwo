(ns cwo.repl
  (:require [cwo.ajax :as ajax])
  (:use [cwo.utils :only (jq)]))

(def your-repl (atom nil))
(def other-repl (atom nil))

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

(defn console-write [output]
  (if (:error output)
    (.Write @your-repl (str (:message output) "\n") "jqconsole-error")
    (.Write @your-repl (str output "\n") "jqconsole-output")))

(defn handler [expr]
  (if expr
    (console-write (ajax/eval-clojure expr)))
  (.Prompt @your-repl true handler (fn [expr]
                               (if (paren-match? expr)
                                 false
                                 (expr-indent expr)))))
(defn init []
  (reset! other-repl (-> (jq "#other-repl")
                       (.jqconsole "Another's REPL\n" "=> " " ")))
  (reset! your-repl (-> (jq "#your-repl")
                      (.jqconsole "Your Clojure REPL\n" "=> " " ")))
  (.SetIndentWidth @your-repl 1)
  (handler nil))

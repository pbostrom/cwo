(ns cwo.repl
  (:require [cwo.socket :as socket])
  (:use [cwo.utils :only (jq jslog)]))

(def publish-console? (atom true))

(def repls
  {:oth (-> (jq "#others-repl") (.jqconsole "Another's REPL\n" "=> " " "))
   :you (-> (jq "#your-repl") (.jqconsole "Your Clojure REPL\n" "=> " " "))})

(defn send-prompt []
  (let [repl (repls :you)]
    (when-let [prompt-text (and (= (.GetState repl) "prompt")(.GetPromptText repl))]
      (.send @socket/sock (pr-str {:p prompt-text})))))

(defn share-console-loop []
  (when @publish-console?
    (send-prompt)
    (js/setTimeout share-console-loop 1900)))

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

(defn prompt [repl]
  (let [handler #(socket/eval-clj % repl)]
    (.Prompt (repl repls) true handler (fn [expr]
                                 (if (paren-match? expr)
                                   false
                                   (expr-indent expr))))))

(defn console-write [repl output]
  (if (:error output)
    (.Write (repl repls) (str (:message output) "\n") "jqconsole-error")
    (.Write (repl repls) (str output "\n") "jqconsole-output"))
  (prompt repl))

(defn init-repl [repl]
  (.Enable (repls repl))
  (.SetIndentWidth (repls repl) 1)
  (prompt repl))

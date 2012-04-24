(ns cwo.app
  (:use [cwo.utils :only (socket jq make-js-map clj->js)])
  (:require [cwo.ajax :as ajax]
            [crate.core :as crate]))

(defn send-console []
  (let [console-nodes (-> (jq "#console .jqconsole-header ~ span") (.clone))
        console-html (-> (jq "<div>") (.append console-nodes) (.remove) (.html))]
    (.send socket (str console-html))))

(defn share-console-loop []
  (send-console)
  (js/setTimeout share-console-loop 1900))

(defn socket-ready []
  (-> (jq "#userbox")
    (.append
      (crate/html [:p.event "Socket Status: " + 
                   (str (.-readyState socket)) + " (open) " [:div#in]])))
  (share-console-loop))

(defn init-repl [config]
  (-> (jq "#console")
    (.console config)))

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
    (.Write jqconsole (str (:message output) "\n") "jqconsole-error")
    (.Write jqconsole (str output "\n"))))


(defn handler [expr]
  (if expr
    (console-write (ajax/eval-clojure expr)))
  (.Prompt jqconsole true handler (fn [expr]
                                    (if (paren-match? expr)
                                      false
                                      (expr-indent expr)))))


(defn init-repl []
  (def jqconsole 
    (-> (jq "#console")
      (.jqconsole "Clojure\n" "=> " " ")))
  (.SetIndentWidth jqconsole 1)
  (handler nil)
  (set! (.-onopen socket) socket-ready))

(if (= js/window.location.pathname "/")
  (init-repl))

(set! (.-onpopstate js/window) (fn [evt] (js/alert window.document.cookie)))

; login mgmt
(-> (jq "#login")
  (.bind "click" (fn [] (ajax/login (.val (jq "#login-input"))))))

(-> (jq "#logout")
  (.bind "click" (fn [] (ajax/logout))))

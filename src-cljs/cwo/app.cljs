(ns cwo.app
  (:use [cwo.utils :only (socket jq make-js-map clj->js jslog)])
  (:require [cwo.ajax :as ajax]
            [cwo.connect :as connect]
            [crate.core :as crate]))

(defn send-console []
  (let [console-nodes (-> (jq "#your-console .jqconsole-header ~ span") (.clone))
        console-html (-> (jq "<div>") (.append console-nodes) (.remove) (.html))]
    (.send socket (str console-html))))

(defn share-console-loop []
  (send-console)
  (js/setTimeout share-console-loop 1900))

(defn socket-ready []
  (-> (jq "#debug-box")
    (.append
      (crate/html [:p.event "Socket Status: " + 
                   (str (.-readyState socket)) + " (open) " [:div#in]])))
  (share-console-loop))

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
    (.Write jqconsole (str output "\n") "jqconsole-output")))

(defn handler [expr]
  (if expr
    (console-write (ajax/eval-clojure expr)))
  (.Prompt jqconsole true handler (fn [expr]
                                    (if (paren-match? expr)
                                      false
                                      (expr-indent expr)))))

; init repl
(def jqconsole 
  (-> (jq "#your-console")
    (.jqconsole "Your Clojure REPL\n" "=> " " ")))
(.SetIndentWidth jqconsole 1)
(handler nil)
(set! (.-onopen socket) socket-ready)

; navigation
(defn nav-handler []
  (let [hsh js/window.location.hash]
    (cond
      (= hsh "#others") (do 
                          (-> (jq "#your-container") (.hide))
                          (-> (jq "#other-container")(.show))
                          (ajax/share-list))
      (empty? hsh) (do 
                     (-> (jq "#your-container")(.show))
                     (-> (jq "#other-container")(.hide))))))

(set! (.-onpopstate js/window) nav-handler)

; hacky way to prevent muli-selects
(-> (jq "#share-list")
  (.on "click" (fn [evt] 
                 (-> (jq "#share-list option:selected") (.removeAttr "selected"))
                 (-> (jq evt.target) (.attr "selected" "selected")))))

; connect button
(-> (jq "#connect")
  (.bind "click" (fn [] (connect/connect (-> (jq "#share-list option:selected") (.val))))))

; login/out buttons 
(-> (jq "#login")
  (.bind "click" (fn [] (ajax/login (.val (jq "#login-input"))))))

(-> (jq "#logout")
  (.bind "click" (fn [] (ajax/logout))))

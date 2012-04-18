(ns cwo.app
  (:use [cwo.utils :only (make-js-map clj->js)]
        [cwo.ajax :only (eval-clojure)])
  (:require [crate.core :as crate]
            [domina :as dm]
            [domina.css :as dmc]
            [goog.dom :as gdom]
            [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [clojure.browser.event :as event]
            [cwo.ajax :as ajax]))


(def jq js/jQuery)
(def ws-url "ws://localhost:8080/socket")
(def socket (js/WebSocket. ws-url))

(defn add-msg [msg-el]
  (gdom/append (dm/single-node (dmc/sel "#chatLog")) msg-el))

(defn send-it []
  (let [text (.-value (goog.dom/getElement "text"))]
    (.send socket text)
    (set! (.-innerHTML (dom/get-element :out)) text)))

(defn console-loop []
  (send-it)
  (js/setTimeout console-loop 2000))

(defn socket-ready []
  (add-msg 
    (crate/html 
      [:p.event "Socket Status: " + 
       (str (.-readyState socket)) + " (open) " [:div#in]]))
  (console-loop))

(defn enter-cb [e]
  (if (= (.-keyCode e) 13)
    (send-it)))

(defn init-repl [config]
  (-> (jq "#console")
    (.console config)))

(defn cljValidate []
  false)

(defn cljHandle [line report]
  make-js-map (array {:msg "\n"
               :className "jquery-console-message"}))

(def clj-repl 
  (make-js-map {:welcomeMessage "Clojure REPL"
                :promptLabel "user=> "
                :commandValidate cljValidate
                :commandHandle cljHandle
                :autofocus true
                :animateScroll true
                :promptHistory true}))


(def jqconsole
  (-> (jq "#console")
    (.jqconsole "hi\n" "=> " " ")))

(def jqconsole-ro
  (-> (jq "#console2")
    (.jqconsole "Read-only\n" "=> " " ")))

(defn startPrompt []
  (.Prompt jqconsole true (fn [input]
                            (.Write jqconsole (str (ajax/eval-clojure input) "\n", "jqconsole-output"))
                            (startPrompt))))

;(init-repl clj-repl)
;(startPrompt)

(defn paren-match? [sexp]
  (>=
    (count (filter #(= % ")") sexp))
    (count (filter #(= % "(") sexp))))

(.SetIndentWidth jqconsole 1)

(defn sexp-indent [sexp]
  (let [lines (js->clj (.split sexp "\n"))
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

(defn handler [sexp]
  (if sexp
    (.Write jqconsole (str "==>" (eval-clojure sexp) "\n")))
  (.Prompt jqconsole true handler (fn [sexp]
                                    (if (paren-match? sexp)
                                      false
                                      (sexp-indent sexp)))))

(handler nil)
(set! (.-onmessage socket)
      (fn add-msg [msg]
         (set! (.-innerHTML (dom/get-element :in)) (.-data msg))))
(add-msg (crate/html [:p.event "Outgoing: " [:div#out]]))

(set! (.-onopen socket) socket-ready)

(event/listen (dm/single-node (dmc/sel "#text"))
              :keypress
              (fn [e]
                (if (= (.-keyCode e) 13)
                  (send-it))))

; Get console text as raw html
; (def cons-text (-> (jq "#console .jqconsole-header ~ span")
; (.clone)))
; (-> (jq "<div>") (.append cons-text) (.remove) (.html))
; When raw html is returned from websocket, insert into console2
; (-> (jq cons-text) (.insertAfter (jq "#console2 .jqconsole-header"))))

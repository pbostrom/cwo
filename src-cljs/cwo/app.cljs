(ns cwo.app
  (:use [cwo.utils :only (jq ws-url jslog)])
  (:require [cwo.ajax :as ajax]
            [cwo.socket :as socket]
            [cwo.repl :as repl]
            [crate.core :as crate]))

; init repl
(repl/init-repl :you)

; open websocket
(reset! socket/sock (js/WebSocket. ws-url))

(defn refresh-repl [msg]
  (-> (jq "#others-repl .jqconsole-header ~ span")
    (.remove))
  (-> (jq msg)
    (.insertAfter (jq "#others-repl .jqconsole-header"))))

(defn refresh-alt-repl [msg-obj]
  (let [{msg :alt} msg-obj]
  (-> (jq "#your-repl .jqconsole-header ~ span")
    (.remove))
  (-> (jq msg)
    (.insertAfter (jq "#your-repl .jqconsole-header")))))

(defn default? [msg]
  (not (or (= (.charAt msg 0) "[") (= (.charAt msg 0) "{"))))

(defn call-wscmd [[cmd args]]
  (.log js/console (name cmd) ":" (pr-str args))
  ((.-value (js/Object.getOwnPropertyDescriptor cwo.wscmd (name cmd))) args))

(defn msg-hdlr [msg]
  (let [msg msg.data]
    (if (default? msg)
      (refresh-repl msg)
      (let [msg-obj (cljs.reader/read-string msg)]
        (cond (vector? msg-obj) (call-wscmd msg-obj)
              (map? msg-obj) (refresh-alt-repl msg-obj))))))

(set! (.-onmessage @socket/sock) msg-hdlr)

; ui listeners

; prevent muli-selects
(-> (jq "#others-box")
  (.on "click" "#others-list" (fn [evt] 
                               (-> (jq "#others-list option:selected") (.removeAttr "selected"))
                               (-> (jq evt.target) (.attr "selected" "selected")))))

; connect button
(-> (jq "#others-box")
  (.on "click" "#connect" (fn [] (socket/subscribe (-> (jq "#others-list option:selected") (.val))))))

; transfer button
(-> (jq "#sub-box")
  (.on "click" "#transfer" (fn [] 
                             (socket/transfer (-> (jq "#sub-list option:selected") (.val))))))

; login/out buttons 
(-> (jq "#user-container")
  (.on "click" "#login" (fn [] (ajax/login (.val (jq "#login-input"))))))

(-> (jq "#user-container")
  (.on "click" "#logout" (fn [] (ajax/logout))))

; tab listener
(-> (jq "#myTab")
  (.on "click" "a" (fn [e]
                     (.preventDefault e)
                     (this-as ta (-> (jq ta)
                                   (.tab "show"))))))

; activate 1st tab
(.ready (jq js/document) #(-> (jq "#myTab a:first") (.tab "show")))

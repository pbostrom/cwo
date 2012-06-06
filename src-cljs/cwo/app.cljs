(ns cwo.app
  (:use [cwo.utils :only (new-socket jq ws-url jslog)])
  (:require [cwo.ajax :as ajax]
            [cwo.socket :as socket]
            [cwo.repl :as repl]
            [crate.core :as crate]))

; init repl
(repl/init-repl repl/your-repl)

; open websocket
(reset! socket/sock (js/WebSocket. ws-url))

(defn refresh-repl [msg]
;  (jslog msg)
  (-> (jq "#others-repl .jqconsole-header ~ span")
    (.remove))
  (-> (jq msg)
    (.insertAfter (jq "#others-repl .jqconsole-header"))))

(defn msg-hdlr [msg]
  (let [msg msg.data]
    (if (= (.charAt msg 0) "[")
      (let [[cmd args] (cljs.reader/read-string msg)]
        (.log js/console (name cmd) ":" (pr-str args))
        ((.-value (js/Object.getOwnPropertyDescriptor cwo.wscmd (name cmd))) args))
      (refresh-repl msg))))

(set! (.-onmessage @socket/sock) msg-hdlr)

; ui listeners

; prevent muli-selects
(-> (jq "#others-box")
  (.on "click" "#others-list" (fn [evt] 
                               (-> (jq "#others-list option:selected") (.removeAttr "selected"))
                               (-> (jq evt.target) (.attr "selected" "selected")))))

; connect button
(-> (jq "#others-box")
  (.on "click" "#connect" (fn [] (socket/connect (-> (jq "#others-list option:selected") (.val))))))

; transfer button
(-> (jq "#your-box")
  (.on "click" "#transfer" (fn [] 
                             (socket/transfer (-> (jq "#peer-list option:selected") (.val)))
                             (repl/init-repl repl/others-repl))))

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

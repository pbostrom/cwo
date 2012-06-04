(ns cwo.app
  (:use [cwo.utils :only (new-socket jq ws-url jslog)])
  (:require [cwo.ajax :as ajax]
            [cwo.socket :as socket]
            [cwo.repl :as repl]
            [crate.core :as crate]))

; init repl
(repl/init)

; open websocket
(reset! socket/sock (js/WebSocket. ws-url))

(defn refresh-repl [msg]
;  (jslog msg)
  (-> (jq "#other-repl .jqconsole-header ~ span")
    (.remove))
  (-> (jq msg)
    (.insertAfter (jq "#other-repl .jqconsole-header"))))

(defn msg-hdlr [msg]
  (let [msg msg.data]
    (if (= (.charAt msg 0) "[")
      (let [[cmd args] (cljs.reader/read-string msg)]
        (.log js/console (name cmd) ":" (pr-str args))
        ((.-value (js/Object.getOwnPropertyDescriptor cwo.wscmd (name cmd))) args))
      (refresh-repl msg))))

(set! (.-onmessage @socket/sock) msg-hdlr)

; navigation
(defn nav-handler []
  (let [hsh js/window.location.hash]
    (cond
      (= hsh "#others") (do 
                          (-> (jq "#your-container") (.hide))
                          (-> (jq "#other-container")(.show)))
      (empty? hsh) (do 
                     (-> (jq "#your-container")(.show))
                     (-> (jq "#other-container")(.hide))))))

(set! (.-onpopstate js/window) nav-handler)

; button listeners
; use body to register for ajax deferred event listeners
(def body (jq "body"))

; prevent muli-selects
(-> body
  (.on "click" "#others-list" (fn [evt] 
                               (-> (jq "#others-list option:selected") (.removeAttr "selected"))
                               (-> (jq evt.target) (.attr "selected" "selected")))))

; connect button
(-> body
  (.on "click" "#connect" (fn [] (socket/connect (-> (jq "#others-list option:selected") (.val))))))

; transfer button
(-> body
  (.on "click" "#transfer" (fn [] (socket/transfer (-> (jq "#peer-list option:selected") (.val))))))

; login/out buttons 
(-> body
  (.on "click" "#login" (fn [] (ajax/login (.val (jq "#login-input"))))))

(-> body
  (.on "click" "#logout" (fn [] (ajax/logout))))

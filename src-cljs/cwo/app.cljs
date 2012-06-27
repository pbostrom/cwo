(ns cwo.app
  (:use [cwo.utils :only (jq ws-url jslog sock)])
  (:require [cwo.ajax :as ajax]
            [cwo.repl :as repl]
            [cwo.wscmd :as _]))


; open websocket
(reset! sock (js/WebSocket. ws-url))

(defn route [msg-obj]
  (let [{pmsg :p tmsg :t} msg-obj]
    (when pmsg
      (.SetPromptText (:oth repl/repls) pmsg))
    (when tmsg
      (.SetPromptText (:you repl/repls) tmsg))))

(defn call-wscmd [[cmd args]]
  (.log js/console (name cmd) ":" (pr-str args))
  ((.-value (js/Object.getOwnPropertyDescriptor cwo.wscmd (name cmd))) args))

(defn msg-hdlr [msg]
  (let [msg-obj (cljs.reader/read-string (.-data msg))]
    (cond (vector? msg-obj) (call-wscmd msg-obj)
          (map? msg-obj) (route msg-obj))))

(set! (.-onmessage @sock) msg-hdlr)

; init repls
(set! (.-onopen @sock) (fn []
                          (repl/set-repl-mode :you :active) 
                          (repl/set-repl-mode :oth :sub))) 

; ui listeners

; prevent muli-selects
(-> (jq "#others-tab")
  (.on "click" "#others-list" (fn [evt] 
                               (-> (jq "#others-list option:selected") (.removeAttr "selected"))
                               (-> (jq evt.target) (.attr "selected" "selected")))))

; connect button
(-> (jq "#others-tab") (.on "click" "#connect" repl/connect))
; disconnect button
(-> (jq "#others-tab") (.on "click" "#discon" repl/disconnect))

; transfer button
(-> (jq "#sub-box") (.on "click" "#transfer" repl/transfer))
; reclaim button
(-> (jq "#your-status") (.on "click" "#reclaim" repl/reclaim))

; login/out buttons 
(-> (jq "#user-container")
  (.on "click" "#login" (fn [] (ajax/login (.val (jq "#login-input")) 
                                           (.text (jq "#others-tab #connected #owner"))))))

(-> (jq "#user-container")
  (.on "click" "#logout" (fn [] (ajax/logout))))

; tab listener
(-> (jq "#myTab")
  (.on "click" "a" (fn [e]
                     (.preventDefault e)
                     (this-as ta (-> (jq ta)
                                   (.tab "show"))))))

; $(document).ready function
(defn ready []
  (-> (jq "#myTab a:first") (.tab "show"))) ; activate 1st tab

(.ready (jq js/document) ready)

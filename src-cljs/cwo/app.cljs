(ns cwo.app
  (:use [cwo.utils :only (jq ws-url jslog sock)])
  (:require [cwo.ajax :as ajax]
            [cwo.repl :as repl]))

; init repls
(repl/set-repl-mode :you :active)
(repl/set-repl-mode :oth :sub)

; open websocket
(reset! sock (js/WebSocket. ws-url))

(defn route [msg-obj]
  (let [{msg :p} msg-obj]
    (.SetPromptText (:oth repl/repls) msg)))

(defn call-wscmd [[cmd args]]
  (.log js/console (name cmd) ":" (pr-str args))
  ((.-value (js/Object.getOwnPropertyDescriptor cwo.wscmd (name cmd))) args))

(defn msg-hdlr [msg]
  (let [msg-obj (cljs.reader/read-string (.-data msg))]
    (cond (vector? msg-obj) (call-wscmd msg-obj)
          (map? msg-obj) (route msg-obj))))

(set! (.-onmessage @sock) msg-hdlr)

; ui listeners

; prevent muli-selects
(-> (jq "#others-box")
  (.on "click" "#others-list" (fn [evt] 
                               (-> (jq "#others-list option:selected") (.removeAttr "selected"))
                               (-> (jq evt.target) (.attr "selected" "selected")))))

; connect button
(-> (jq "#others-box") (.on "click" "#connect" repl/connect))
; disconnect button
(-> (jq "#others-repl") (.on "click" "#discon" repl/disconnect))

; transfer button
(-> (jq "#sub-box") (.on "click" "#transfer" repl/transfer))

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

; $(document).ready function
(defn ready []
  (-> (jq "#myTab a:first") (.tab "show"))) ; activate 1st tab

(.ready (jq js/document) ready)

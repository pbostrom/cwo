(ns cwo.app
  (:use [cwo.utils :only (jq ws-url jslog)])
  (:require [cwo.ajax :as ajax]
            [cwo.socket :as socket]
            [cwo.repl :as repl]
            [crate.core :as crate]))



; init repls
(repl/set-repl-mode :you :active)
(repl/set-repl-mode :oth :sub)

; open websocket
(reset! socket/sock (js/WebSocket. ws-url))

(defn route [msg-obj]
  (let [{msg :p} msg-obj]
    (.SetPromptText (:oth repl/repls) msg)))

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
              (map? msg-obj) (route msg-obj))))))

(set! (.-onmessage @socket/sock) msg-hdlr)

; ui listeners

; prevent muli-selects
(-> (jq "#others-box")
  (.on "click" "#others-list" (fn [evt] 
                               (-> (jq "#others-list option:selected") (.removeAttr "selected"))
                               (-> (jq evt.target) (.attr "selected" "selected")))))

; subscribe button
(-> (jq "#others-box")
  (.on "click" "#subscribe" (fn []
                              (repl/set-repl-mode :oth :sub)
                              (socket/subscribe (-> (jq "#others-list option:selected") (.val))))))

; transfer button
(-> (jq "#sub-box")
  (.on "click" "#transfer" (fn []
                             (reset! repl/publish-console? false)
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
(.ready (jq js/document) #(do 
                            (-> (jq "#myTab a:first") (.tab "show"))))

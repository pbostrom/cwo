(ns cwo.app
  (:require [cwo.utils :as utils :refer [jq ws-url jslog sock get-hash]]
            [cwo.ajax :as ajax]
            [cwo.repl :as repl]
            [cwo.wscmd :as wscmd]))

(defn route [msg-obj]
  (let [{pmsg :p tmsg :t} msg-obj]
    (when pmsg
      (.SetPromptText (:oth repl/repls) pmsg))
    (when tmsg
      (.SetPromptText (:you repl/repls) tmsg))))

(defn msg-hdlr [msg]
  (jslog msg)
  (let [msg-obj (cljs.reader/read-string (.-data msg))]
    (cond (vector? msg-obj) (apply wscmd/wscmd msg-obj)
          (map? msg-obj) (route msg-obj))))

; open websocket and set handlers
(defn open-websocket []
  (reset! sock (js/WebSocket. ws-url))
  (set! (.-onmessage @sock) msg-hdlr)
  (set! (.-onopen @sock) (fn []
                           (repl/set-repl-mode :you :active) 
                           (repl/set-repl-mode :oth :sub))))
; ui listeners

; button [en|dis]ablers
(-> (jq "#others-list")
  (.on "change" (fn [evt]
                 (if (= 1 (.-length (jq "#others-list > option:selected")))
                   (-> (jq "#join-btn") (.removeAttr "disabled"))
                   (-> (jq "#join-btn") (.attr "disabled" "disabled"))))))

(-> (jq "#home-peer-list")
  (.on "change" 
       (fn [evt]
         (let [num-sel (-> 
                         (jq "#home-peer-list > option:selected")
                         (.not "[class='anon']")
                         (.-length))] 
           (if (= 1  num-sel)
             (-> (jq "#transfer") (.removeAttr "disabled"))
             (-> (jq "#transfer") (.attr "disabled" "disabled")))))))

; button listners
(-> (jq "#join-btn") (.on "click" repl/join))
(-> (jq "#peer-status") (.on "click" "#discon" repl/disconnect))
(-> (jq "#transfer") (.on "click" repl/transfer))
(-> (jq "#reclaim") (.on "click" repl/reclaim))

; login/out buttons 
(-> (jq "#user-container")
  (.on "click" "#login" repl/login))
(-> (jq "#user-container")
  (.on "click" "#logout" (fn [] (repl/logout))))

(defn chat-hdlr [e]
  (when (= (.-which e) 13)
    (this-as ta
             (.send @sock (pr-str [:chat [(.-id ta) (.val (jq ta))]]))
             (.val (jq ta) ""))))

; chat input listeners
(-> (jq ".chatwin > input") (.on "keydown" chat-hdlr))

; repl tabs
(-> (jq "#repl-tabs a")
  (.on "click" (fn [e]
                 (.preventDefault e)
                 (this-as ta (.tab (jq ta) "show")))))

; set up status table based on active repl
(.on (jq "#repl-tabs a[href=\"#peer\"]") "show" 
     (fn [] 
       (.append (jq "#widgets") (jq "#home-panel"))
       (.after (jq "#peer-panel div.spacer") (jq "#others-box"))
       (.prepend (jq "#panel-box") (jq "#peer-panel"))))

(.on (jq "#repl-tabs a[href=\"#home\"]") "show" 
     (fn [] 
       (.append (jq "#widgets") (jq "#peer-panel"))
       (.after (jq "#home-panel div.spacer") (jq "#others-box"))
       (.prepend (jq "#panel-box") (jq "#home-panel"))))

; $(document).ready function
(defn ready []
  (let [token (.attr (jq "#token") "value")]
    (when (and token ((comp not empty?) token) 
               (ajax/gh-profile token))))

  (if (get-hash)
    (-> (jq "#repl-tabs a[href=\"#peer\"]") (.tab "show"))
    (-> (jq "#repl-tabs a:first") (.tab "show")))
  
  (open-websocket))

(.ready (jq js/document) ready)

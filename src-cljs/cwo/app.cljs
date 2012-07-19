(ns cwo.app
  (:require [cwo.utils :as utils :refer [jq ws-url jslog sock others-set get-hash srv-cmd]]
            [cwo.ajax :as ajax]
            [cwo.repl :as repl]
            [cwo.wscmd :as wscmd]))

; open websocket
(reset! sock (js/WebSocket. ws-url))

(defn route [msg-obj]
  (let [{pmsg :p tmsg :t} msg-obj]
    (when pmsg
      (.SetPromptText (:oth repl/repls) pmsg))
    (when tmsg
      (.SetPromptText (:you repl/repls) tmsg))))

(defn msg-hdlr [msg]
  (let [msg-obj (cljs.reader/read-string (.-data msg))]
    (cond (vector? msg-obj) (apply wscmd/wscmd msg-obj)
          (map? msg-obj) (route msg-obj))))

(set! (.-onmessage @sock) msg-hdlr)

; init repls
(set! (.-onopen @sock) (fn []
                          (repl/set-repl-mode :you :active) 
                          (repl/set-repl-mode :oth :sub))) 

; ui listeners

; prevent muli-selects
(-> (jq "#others-list")
  (.on "click" (fn [evt] 
                 (-> (jq "#others-list option:selected") (.removeAttr "selected"))
                 (-> (jq (.-target evt)) (.attr "selected" "selected"))
                 (-> (jq "#repl-tabs a[href=\"#peer\"]") (.tab "show")))))

; connect button
(-> (jq "#join-btn") (.on "click" repl/join))
; disconnect button
(-> (jq "#peers") (.on "click" "#discon" repl/disconnect))

; transfer button
(-> (jq "#sub-box") (.on "click" "#transfer" repl/transfer))
; reclaim button
(-> (jq "#your-status") (.on "click" "#reclaim" repl/reclaim))

; login/out buttons 
(-> (jq "#user-container")
  (.on "click" "#login" (fn [] (ajax/login (.val (jq "#login-input"))))))
(-> (jq "#user-container")
  (.on "click" "#logout" (fn [] (ajax/logout))))

(defn chat-hdlr [e]
  (when (= (.-which e) 13)
    (this-as ta
             (.send @sock (pr-str [:chat [(.-id ta) (.val (jq ta))]]))
             (.val (jq ta) ""))))

; chat input listeners
(-> (jq ".tab-pane > .row") (.on "keydown" ".chat > input" chat-hdlr))

; bootstrap UI widgets
; main accordion menu
(-> (jq ".accordion-body")
  (.on "show" (fn [] (this-as ta 
                              (let [icon (jq "i" (.parent (jq ta)))]
                                (.removeClass icon "icon-chevron-right")
                                (.addClass icon "icon-chevron-down")))))
  (.on "hide" (fn [] (this-as ta 
                              (let [icon (jq "i" (.parent (jq ta)))]
                                (.removeClass icon "icon-chevron-down")
                                (.addClass icon "icon-chevron-right"))))))

; repl tabs
(-> (jq "#repl-tabs a")
  (.on "click" (fn [e]
                 (.preventDefault e)
                 (this-as ta (.tab (jq ta) "show")))))

; broadcast radio buttons
; need to manage button state ourselves

; click listener to check for valid toggle states
(-> (jq "#bc-radio button")
  (.click (fn [] (this-as ta
                          (let [btn (jq ta) ]
                            (when-not (or (.hasClass btn "disabled") (.hasClass btn "active"))
                                  (let [parent (.parent btn)]
                                    (-> parent 
                                      (.find ".active")
                                      (.removeClass "active"))
                                    (.addClass btn "active")
                                    (.trigger parent "change"))))))))

(defn set-bc-menu-badge [value]
  ((value {:on (fn [] 
                 (.append (jq "#widgets") (jq "#bdg-off"))
                 (.append (.first (jq "button.broadcast")) (jq "#bdg-on"))) 
           :off (fn [] 
                  (.append (jq "button.broadcast") (jq "#bdg-off"))
                  (.append (jq "#widgets") (jq "#bdg-on")))})))

; listen on the radio groups 'change' event
(-> (jq "#bc-radio")
  (.change (fn [] (this-as ta
                           (let [active-btn (jq ".active" ta)
                                 value (keyword (.val active-btn))]
                             (srv-cmd :broadcast value)
                             (set-bc-menu-badge value))))))

; set up status table based on active repl
(.on (jq "#repl-tabs a[href=\"#peer\"]") "show" 
     (fn [] 
       (.append (jq "#widgets") (jq "#home-panel"))
       (.after (jq "#peer-panel div.span3.empty") (jq "#others-box"))
       (.prepend (jq "#panel-box") (jq "#peer-panel"))))

(.on (jq "#repl-tabs a[href=\"#home\"]") "show" 
     (fn [] 
       (.append (jq "#widgets") (jq "#peer-panel"))
       (.after (jq "#home-panel div.span3.empty") (jq "#others-box"))
       (.prepend (jq "#panel-box") (jq "#home-panel"))))

; $(document).ready function
(defn ready []
  (let [token (.attr (jq "#token") "value")]
    (when (and token ((comp not empty?) token) 
               (ajax/gh-profile token))))
  (when (= 0 (.size (jq "#user-container > #logoutbox")))
    (.removeAttr (jq "#bc-radio") "data-toggle")
    (.addClass (jq "#bc-radio > button") "disabled"))

  (if (get-hash)
    (-> (jq "#repl-tabs a[href=\"#peer\"]") (.tab "show"))
    (-> (jq "#repl-tabs a:first") (.tab "show"))))

(.ready (jq js/document) ready)

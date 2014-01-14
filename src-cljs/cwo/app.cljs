(ns cwo.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [put! chan <!]]
   [cwo.utils :as utils :refer [jq ws-url jslog sock get-hash srv-cmd]]
   
   [cwo.ajax :as ajax]
   [cwo.repl :as repl]
   [cwo.wscmd :as wscmd]))

(defn route [msg-obj editor]
  (.log js/console (pr-str msg-obj))
  (let [{pmsg :p tmsg :t} msg-obj]
    (when-let [[content [line ch]] pmsg]
      (.setValue editor content)
      (.setCursor editor line ch))
    (when tmsg
      (.SetPromptText (:you repl/repls) tmsg))))

(defn msg-hdlr [msg editor]
  (let [msg-obj (cljs.reader/read-string (.-data msg))]
    (cond (vector? msg-obj) (apply wscmd/wscmd msg-obj)
          (map? msg-obj) (route msg-obj editor))))

; open websocket and set handlers
(defn open-websocket [editor]
  (reset! sock (js/WebSocket. ws-url))
  (set! (.-onmessage @sock) #(msg-hdlr % editor))
  (set! (.-onopen @sock) (fn []
                           (jslog "socket open"))))
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
(defn init-buttons [you oth]
  (-> (jq "#join-btn") (.on "click" #(repl/join % oth)))
  (-> (jq "#peer-status") (.on "click" "#discon" repl/disconnect))
  (-> (jq "#transfer") (.on "click" repl/transfer))
  (-> (jq "#reclaim") (.on "click" repl/reclaim)))

;; paste handler
(defn paste-hdlr []
  (let [host (.val (jq "input[name=pastehost]:checked"))
        id (.val (jq "#paste-id"))]
    (srv-cmd :paste [host id :you]))
  (.modal (jq "#paste-modal") "hide"))

(-> (jq "#pastebtn") (.on "click" paste-hdlr))
(-> (jq "#paste-modal") (.on "hidden" #(.html (jq "#paste-err") "")))

; login/out buttons 
(-> (jq "#user-container")
  (.on "click" "#login" repl/login))
(-> (jq "#user-container")
  (.on "click" "#logout" (fn [] (repl/logout))))

(defn chat-hdlr [e]
  (when (= (.-which e) 13)
    (let [ta (.-target e)]
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

(defn listen [qry-str event]
  (let [out (chan)]
    (.on (jq qry-str) event
         (fn [e] (put! out e)))
    out))

(def cmr (atom nil))

; $(document).ready function
(defn ready []
  (-> (jq "#repl-tabs a:first") (.tab "show"))
  (set! (.-onhashchange js/window) (fn [x] (repl/process-hash (get-hash))))

  (let [cm-id "#you-editor"
        cm-opts (clj->js {:matchBrackets true :autoCloseBrackets true})
        editor (js/CodeMirror (aget (jq cm-id) 0) cm-opts)
        oth-editor (js/CodeMirror (aget (jq "#oth-editor") 0) cm-opts)
        c (listen cm-id "keydown")]
    (open-websocket oth-editor)
    (reset! cmr editor)
;    (.on editor "change" (fn [cm co] (.log js/console co)))
    (go
     (while true
       (let [e (<! c)
             content (.getValue editor)
             {:keys [line ch]} (.getCursor editor)]
         (repl/send-editor-state :you [content [line ch]])
         (when (and (.-ctrlKey e) (.-shiftKey e))
           (cond
            (= (.-keyCode e) 88) (repl/editor-eval editor e) 
            (= (.-keyCode e) 90) (repl/editor-load editor e))))))))

(.ready (jq js/document) ready)

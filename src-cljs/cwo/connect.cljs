(ns cwo.connect
  (:use [cwo.utils :only (jq jslog ws-url)]))

(defn connect [user]
  (def socket (js/WebSocket. ws-url))
  (def jqconsole-ro
    (-> (jq "#other-console")
      (.jqconsole (str user "'s REPL\n") "=> " " ")))
  (set! (.-onopen socket) #(-> (jq "#debug-box") (.append "Socket Ready")))
  (set! (.-onerror socket) #(-> (jq "#debug-box") (.append "Socket fubar")))
  (set! (.-onmessage socket)
        (fn [msg]
          (jslog (.-data msg))
          (-> (jq "#other-console .jqconsole-header ~ span")
            (.remove))
          (-> (jq (.-data msg))
            (.insertAfter (jq "#other-console .jqconsole-header"))))))

(if (= js/window.location.pathname "/shared")
  (begin-share))
;         (set! (.-innerHTML (dom/get-element :in)) (.-data msg))))
; Get console text as raw html
; (def cons-text (-> (jq "#console .jqconsole-header ~ span")
; (.clone)))
; (-> (jq "<div>") (.append cons-text) (.remove) (.html))
; When raw html is returned from websocket, insert into console2
; (-> (jq cons-text) (.insertAfter (jq "#console2 .jqconsole-header"))))
;

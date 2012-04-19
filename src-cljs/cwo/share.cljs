(ns cwo.share
  (:use [cwo.utils :only (jq jslog ws-url)]))

(defn begin-share []
  (def socket (js/WebSocket. ws-url))
  (def jqconsole-ro
    (-> (jq "#console2")
      (.jqconsole "Read-only\n" "=> " " ")))
  (set! (.-onmessage socket)
        (fn [msg]
          (jslog (.-data msg))
          (-> (jq "#console2 .jqconsole-header ~ span")
            (.remove))
          (-> (jq (.-data msg))
            (.insertAfter (jq "#console2 .jqconsole-header"))))))

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

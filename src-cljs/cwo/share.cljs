(ns cwo.share)

(def jq js/jQuery)
(def ws-url "ws://localhost:8080/socket")
(def socket (js/WebSocket. ws-url))

(defn send-console []
 (let [console-nodes (-> (jq "#console .jqconsole-header ~ span") (.clone))
       console-html (-> (jq "<div>") (.append cons-text) (.remove) (.html))]
  (.send socket console-html)))

(defn send-console-test []
 (let [console-node-count (.-length (jq "#console .jqconsole-header ~ span"))]
  (.send socket console-node-count)))

(defn share-console-loop []
  (send-console-test)
  (js/setTimeout share-console-loop 2000))

; Get console text as raw html
; (def cons-text (-> (jq "#console .jqconsole-header ~ span")
; (.clone)))
; (-> (jq "<div>") (.append cons-text) (.remove) (.html))
; When raw html is returned from websocket, insert into console2
; (-> (jq cons-text) (.insertAfter (jq "#console2 .jqconsole-header"))))
;

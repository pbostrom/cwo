(ns websox.core
  (:require [lamina.core :as lamina]))


(defprotocol Channel ;extend/wrap lamina channel?
  "documentation"
  (rpc [ch cmd args])
  (msg [ch dst msg-body])
  (rpc-listen [ch cmd function]))

(defprotocol WebSocket
  "documentation"
  (onopen [ws f])
  (onmsg [ws f])
  (onclose [ws f])
  (msg [ws])
  (open-channel [ws id])
  (register-channel [ws id]))

(extend-protocol WebSocket
  lamina.core.channel.Channel
  (onopen [ch f]
    
    )
  )

(defn websocket
  "Returns a websocket"
  [aleph-conn])
(defn channel [ws k])
(defn register-channel [ws k])
(defn onclose [ws f])
(defn onmessage [ws f])
(defn send [ws msg])

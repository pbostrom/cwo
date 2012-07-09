(ns cwo.channel
  (defprotocol Channel ;extend/wrap lamina channel?
    "documentation"
    (rpc ch [cmd args])
    (rpc-listen [ch cmd function]))

  (defprotocol WebSocket
    "documentation"
    (onopen [ws f])
    (onmsg [ws f])
    (onclose [ws f])
    (msg [ws])
    (open-channel [ws id])
    (register-channel [ws id])))

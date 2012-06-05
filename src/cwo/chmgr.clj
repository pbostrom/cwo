(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]))

; Channel mgmt architecture
; A channel controller is a map for regulating websocket traffic between clients
; {
;  :snd     Required, permanent channel to receive commands and shared REPLs
;  :rec     Required, permanent channel to send commands and REPL contents
;  :handle  Optional, anonymous users permitted
;
;valves are closable channels that route traffic between permanent channels
;  :rec-valve    Optional, a subscription to a shared REPL session
;  :trans-valve  Optional, a subscription to a REPL has been transferred,
;                also acts as a pass-thru for other subscribers
; }

; Every web session has an associated channel controller
(def sesh-id->cc (atom {}))

; map to lookup session id from handle
(def handle->sesh-id (atom {}))

; channel to update handle list
(def handle-ch (lamina/channel* :permanent? true))

; create a send/receive channel pair
(defn init-cc []
  {:snd (lamina/channel* :grounded? true :permanent? true)
   :rec (lamina/channel* :grounded? true :permanent? true)})

; Handle commands from the channel
(defn command-handler [sesh-id cmd-str]
  (let [[cmd arg] (read-string cmd-str)]
    (println "cmd:" cmd sesh-id arg)
    ((ns-resolve 'cwo.chmgr (symbol (name cmd))) sesh-id arg)))

;get the channel controller of the current session, initializing if needed
(defn get-cc []
  (let [sesh-id (session/get "sesh-id")]
    (if-let [cc (get-in @sesh-id->cc [sesh-id])]
      cc
      (let [newcc (init-cc)]
        (lamina/receive-all 
          (lamina/filter* #(.startsWith % "[") (newcc :snd)) #(command-handler sesh-id %))
        (lamina/siphon handle-ch (newcc :rec))
        (swap! sesh-id->cc assoc sesh-id newcc)
        newcc))))

; send a command to a websocket client
(defn client-cmd [ch cmdvec]
  (lamina/enqueue ch (pr-str cmdvec)))

(defn broadcast []
  (let [handle (session/get "handle")
        sesh-id (session/get "sesh-id")]
    (swap! handle->sesh-id assoc handle sesh-id)
    (swap! sesh-id->cc assoc-in [sesh-id :handle] handle)
    (client-cmd handle-ch [:addhandles [handle]])))

(defn end-broadcast []
  (let [handle (session/get "handle")]
    (swap! handle->sesh-id dissoc handle)
    (client-cmd handle-ch [:rmhandle handle])))

(defn socket-handler [webch handshake]
  (let [cc (get-cc)]
    (when (session/get "handle") (broadcast))
    (lamina/siphon webch (cc :snd))
    (lamina/siphon (cc :rec) webch)
    (client-cmd (cc :rec) [:addhandles (keys @handle->sesh-id)])))

; socket ctrl commands below
(defn connect [sesh-id handle]
  (let [peer-handle (get-in @sesh-id->cc [sesh-id :handle] "anonymous")
        target-cc (@sesh-id->cc (@handle->sesh-id handle))
        rec-valve (lamina/channel)]
    (println sesh-id "subscribe to" handle)
    (client-cmd (target-cc :rec) [:addpeer peer-handle])
    (when-let [old-ch (get-in @sesh-id->cc [sesh-id :rec-valve])]
      (lamina/close old-ch))
    (swap! sesh-id->cc assoc-in [sesh-id :rec-valve] rec-valve)
    (lamina/siphon (target-cc :snd) rec-valve (get-in @sesh-id->cc [sesh-id :rec]))))

(defn transfer [sesh-id handle]
  (let [new-cc (@sesh-id->cc (@handle->sesh-id handle))
        old-cc (@sesh-id->cc sesh-id)
        trans-valve (lamina/channel)]
    (lamina/close (new-cc :rec-valve))
    (client-cmd (new-cc :rec) [:transfer handle])
    (when-let [old-ch (get-in @sesh-id->cc [sesh-id :trans-valve])]
      (lamina/close old-ch))
    (swap! sesh-id->cc assoc-in [sesh-id :trans-valve] trans-valve)
    (lamina/siphon (new-cc :snd) trans-valve (old-cc :rec))
    (lamina/siphon trans-valve (old-cc :snd))))

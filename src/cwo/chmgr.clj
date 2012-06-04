(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]))

; Channel mgmt architecture
; Every web session has an associated channel controller
(def sesh-id->cc (atom {}))

; A channel controller is a map structure for regulating websocket traffic between clients
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

; map to lookup session id from handle
(def handle->sesh-id (atom {}))

; channel to update handle list
(def handle-ch (lamina/channel* :permanent? true))

; create a send/receive channel pair
(defn sr-pair []
  {:snd (lamina/channel* :grounded? true :permanent? true)
   :rec (lamina/channel* :grounded? true :permanent? true)})

; Handle commands from the channel
(defn command-handler [sesh-id cmd-str]
  (let [[cmd arg] (read-string cmd-str)]
    (println "cmd:" cmd sesh-id arg "ns:" *ns*)
    ((ns-resolve 'cwo.chmgr (symbol (name cmd))) sesh-id arg)))

;get the send-receive pair of the current session, creates if nec
(defn get-srp []
  (let [sesh-id (session/get "sesh-id")]
    (if-let [srp (get-in @sesh-id->cc [sesh-id :srp])]
      srp
      (let [newsrp (sr-pair)]
        (lamina/receive-all (lamina/filter* #(.startsWith % "[") (newsrp :snd)) #(command-handler sesh-id %))
        (lamina/siphon handle-ch (newsrp :rec))
        (swap! sesh-id->cc assoc-in [sesh-id :srp] newsrp)
        newsrp))))

(defn broadcast []
  (let [handle (session/get "handle")
        sesh-id (session/get "sesh-id")]
      (swap! handle->sesh-id assoc handle sesh-id)
      (swap! sesh-id->cc assoc-in [sesh-id :handle] handle)
      (lamina/enqueue handle-ch (pr-str [:addhandles [handle]]))))

(defn end-broadcast []
  (let [handle (session/get "handle")]
    (swap! handle->sesh-id dissoc handle)
    (lamina/enqueue handle-ch (pr-str [:rmhandle handle]))))

(defn socket-handler [webch handshake]
  (let [srp (get-srp)]
    (when (session/get "handle") (broadcast))
    (lamina/siphon webch (srp :snd))
    (lamina/siphon (srp :rec) webch)
    (lamina/enqueue (srp :rec) (pr-str [:addhandles (keys @handle->sesh-id)]))))

; socket ctrl commands below
(defn connect [sesh-id handle]
  (let [peer-handle (get-in @sesh-id->cc [sesh-id :handle] "anonymous")
        target-srp (get-in @sesh-id->cc [(@handle->sesh-id handle) :srp])
        rec-valve (lamina/channel)]
    (println sesh-id "subscribe to" handle)
    (lamina/enqueue (target-srp :rec) (pr-str [:addpeer peer-handle]))
    (when-let [old-ch (get-in @sesh-id->cc [sesh-id :rec-valve])]
      (lamina/close old-ch))
    (swap! sesh-id->cc assoc-in [sesh-id :rec-valve] rec-valve)
    (lamina/siphon (target-srp :snd) rec-valve (get-in @sesh-id->cc [sesh-id :srp :rec]))))

(defn transfer [sesh-id handle]
  (let [new-srp (@sesh-id->cc (@handle->sesh-id handle))
        old-srp (@sesh-id->cc sesh-id)
        trans-valve (lamina/channel)]
    (lamina/close (new-srp :rec-valve))
    (when-let [old-ch (get-in @sesh-id->cc [sesh-id :trans-valve])]
      (lamina/close old-ch))
    (swap! sesh-id->cc assoc-in [sesh-id :trans-valve] trans-valve)
    (lamina/siphon nil nil)))


(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]
            [cwo.sandbox :as sb]))

; Channel mgmt architecture
; A channel controller is a map for regulating websocket traffic between clients
; {
;  :snd     Required, permanent channel to send commands and REPL contents
;  :rec     Required, permanent channel to receive commands and shared REPLs
;  :handle  Optional, anonymous users permitted
;
;   valves are closable channels that route traffic between permanent channels
;  :rec-valve    Optional, a subscription to a shared REPL session
;  :trans-valve  Optional, a subscription to a REPL has been transferred,
;                also acts as a pass-thru for other subscribers
;
;  evaluation sandboxes
;  :you  Your primary code evaluation environment
;  :oth  Optional, if someone transfers their repl to you
; }

; Every web session has an associated channel controller
(def sesh-id->cc (atom {}))

; map to lookup session id from handle
(def handle->sesh-id (atom {}))

; channel to update handle list
(def handle-ch (lamina/channel* :permanent? true))

; Handle commands from the channel
(defn command-handler [sesh-id cmd-str]
  (let [[cmd arg] (read-string cmd-str)]
    (println "cmd:" cmd sesh-id arg)
    ((ns-resolve 'cwo.chmgr (symbol (name cmd))) sesh-id arg)))

; create a send/receive channel pair, swap map structure
(defn init-cc! [sesh-id]
  (println "init-cc!")
  (let [newcc {:snd (lamina/channel* :grounded? true :permanent? true)
               :rec (lamina/channel* :grounded? true :permanent? true)
               :you (sb/make-sandbox)}]
    (lamina/receive-all 
      (lamina/filter* #(.startsWith % "[") (newcc :snd)) #(command-handler sesh-id %))
    (lamina/siphon handle-ch (newcc :rec))
    (swap! sesh-id->cc assoc sesh-id newcc)
    newcc))

;get the channel controller of the current session, initializing if needed
(defn get-cc []
  (let [sesh-id (session/get "sesh-id")]
    (or (@sesh-id->cc sesh-id) (init-cc! sesh-id))))

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

; transfer control of sesh-id's REPL (oldcc) to newcc specified by handle
(defn transfer [sesh-id handle]
  (let [hdl-sesh-id (@handle->sesh-id handle)
        {newccr :rec newccs :snd newccrv :rec-valve} (@sesh-id->cc hdl-sesh-id)
        {oldccr :rec oldccs :snd oldcctv :trans-valve target-sb :you} (@sesh-id->cc sesh-id)
        trans-valve (lamina/channel)]
    (lamina/close newccrv) ; close subscription created by (connect ...)
    (client-cmd newccr [:transfer handle]) ; tell client that subscribed REPL is being transfered
    (when oldcctv (lamina/close oldcctv)) ;close previous trans-valve if it exists
;    (swap! sesh-id->cc assoc-in [sesh-id :trans-valve] trans-valve)
;    (swap! sesh-id->cc assoc-in [hdl-sesh-id :oth] oldsb)
    (swap! sesh-id->cc
           (fn [m] (reduce #(apply assoc-in %1 %2) m
                           {[sesh-id :trans-valve] trans-valve, [hdl-sesh-id :oth] target-sb})))
    (lamina/siphon newccs trans-valve oldccr)
    (lamina/siphon trans-valve oldccr)))

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
;  :sub-valve    Optional, a subscription to a shared REPL session
;  :tsub-valve   Optional, a subscription to your own REPL after a transfer
;  :pt-valve     Optional, a pass-thru for your subscribers after a transfer
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

; message predicates to aid in routing
(defn cmd? [msg]
  (.startsWith msg "["))
;  (and msg (.startsWith msg "[")))

(defn default? [msg]
  (not (or (cmd? msg) (.startsWith msg "{"))))

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
      (lamina/filter* cmd? (newcc :snd)) #(command-handler sesh-id %))
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
        sub-valve (lamina/channel)]
    (println sesh-id "subscribe to" handle)
    (client-cmd (target-cc :rec) [:addpeer peer-handle])
    (when-let [old-ch (get-in @sesh-id->cc [sesh-id :sub-valve])]
      (lamina/close old-ch))
    (swap! sesh-id->cc assoc-in [sesh-id :sub-valve] sub-valve)
    (lamina/siphon (lamina/filter* default? (target-cc :snd))
                   sub-valve (get-in @sesh-id->cc [sesh-id :rec]))))

(defn strip-alt [msg]
  (println msg)
  (let [msg-obj (read-string msg)]
    (:alt msg-obj)))

; transfer control of sesh-id's REPL (oldcc) to newcc specified by handle
(defn transfer [sesh-id handle]
  (let [hdl-sesh-id (@handle->sesh-id handle)
        {newccr :rec newccs :snd newccrv :sub-valve} (@sesh-id->cc hdl-sesh-id)
        {oldccr :rec oldccs :snd oldcctv :tsub-valve
         oldccpv :pt-valve target-sb :you} (@sesh-id->cc sesh-id)
        tsub-valve (lamina/channel)
        pt-valve (lamina/map* strip-alt newccs)]
    (lamina/close newccrv) ; close subscription created by (connect ...)
    
    (when oldcctv 
      (lamina/close oldcctv)
      (lamina/close oldccpv)) ;close previous trans-valve if it exists
;    (swap! sesh-id->cc assoc-in [sesh-id :trans-valve] trans-valve)
;    (swap! sesh-id->cc assoc-in [hdl-sesh-id :oth] oldsb)
    (swap! sesh-id->cc
           (fn [m] (reduce #(apply assoc-in %1 %2) m
                           {[sesh-id :tsub-valve] tsub-valve,
                            [sesh-id :pt-valve] pt-valve,
                            [hdl-sesh-id :oth] target-sb})))
    (lamina/siphon pt-valve oldccs)
    (lamina/siphon newccs tsub-valve oldccr)
    (client-cmd newccr [:transfer handle]))) ; tell client that subscribed REPL is being transfered

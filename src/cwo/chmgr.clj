(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]
            [cwo.sandbox :as sb]
            [cwo.eval :as evl]))

; Channel mgmt architecture
; A channel controller is a map for regulating websocket traffic between clients
; {
;  :srv-ch    Required, permanent channel to send commands to server
;  :cl-ch     Required, permanent channel to send commands to client
;  :handle    Optional, anonymous users permitted
;
;   valves are closable channels that route traffic between permanent channels
;  :sub-valve    Optional, a subscription to a shared REPL session
;  :tsub-valve   Optional, a subscription to your own REPL after a transfer
;  :pt-valve     Optional, a pass-thru for your subscribers after a transfer
;
;  evaluation sandboxes
;  :you Repl Your primary code evaluation environment
;  :oth Repl Optional, if someone transfers their repl to you
; }
;
(defrecord Repl [hist sb])

; Every web session has an associated channel controller
(def sesh-id->cc (atom {}))

; map to lookup session id from handle
(def handle->sesh-id (atom {}))

; channel to update handle list
(def handle-ch (lamina/channel* :permanent? true))

(defn cc-from-handle [handle]
  (@sesh-id->cc (@handle->sesh-id handle)))

; higher order function to filter routes
(defn route? [dst]
  (when (not (contains? #{:prompt} dst))
    (println "Unsupported route filter!"))
  (let [dst-pre (first (name dst))]
    #(.startsWith % (str "{:" dst-pre))))

(defn cmd? [msg]
  (.startsWith msg "["))

; Handle commands send via srv-ch
(defn cmd-hdlr [sesh-id cmd-str]
  (let [[cmd arg] (read-string cmd-str)]
    (println "cmd:" cmd sesh-id arg)
    ((ns-resolve 'cwo.chmgr (symbol (name cmd))) sesh-id arg)))

; create a send/receive channel pair, swap map structure
(defn init-cc! [sesh-id]
  (println "init-cc!")
  (let [newcc {:srv-ch (lamina/channel* :grounded? true :permanent? true)
               :cl-ch (lamina/channel* :grounded? true :permanent? true)
               :you (Repl. (lamina/permanent-channel) (sb/make-sandbox))}]
    (lamina/receive-all (lamina/filter* cmd? (newcc :srv-ch)) #(cmd-hdlr sesh-id %))
    (lamina/siphon handle-ch (newcc :cl-ch))
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
    (lamina/siphon webch (cc :srv-ch))
    (lamina/siphon (cc :cl-ch) webch)
    (client-cmd (cc :cl-ch) [:addhandles (keys @handle->sesh-id)])))

; socket ctrl commands below
(defn subscribe [sesh-id handle]
  (let [{{:keys [cl-ch srv-ch {:keys [hist]} you]} (@handle->sesh-id handle)} @sesh-id->cc ;publisher
        {{old-sv :sub-valve subclch :cl-ch pr-hdl :handle
          :or {pr-hdl "anonymous"}} sesh-id} @sesh-id->cc ;subscriber
        sub-valve (lamina/channel)]
    (println sesh-id "subscribe to" handle)
    (client-cmd cl-ch [:addsub pr-hdl])
    (when old-sv (lamina/close old-sv))
    (swap! sesh-id->cc assoc-in [sesh-id :sub-valve] sub-valve)
    (lamina/siphon (lamina/fork hist) sub-valve subclch)
    (lamina/siphon (lamina/filter* (route? :prompt) srv-ch) sub-valve subclch)))

; transfer control of sesh-id's REPL (oldcc) to newcc specified by handle
(defn transfer [sesh-id handle]
  (let [hdl-sesh-id (@handle->sesh-id handle)
        {newccr :rec newccs :snd newccsv :sub-valve} (@sesh-id->cc hdl-sesh-id)
        {oldccr :rec oldccs :snd oldcctv :tsub-valve
         oldccpv :pt-valve target-sb :you} (@sesh-id->cc sesh-id)
        tsub-valve (lamina/channel)
        pt-valve nil]
    (lamina/close newccsv) ; close subscription created by (connect ...)
    (when oldcctv 
      (lamina/close oldcctv)
      (lamina/close oldccpv)) ;close previous trans-valve if it exists
    (swap! sesh-id->cc
           (fn [m] (reduce #(apply assoc-in %1 %2) m
                           {[sesh-id :tsub-valve] tsub-valve,
                            [sesh-id :pt-valve] pt-valve,
                            [hdl-sesh-id :oth] target-sb})))
    (lamina/siphon pt-valve oldccs)
    (lamina/siphon newccs tsub-valve oldccr)
    (client-cmd newccr [:transfer handle]))) ; tell client that subscribed REPL is being transfered

(defn eval-clj [sesh-id [expr sb-key]]
  (let [{{:keys [cl-ch] repl sb-key} sesh-id} @sesh-id->cc
        sb (:sb repl)
        {:keys [expr result error message] :as res} (evl/eval-expr expr sb)
        data (if error
               res
               (let [[out res] result]
                 (str out (pr-str res))))]
    (client-cmd cl-ch [:result (pr-str [sb-key data])])
    (client-cmd (:hist repl) [:hist (pr-str [expr data])])))

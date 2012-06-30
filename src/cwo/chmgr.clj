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
;  :transfer  Optional, handle that your REPL has been transfered to
;
;   valves are closable channels that route traffic between permanent channels
;  :sub-vlv    Optional, a subscription to a shared REPL session
;  :pt-vlv     Optional, a pass-thru for your subscribers after a transfer
;  :tsub-vlv   Optional, a subscription to your transfered REPL session
;  
;
;  evaluation sandboxes
;  :you Repl Your primary code evaluation environment
;  :oth Repl Optional, if someone transfers their repl to you
; }

; {
;  :hist      Channel to send history commands to connected users
;  :sb        Evalutation sandbox
;  :ts        Timestamp of last activity
; }
(defrecord Repl [hist sb ts])

; Every web session has an associated channel controller
(def sesh-id->cc (atom {}))

; map to lookup session id from handle
(def handle->sesh-id (atom {}))

; channel to update handle list
(def handle-ch (lamina/channel* :permanent? true))

;;
;; helper functions
;;

(defn cc-from-handle [handle]
  (@sesh-id->cc (@handle->sesh-id handle)))

; higher order function to filter routes
(defn route-old? [dst]
  (when (not (contains? #{:p :t} dst))
    (println "Unsupported route filter!"))
  #(.startsWith % (str "{" dst)))

; higher order function to filter routes
; :l last-activity timestamp
; :p prompt text
; :t wrapper for transferred prompt msgs
(defn route? [rt-set]
  (when (not (clojure.set/subset? rt-set #{:l :p :t}))
    (println "Unsupported route filter!"))
  (fn [msg]
    (let [[msg-obj] (seq (safe-read-str msg))]
      (contains? rt-set (key msg-obj)))))

; avoid eval injections via websocket msgs
(defn safe-read-str [st]
  (binding [*read-eval* false] (read-string expr)))

(defn cmd? 
  ([msg] (.startsWith msg "["))
  ([msg cmd] (.startsWith msg (str "[" cmd " ")))) ;TODO: this might be better as a regex

; return the number of milliseconds since the specified time
(defn ms-since [t]
  (- (System/currentTimeMillis) t))

; Handle commands send via srv-ch
(defn cmd-hdlr [sesh-id cmd-str]
  (let [[cmd arg] (safe-read-str cmd-str)]
    (println "cmd:" cmd sesh-id arg)
    ((ns-resolve 'cwo.chmgr (symbol (name cmd))) sesh-id arg)))

; create a send/receive channel pair, swap map structure
(defn init-cc! [sesh-id]
  (println "init-cc!")
  (let [newcc {:srv-ch (lamina/channel* :grounded? true :permanent? true)
               :cl-ch (lamina/channel* :grounded? true :permanent? true)
               :you (Repl. (lamina/permanent-channel)
                           (sb/make-sandbox)
                           (atom (System/currentTimeMillis)))}]
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

; add handle of subscriber to publisher's list
(defn add-sub [pub-hdl sub-hdl]
  (let [{cl-ch :cl-ch} (cc-from-handle pub-hdl)] 
    (client-cmd cl-ch [:addsub sub-hdl])))

(defn broadcast []
  (let [handle (session/get "handle")
        sesh-id (session/get "sesh-id")]
    (swap! handle->sesh-id assoc handle sesh-id)
    (swap! sesh-id->cc assoc-in [sesh-id :handle] handle)
    (client-cmd handle-ch [:addhandles [handle]])))

(defn end-broadcast []
  (let [handle (session/get "handle")
        sesh-id (@handle->sesh-id handle)]
    (swap! handle->sesh-id dissoc handle)
    (swap! sesh-id->cc update-in [sesh-id] dissoc :handle)
    (client-cmd handle-ch [:rmhandle handle])))

(defn socket-handler [webch handshake]
  (let [cc (get-cc)]
    (when (session/get "handle") (broadcast))
    (lamina/siphon webch (cc :srv-ch))
    (lamina/siphon (cc :cl-ch) webch)
    (client-cmd (cc :cl-ch) [:addhandles (keys @handle->sesh-id)])))

;;
;; socket ctrl commands below
;;

(defn subscribe [sesh-id handle]
  (let [{{:keys [cl-ch srv-ch you]} (@handle->sesh-id handle)} @sesh-id->cc ;publisher
        {{old-sv :sub-vlv subclch :cl-ch pr-hdl :handle} sesh-id} @sesh-id->cc ;subscriber
        sub-vlv (lamina/channel)]
    (println sesh-id "subscribe to" handle)
    (when pr-hdl (client-cmd cl-ch [:addsub pr-hdl]))
    (when old-sv (lamina/close old-sv))
    (swap! sesh-id->cc assoc-in [sesh-id :sub-vlv] sub-vlv)
    (lamina/siphon (lamina/fork (:hist you)) sub-vlv subclch)
    (lamina/siphon (lamina/filter* (route? #{:l :p}) srv-ch) sub-vlv subclch)))

(defn end-transfer [sesh-id handle]
  (let [hdl-sesh-id (@handle->sesh-id handle)
        {pv :pt-vlv tv :tsub-vlv cl :cl-ch owner-handle :handle} (@sesh-id->cc sesh-id)]
    (lamina/close pv)
    (lamina/close tv)
    (swap! sesh-id->cc
           (fn [m]
             (reduce #(apply assoc-in %1 %2) m
                     {[sesh-id :tsub-vlv] nil,
                      [sesh-id :pt-vlv] nil,
                      [sesh-id :transfer] nil,
                      [hdl-sesh-id :oth] nil})));TODO: potential synchronization bug here
    (client-cmd (get-in @sesh-id->cc [hdl-sesh-id :cl-ch]) [:endtransfer :_])))

; transfer control of sesh-id's REPL to specified handle
(defn transfer [sesh-id handle]
  (let [hdl-sesh-id (@handle->sesh-id handle)
        {tr-cl :cl-ch tr-srv :srv-ch subv :sub-vlv} (@sesh-id->cc hdl-sesh-id) ;transferee
        {old-cl :cl-ch old-srv :srv-ch target-repl :you owner-hdl :handle 
         old-pv :pt-vlv old-tv :tsub-vlv trans :transfer} (@sesh-id->cc sesh-id) ;owner
        pv (lamina/channel)
        tv (lamina/channel)
        parse-tprompt (fn [msg]
                        (let [{msg :t} (safe-read-str msg)]
                          (pr-str msg)))
        route-hist (fn [msg]
                     (str "[:trepl " msg "]"))
        route-prompt (fn [msg]
                       (let [{prompt-txt :p} (safe-read-str msg)]
                         (pr-str {:t prompt-txt})))]
    (when trans
      (end-transfer sesh-id trans)
      (subscribe (@handle->sesh-id trans) owner-hdl))
    (client-cmd (:hist target-repl) [:chctrl handle])
    (lamina/close subv) ; close subscription created by (connect ...)
    (lamina/siphon 
      (lamina/map* parse-tprompt (lamina/filter* (route? #{:t}) tr-srv))
      pv old-srv)
    (lamina/siphon (lamina/map* route-hist (lamina/fork (:hist target-repl))) tv old-cl)
    (lamina/siphon (lamina/map* route-prompt (lamina/fork pv)) tv old-cl)
    (swap! sesh-id->cc
           (fn [m] (reduce #(apply assoc-in %1 %2) m
                           {[sesh-id :tsub-vlv] tv,
                            [sesh-id :pt-vlv] pv,
                            [sesh-id :transfer] handle,
                            [hdl-sesh-id :oth] target-repl})))
    (client-cmd tr-cl [:transfer handle])))

(defn disconnect [sesh-id handle]
  (println "disconnect" handle)
  (let [pub-sesh-id (@handle->sesh-id handle)
        {{:keys [cl-ch transfer]} pub-sesh-id} @sesh-id->cc ;publisher
        {{sv :sub-vlv sub-hdl :handle} sesh-id} @sesh-id->cc] ;subscriber
    (when (and sub-hdl (= sub-hdl transfer))
      (end-transfer pub-sesh-id sub-hdl)
      (client-cmd (get-in @sesh-id->cc [pub-sesh-id :cl-ch]) [:reclaim :_])
      (client-cmd (:hist (get-in @sesh-id->cc [pub-sesh-id :you])) [:chctrl handle]))
    (lamina/close sv)
    (swap! sesh-id->cc assoc-in [sesh-id :sub-vlv] nil)
    (when sub-hdl (client-cmd cl-ch [:rmsub sub-hdl]))))

; reclaim control of sesh-id's REPL from specified handle
(defn reclaim [sesh-id handle]
  (let [hdl-sesh-id (@handle->sesh-id handle)
        {owner-handle :handle cl :cl-ch repl :you} (@sesh-id->cc sesh-id)]
    (end-transfer sesh-id handle)
    (client-cmd cl [:reclaim :_]) 
    (client-cmd (:hist repl) [:chctrl owner-handle])
    (subscribe hdl-sesh-id owner-handle)))

(defn eval-clj [sesh-id [expr sb-key]]
  (let [expr (safe-read-str expr) 
        {{:keys [cl-ch] repl sb-key} sesh-id} @sesh-id->cc
        sb (:sb repl)
        {:keys [result error message] :as res} (evl/eval-expr expr sb)
        data (if error
               res
               (let [[out res] result]
                 (str out (pr-str res))))]
    (println res)
    (reset! (:ts repl) (System/currentTimeMillis))
    (client-cmd cl-ch [:result (pr-str [sb-key data])])
    (client-cmd (:hist repl) [:hist (pr-str [expr data])])))

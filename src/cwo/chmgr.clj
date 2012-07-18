(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]
            [cwo.sandbox :as sb]
            [cwo.models.user :as user]
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
;  :sub {:vlv :hdl}   Optional, a subscription (valve, handle) to a shared REPL session
;  :pt-vlv            Optional, a pass-thru for your subscribers after a transfer
;  :tsub-vlv          Optional, a subscription to your transfered REPL session
;
;  evaluation sandboxes
;  :you Repl Your primary code evaluation environment
;  :oth Repl Optional, if someone transfers their repl to you
; }
;

; Repl
; {
;  :hist      Channel to send history commands to connected users
;  :sb        Evalutation sandbox
;  :ts        Timestamp of last activity
; }
(defrecord Repl [hist sb ts])

; handle -> User -> cc
; Every web session has an associated channel controller
(def sesh-id->cc (atom {}))

; map to lookup session id from handle
(def handle->sesh-id (atom (sorted-map)))

; channel to update handle list
(def handle-ch (lamina/channel* :permanent? true))

(defn recycle-all!
  "For development use only! Clears out all application state."
  []
  (reset! sesh-id->cc {})
  (reset! handle->sesh-id {}))

(defn recycle!
  "Remove the channel controller map for the specified session id"
  [sesh-id]
  (println "Recycling" sesh-id)
  (declare disconnect client-cmd)
  (let [cc (@sesh-id->cc sesh-id)]
    (when-let [sub-hdl (get-in cc [:sub :hdl])]
      (disconnect sesh-id sub-hdl))
    (when-let [hdl (user/get-handle sesh-id)]
      (client-cmd handle-ch [:rmhandle hdl]))
    (user/rm-user! sesh-id)
    (swap! sesh-id->cc dissoc sesh-id)))
;;
;; helper functions
;;

; avoid eval injections via websocket msgs
(defn safe-read-str [st]
  (binding [*read-eval* false] (read-string st)))

(defn cc-from-handle [handle]
  (@sesh-id->cc (user/get-session handle)))

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
      (contains? rt-set (keyword msg-obj)))))

(defn cmd? 
  ([msg]
   (declare cmd-set)
   (let [msg-obj (safe-read-str msg)]
     (and (vector? msg-obj) (contains? cmd-set (symbol (name (first msg-obj)))))))
  ([msg cmd] (.startsWith msg (str "[" cmd " ")))) ;TODO: this might be better as a regex

; return the number of milliseconds since the specified time
(defn ms-since [t]
  (- (System/currentTimeMillis) t))

; Handle commands send via srv-ch
(defn cmd-hdlr [sesh-id cmd-str]
  (let [[cmd arg] (safe-read-str cmd-str)]
    ((ns-resolve 'cwo.chmgr (symbol (name cmd))) sesh-id arg)))

; create a send/receive channel pair, swap map structure
(defn init-cc! [sesh-id]
  (println "init-cc!" sesh-id)
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

(defn socket-handler [webch handshake]
  (declare login)
  (let [cc (get-cc)
        sesh-id (session/get "sesh-id")]
    (when-let [handle (user/get-handle sesh-id)] 
      (login sesh-id handle))
    (lamina/siphon webch (cc :srv-ch))
    (lamina/siphon (cc :cl-ch) webch)
;    (lamina/on-closed webch #(when-not (= (get-in sesh-id->cc [sesh-id :status]) "gh")
;                               (recycle! sesh-id)))
    (client-cmd (cc :cl-ch) [:inithandles (user/get-broadcasters)])))


;;
;; socket ctrl commands below
;;

(defn broadcast [sesh-id action]
  (let [handle (user/get-handle sesh-id)
        actions {:on (fn []
                       (client-cmd handle-ch [:addhandle handle])
                       (user/set-user! sesh-id {:bc true}))
                 :off (fn []
                        (client-cmd handle-ch [:rmhandle handle])
                        (user/set-user! sesh-id {:bc false}))}]
   ((action actions))))

(defn login [sesh-id handle]
  (user/set-user! sesh-id {:handle handle})
  (println handle "@" sesh-id "signed in")
  (client-cmd handle-ch [:addhandle handle])
  (when-let [pub-hdl (get-in @sesh-id->cc [sesh-id :sub :hdl])]
    (let [{:keys [cl-ch srv-ch]} (cc-from-handle pub-hdl)] 
      (client-cmd cl-ch [:rmanonsub nil])
      (client-cmd cl-ch [:addsub handle])
      (client-cmd srv-ch [:adduser ["#sub-peer-list"]])
      )
    (let [pub-si (user/get-session pub-hdl)] 
      (user/add-peer! pub-si handle) 
      (user/rm-anon-peer! pub-si))))

(defn logout [sesh-id _]
  (let [handle (user/get-handle sesh-id)]
    (println "logout handle" handle)
    (client-cmd handle-ch [:rmhandle handle])
    (user/rm-user! sesh-id)
    (when-let [pub-hdl (get-in @sesh-id->cc [sesh-id :sub :hdl])]
      (let [{cl-ch :cl-ch} (cc-from-handle pub-hdl)]
        (client-cmd cl-ch [:rmsub handle])
        (client-cmd cl-ch [:addanonsub nil]))
      (let [pub-si (user/get-session pub-hdl)]  
        (user/rm-peer! pub-si handle) 
        (user/add-anon-peer! pub-si)))))

(defn subscribe [sesh-id handle]
  (let [publish-sesh-id  (user/get-session handle)
        {{:keys [cl-ch srv-ch you]} publish-sesh-id} @sesh-id->cc ;publisher
        {{old-sub :sub subclch :cl-ch} sesh-id} @sesh-id->cc ;subscriber
        user (user/get-user sesh-id)
        sub-vlv (lamina/channel)]
    (println sesh-id "subscribe to" handle)
    (if user 
      (let [handle (:handle user)]
        (client-cmd cl-ch [:adduser ["#home-peer-list" handle]]) 
        (user/add-peer! publish-sesh-id handle)) 
      (do 
        (client-cmd cl-ch [:addanonsub])
        (user/add-anon-peer! publish-sesh-id)))
    (when old-sub (lamina/close (:vlv old-sub)))
    (swap! sesh-id->cc assoc-in [sesh-id :sub] {:vlv sub-vlv :hdl handle})
    (lamina/siphon (lamina/fork (:hist you)) sub-vlv subclch)
    (lamina/siphon (lamina/filter* (comp not cmd?) srv-ch) sub-vlv subclch)
    (client-cmd subclch [:ts (ms-since @(:ts you))])
    (client-cmd subclch [:initpeers (user/get-peers publish-sesh-id)])))

(defn end-transfer [sesh-id handle]
  (let [hdl-sesh-id (user/get-session handle)
        {pv :pt-vlv tv :tsub-vlv cl :cl-ch} (@sesh-id->cc sesh-id)]
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
  (let [hdl-sesh-id (user/get-session handle)
        {tr-cl :cl-ch tr-srv :srv-ch sub :sub} (@sesh-id->cc hdl-sesh-id) ;transferee
        {old-cl :cl-ch old-srv :srv-ch target-repl :you owner-user :user 
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
      (subscribe (user/get-session trans) (:handle owner-user)))
    (client-cmd (:hist target-repl) [:chctrl handle])
    (lamina/close (:vlv sub)) ; close subscription created by (connect ...)
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
  (let [pub-sesh-id (user/get-session handle)
        {{:keys [cl-ch transfer]} pub-sesh-id} @sesh-id->cc ;publisher
        {{sub :sub sub-user :user} sesh-id} @sesh-id->cc ;subscriber
        sub-hdl (:handle sub-user)] 
    (when (and sub-hdl (= sub-hdl transfer))
      (end-transfer pub-sesh-id sub-hdl)
      (client-cmd (get-in @sesh-id->cc [pub-sesh-id :cl-ch]) [:reclaim :_])
      (client-cmd (:hist (get-in @sesh-id->cc [pub-sesh-id :you])) [:chctrl handle]))
    (lamina/close (:vlv sub))
    (swap! sesh-id->cc assoc-in [sesh-id :sub] nil)
    (if sub-hdl
      (do
        (user/rm-peer! pub-sesh-id handle) 
        (client-cmd cl-ch [:rmsub sub-hdl])) 
      (do
        (client-cmd cl-ch [:rmanonsub nil])
        (user/rm-anon-peer! pub-sesh-id)))))

; reclaim control of sesh-id's REPL from specified handle
(defn reclaim [sesh-id handle]
  (let [hdl-sesh-id (user/get-session handle)
        {owner-user :user cl :cl-ch repl :you} (@sesh-id->cc sesh-id)
        owner-handle (:handle owner-user)]
    (end-transfer sesh-id handle)
    (client-cmd cl [:reclaim :_]) 
    (client-cmd (:hist repl) [:chctrl owner-handle])
    (subscribe hdl-sesh-id owner-handle)))

(defn eval-clj [sesh-id [expr sb-key]]
  (let [{{:keys [cl-ch srv-ch] repl sb-key} sesh-id} @sesh-id->cc
        sb (:sb repl)
        {:keys [result error message] :as res} (evl/eval-expr expr sb)
        data (if error
               res
               (let [[out res] result]
                 (str out (pr-str res))))]
    (reset! (:ts repl) (System/currentTimeMillis))
    (client-cmd cl-ch [:result (pr-str [sb-key data])])
    (client-cmd (:hist repl) [:hist (pr-str [expr data])])
    (client-cmd srv-ch [:ts 0])))

(defn chat [sesh-id [chat-id txt]]
  (let [{:keys [srv-ch cl-ch sub handle]} (@sesh-id->cc sesh-id) 
        chat-hdlr {:you-chat 
                   (fn [t]
                     (client-cmd srv-ch [:othchat [handle txt]])
                     (client-cmd cl-ch [:youchat [handle txt]]))
                   :oth-chat 
                   (fn [t]
                     (let [{:keys [srv-ch cl-ch transfer]} (cc-from-handle (:hdl sub))]
                       (client-cmd srv-ch [:othchat [handle txt]])
                       (client-cmd cl-ch [:youchat [handle txt]])
                       (when transfer
                         (client-cmd (:cl-ch (cc-from-handle transfer)) [:othchat [handle txt]]))))}]
    (((keyword chat-id) chat-hdlr) txt)))

(def cmd-set (set (keys (ns-publics 'cwo.chmgr))))

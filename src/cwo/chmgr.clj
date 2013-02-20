(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [cwo.http :as http]
            [cwo.eval :as evl]
            [cwo.sandbox :as sb]
            [cwo.utils :refer [safe-read-str]]
            [cwo.views.enlive :as el]))

; Channel mgmt architecture
; A channel controller is a map for regulating websocket traffic between clients
; {
;  :srv-ch    Required, permanent channel to send commands to server
;  :cl-ch     Required, permanent channel to send commands to client
;  :handle    Registered handle of this user
;  :sidefx    A queue of side-effects to execute outside dosync block
;  :peers     A vector of connected peers
;  :anon      A count of connected anonymous peers
;  :transfer  A handle that your REPL has been transfered to
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

; channel to update handle listq
; TODO: this should not be at the top-level
(def handle-ch (lamina/channel* :permanent? true))

;;
;; helper functions
;;

; send a command to a websocket client
(defn client-cmd [ch cmdvec]
  (lamina/enqueue ch (pr-str cmdvec)))

(defn cmd? 
  ([msg]
    (declare fn-map)
     (let [msg-obj (safe-read-str msg)]
       (and (vector? msg-obj) (contains? fn-map (first msg-obj)))))
  ([msg cmd] 
     (.startsWith msg (str "[" cmd " ")))) ;TODO: this might be better as a regex

(defn cc-from-handle [app-state handle]
  (@app-state (@(:handles @app-state) handle)))

; return the number of milliseconds since the specified time
(defn ms-since [t]
  (- (System/currentTimeMillis) t))

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

;; RPC methods, declared private

(defn run-sidefx [cc-seq]
"Iterate through seq of cc, empty and call sidefx queue"
  (doseq [cc cc-seq]
    (let [fs (dosync
               (let [q (:sidefx @cc)]
                 (alter cc assoc :sidefx [])
                 q))]
      (doseq [f fs]
        (f)))))

(defn- login [app-state sesh-id handle]
  (println "login!")
  (let [cc (@app-state sesh-id)]
    (if (dosync
          (when-not 
            (or ((ensure (:handles @app-state)) handle) (:handle (ensure cc)))
            (alter cc assoc :handle handle)
            (alter (:handles @app-state) assoc handle sesh-id)
            handle))
      (dosync 
        (let [{:keys [cl-ch]} @cc
              cmds [#(client-cmd handle-ch [:adduser ["#others-list" handle]])
                    #(client-cmd cl-ch [:rehtml ["#user-container" (el/login-html {:handle handle})]])]]
          (alter cc update-in [:sidefx] into cmds))
        (into [cc] 
              (dosync
                (when-let [pub-cc (cc-from-handle app-state (get-in (ensure cc) [:sub :hdl]))]
                  (let [{:keys [cl-ch srv-ch]} @pub-cc
                        cmds [#(client-cmd cl-ch [:rmanon "#home-peer-list"])
                              #(client-cmd srv-ch [:rmanon "#peer-list"])
                              #(client-cmd cl-ch [:adduser ["#home-peer-list" handle]])
                              #(client-cmd srv-ch [:adduser ["#peer-list" handle]])]]
                    (alter pub-cc 
                           (fn [cc]
                             (-> cc 
                               (update-in [:sidefx] into cmds)
                               (update-in [:anon] dec)
                               (update-in [:peers] conj handle))))
                    [pub-cc])))))
      (dosync
        (let [{cl-ch :cl-ch} @cc
              err-msg #(client-cmd cl-ch [:error "Handle taken"])]
          (alter cc #(update-in % [:sidefx] conj err-msg))
          [cc])))))

(defn- logout [app-state sesh-id _]
  (println "logout")
  (let [cc (@app-state sesh-id)]
    (if-let [handle (dosync
                      (let [handle (:handle (ensure cc))]
                        (when
                          (and handle ((ensure (:handles @app-state)) handle) )
                          (alter cc dissoc :handle)
                          (alter (:handles @app-state) dissoc handle)
                          handle)))] 
      (dosync 
        (let [{:keys [cl-ch]} @cc
              cmds [#(client-cmd handle-ch [:rmuser ["#others-list" handle]])
                    #(client-cmd cl-ch [:rehtml ["#user-container" (el/logout-html)]])]]
          (alter cc update-in [:sidefx] into cmds))
        (into [cc] 
              (dosync
                (when-let [pub-cc (cc-from-handle app-state (get-in (ensure cc) [:sub :hdl]))]
                  (let [{:keys [cl-ch srv-ch]} @pub-cc
                        cmds [#(client-cmd cl-ch [:addanon "#home-peer-list"])
                              #(client-cmd srv-ch [:addanon "#peer-list"])
                              #(client-cmd cl-ch [:rmuser ["#home-peer-list" handle]])
                              #(client-cmd srv-ch [:rmuser ["#peer-list" handle]])]]
                    (alter pub-cc 
                           (fn [cc]
                             (-> cc 
                               (update-in [:sidefx] into cmds)
                               (update-in [:anon] inc)
                               (update-in [:peers] disj handle))))
                    [pub-cc])))))
      (dosync
        (let [{cl-ch :cl-ch} @cc
              err-msg #(client-cmd cl-ch [:error "Not logged in"])]
          (alter cc #(update-in % [:sidefx] conj err-msg))
          [cc])))))

(defn- subscribe [app-state sesh-id pub-hdl]
  (println "**" @app-state "**")
  (dosync
    (let [pub-cc (cc-from-handle app-state pub-hdl)
          sub-cc (@app-state sesh-id)
          {:keys [cl-ch srv-ch you]} @pub-cc  ;publisher
          {old-sub :sub subclch :cl-ch} @sub-cc ;subscriber
          sub-vlv (lamina/channel)]
      (when (and pub-cc (:handle @pub-cc))
        (println sesh-id "subscribe to" pub-hdl)
        (if-let [sub-hdl (:handle @sub-cc)]
          (alter pub-cc 
                 (fn [cc]
                   (-> cc 
                     (update-in [:sidefx] 
                                conj 
                                #(client-cmd cl-ch [:adduser ["#home-peer-list" sub-hdl]])
                                #(client-cmd srv-ch [:adduser ["#peer-list" sub-hdl]]))
                     (update-in [:peers] conj sub-hdl))))
          (alter pub-cc 
                 (fn [cc]
                   (-> cc 
                     (update-in [:sidefx] conj #(client-cmd cl-ch [:addanon "#home-peer-list"]))
                     (update-in [:sidefx] conj #(client-cmd srv-ch [:addanon "#peer-list"]))
                     (update-in [:anon] inc)))))
        (when old-sub 
          (alter sub-cc update-in [:sidefx] conj #(lamina/close (:vlv old-sub))))
        (alter sub-cc assoc :sub {:vlv sub-vlv :hdl pub-hdl})
        (let [{:keys [anon peers]} @pub-cc
              cmds [#(lamina/siphon (lamina/fork (:hist you)) sub-vlv subclch)
                    #(lamina/siphon (lamina/filter* (comp not cmd?) srv-ch) sub-vlv subclch)
                    #(client-cmd subclch [:ts (ms-since @(:ts you))])
                    #(client-cmd subclch [:initusers ["#peer-list" peers anon]])]]
          (alter sub-cc update-in [:sidefx] into cmds)))
      [pub-cc sub-cc])))

(defn- end-transfer [app-state sesh-id handle]
  (let [owner-cc (@app-state sesh-id)
        trans-cc (cc-from-handle app-state handle)]
    (dosync
      (let [{pv :pt-vlv tv :tsub-vlv cl :cl-ch} (ensure owner-cc)
            ocmds [#(lamina/close pv)
                   #(lamina/close tv)]
            tcmd #(client-cmd (:cl-ch @trans-cc) [:endtransfer :_])]
        (alter trans-cc #(-> %
                           (update-in [:sidefx] into ocmds)
                           (into {:tsub-vlv nil
                                  :pt-vlv nil})))
        (alter owner-cc #(-> %
                           (update-in [:sidefx] conj tcmd)
                           (into {:oth nil
                                  :transfer nil})))))
    [owner-cc trans-cc]))

; transfer control of sesh-id's REPL to specified handle
(defn- transfer [app-state sesh-id handle]
  (println app-state "+" handle)
  (let [owner-cc (@app-state sesh-id)
        trans-cc (cc-from-handle app-state handle)
        pv (lamina/channel)
        tv (lamina/channel)
        parse-tprompt (fn [msg]
                        (let [{msg :t} (safe-read-str msg)]
                          (pr-str msg)))
        route-hist (fn [msg]
                     (str "[:trepl " msg "]"))
        route-prompt (fn [msg]
                       (let [{prompt-txt :p} (safe-read-str msg)]
                         (pr-str {:t prompt-txt})))
        cc-vec (atom [owner-cc trans-cc])]
    (dosync
      (let [{tr-cl :cl-ch tr-srv :srv-ch sub :sub} (ensure trans-cc)
            {old-cl :cl-ch old-srv :srv-ch target-repl :you owner-hdl :handle 
             trans :transfer} (ensure owner-cc)
            ocmds [#(client-cmd (:hist target-repl) [:chctrl handle])
                   #(lamina/siphon 
                      (lamina/map* parse-tprompt (lamina/filter* (route? #{:t}) tr-srv))
                      pv old-srv)
                   #(lamina/siphon 
                      (lamina/map* route-hist (lamina/fork (:hist target-repl))) tv old-cl)
                   #(lamina/siphon (lamina/map* route-prompt (lamina/fork pv)) tv old-cl)]
            tcmds [#(client-cmd tr-cl [:transfer nil])
                   #(lamina/close (:vlv sub))]]
        (when trans
          (end-transfer app-state sesh-id trans)
          (swap! cc-vec into (subscribe app-state (@(:handles @app-state) trans) owner-hdl)))
        (alter trans-cc #(-> %
                           (update-in [:sidefx] into tcmds)
                           (into {:oth target-repl})))
        (alter owner-cc #(-> %
                           (update-in [:sidefx] into ocmds)
                           (into {:tsub-vlv tv
                                  :pt-vlv pv
                                  :transfer handle})))))
    @cc-vec))

(defn- disconnect [app-state sesh-id handle]
  (println "disconnect" handle)
  (let [pub-cc (cc-from-handle app-state handle)
        sub-cc (@app-state sesh-id)]
    (dosync
      (let [{cl-ch :cl-ch srv-ch :srv-ch trans :transfer pub-repl :you} (and pub-cc (ensure pub-cc))
            {sub :sub sub-hdl :handle} (ensure sub-cc)
            cc-vec (atom [sub-cc])]
        (when cl-ch
          (when (and sub-hdl (= sub-hdl trans))
            (end-transfer app-state (@(:handles @app-state) handle) sub-hdl)
            (alter pub-cc update-in [:sidefx] into [#(client-cmd cl-ch [:reclaim :_])
                                                    #(client-cmd (:hist pub-repl) [:chctrl handle])]))   
          (if sub-hdl
            (alter pub-cc (fn [cc] (-> cc
                                     (update-in [:sidefx] conj #(client-cmd cl-ch [:rmuser ["#home-peer-list" sub-hdl]]))
                                     (update-in [:sidefx] conj #(client-cmd srv-ch [:rmuser ["#peer-list" sub-hdl]]))
                                     (update-in [:peers] disj sub-hdl))))
            (alter pub-cc (fn [cc] (-> cc
                                     (update-in [:sidefx] conj #(client-cmd cl-ch [:rmanon "#home-peer-list"]))
                                     (update-in [:sidefx] conj #(client-cmd srv-ch [:rmanon "#peer-list"]))
                                     (update-in [:anon] dec)))))
          (swap! cc-vec conj pub-cc)) 
        (alter sub-cc (fn [cc] (-> cc
                                 (update-in [:sidefx] conj #(lamina/close (:vlv sub)))
                                 (dissoc :sub))))
        (reverse @cc-vec)))))

; reclaim control of sesh-id's REPL from specified handle
(defn- reclaim [app-state sesh-id handle]
  (let [pub-cc (@app-state sesh-id)
        sub-cc (cc-from-handle app-state handle)]
    (dosync
      (let [{pub-cl :cl-ch repl :you pub-hdl :handle} (ensure pub-cc)
            pub-cmds [#(client-cmd pub-cl [:reclaim :_])
                      #(client-cmd (:hist repl) [:chctrl pub-hdl])]]
        (end-transfer app-state sesh-id handle)
        (alter pub-cc update-in [:sidefx] into pub-cmds)
        (subscribe app-state (@(:handles @app-state) handle) pub-hdl)))
    [pub-cc sub-cc]))

; disconnect any subscriptions, logout, etc
(defn- drop-off [app-state sesh-id]
  (println "drop-off")
  (let [app-snapshot (atom @app-state)]
    (swap! app-state dissoc sesh-id)
    (-> 
      (dosync
        (let [cc (@app-snapshot sesh-id)
              {:keys [sub you srv-ch handle]} (ensure cc)
              cc-vec (atom [cc])
              sub-hdl (:hdl sub)]
          (when sub-hdl
            (swap! cc-vec into (disconnect app-snapshot sesh-id sub-hdl)))
          (when handle 
            (alter cc update-in [:sidefx] into 
                   [#(client-cmd srv-ch [:drop-off handle])
                    #(client-cmd handle-ch [:rmuser ["#others-list" handle]])]))
          (alter (:handles @app-state) dissoc handle)
          @cc-vec))
      run-sidefx)))

(defn- eval-clj [app-state sesh-id expr sb-key]
  (println "eval-clj:" expr)
  (let [{:keys [cl-ch srv-ch] repl sb-key} @(@app-state sesh-id)
        sb (:sb repl)
        {:keys [result error message] :as res} (evl/eval-expr expr sb)
        data (if error
               res
               (let [[out res] result]
                 (str out (pr-str res))))]
    (reset! (:ts repl) (System/currentTimeMillis))
    (client-cmd cl-ch [:result (pr-str [sb-key data])])
    (client-cmd (:hist repl) [:hist (pr-str [expr data])])
    (client-cmd srv-ch [:ts 0])
    nil))

(defn- read-eval-clj [app-state sesh-id [expr sb-key]]
  (eval-clj app-state sesh-id (safe-read-str expr) sb-key))

(defn- paste [app-state sesh-id [host id repl]]
  (println "Paste:" host id repl)
  (let [{cl-ch :cl-ch} @(@app-state sesh-id)
        forms (http/read-paste (keyword host) id)]
    (doseq [form forms]
      (client-cmd cl-ch [:expr (pr-str [repl form])])
      (eval-clj app-state sesh-id form repl))))

(defn- chat [app-state sesh-id [chat-id txt]]
  (println chat-id "-" txt)
  (let [{:keys [srv-ch cl-ch sub handle transfer]} @(@app-state sesh-id) 
        chat-hdlr {:you-chat 
                   (fn [t]
                     (println "youchat")
                     (client-cmd srv-ch [:othchat [handle txt]])
                     (client-cmd cl-ch [:youchat [handle txt]])
                     (when transfer
                       (client-cmd (:cl-ch @(cc-from-handle app-state transfer)) 
                                   [:othchat [handle txt]])))
                   :oth-chat 
                   (fn [t]
                     (println "othchat")
                     (let [{:keys [srv-ch cl-ch transfer]} @(cc-from-handle app-state (:hdl sub))]
                       (client-cmd srv-ch [:othchat [handle txt]])
                       (client-cmd cl-ch [:youchat [handle txt]])
                       (when transfer
                         (client-cmd (:cl-ch @(cc-from-handle app-state transfer)) 
                                     [:othchat [handle txt]]))))}]
    (((keyword chat-id) chat-hdlr) txt)
    nil))

(def fn-map {:login login
             :logout logout
             :subscribe subscribe
             :end-transfer end-transfer
             :transfer transfer
             :disconnect disconnect
             :reclaim reclaim
             :chat chat
             :paste paste
             :read-eval-clj read-eval-clj})

(defn execute [cmd app-state sesh-id arg]
  (when-not (contains? fn-map cmd) 
    (throw (Exception. (str "RPC function not defined: " :cmd))))
  ((cmd fn-map) app-state sesh-id arg))

(defn recycle-all!
  "For development use only! Clears out all application state."
  []
  (throw (Exception. "Recycling not implemented yet")))

; Handle commands send via srv-ch
(defn cmd-hdlr [app-state sesh-id cmd-str]
  (let [[cmd arg] (safe-read-str cmd-str)]
    (run-sidefx ((cmd fn-map) app-state sesh-id arg))))

; create a send/receive channel pair, swap map structure
(defn init-cc! [app-state sesh-id]
  (println "init-cc!" sesh-id)
  (let [newcc (ref {:srv-ch (lamina/channel* :grounded? true :permanent? true)
                    :cl-ch (lamina/channel* :grounded? true :permanent? true)
                    :handle nil
                    :sidefx []
                    :peers #{}
                    :anon 0
                    :you (Repl. (lamina/permanent-channel)
                                (sb/make-sandbox)
                                (atom (System/currentTimeMillis)))})]
    (lamina/receive-all (lamina/filter* cmd? (:srv-ch @newcc)) #(cmd-hdlr app-state sesh-id %))
    (lamina/siphon handle-ch (:cl-ch @newcc))
    (swap! app-state assoc sesh-id newcc)
    newcc))

(defn init-socket [app-state sesh-id sock]
  (let [cc (or (@app-state sesh-id) (init-cc! app-state sesh-id))]
    (lamina/siphon sock (@cc :srv-ch))
    (lamina/siphon (@cc :cl-ch) sock)
    ;(lamina/on-closed sock #(client-cmd (@cc :cl-ch) [:logout nil]))
    (lamina/on-closed sock (fn [] 
                             (println "closing!")
                             (drop-off app-state sesh-id)))
    ;    (lamina/on-closed webch #(when-not (= (get-in app-state [sesh-id :status]) "gh")
    ;                               (recycle! sesh-id)))
    (client-cmd (@cc :cl-ch) [:initclient (keys @(:handles @app-state))])))

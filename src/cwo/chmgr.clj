(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]))

; nested map for assoc'ing session to handle, send/receive pair, and other-repl
(def session-map (atom {}))
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
    (if-let [srp (get-in @session-map [sesh-id :srp])]
      srp
      (let [newsrp (sr-pair)]
        (lamina/receive-all (lamina/filter* #(.startsWith % "[") (newsrp :snd)) #(command-handler sesh-id %))
        (lamina/siphon handle-ch (newsrp :rec))
        (swap! session-map assoc-in [sesh-id :srp] newsrp)
        newsrp))))

(defn broadcast []
  (let [handle (session/get "handle")
        sesh-id (session/get "sesh-id")]
      (swap! handle->sesh-id assoc handle sesh-id)
      (swap! session-map assoc-in [sesh-id :handle] handle)
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
  (let [peer-handle (get-in @session-map [sesh-id :handle] "anonymous")
        target-srp (get-in @session-map [(@handle->sesh-id handle) :srp])
        tmp-ch (lamina/channel)]
    (println sesh-id "subscribe to" handle)
    (lamina/enqueue (target-srp :rec) (pr-str [:addpeer peer-handle]))
    (when-let [old-ch (get-in @session-map [sesh-id :tmp-ch])]
      (lamina/close old-ch))
    (swap! session-map assoc-in [sesh-id :tmp-ch] tmp-ch)
    (lamina/siphon (target-srp :snd) tmp-ch (get-in @session-map [sesh-id :srp :rec]))))

(defn transfer [sesh-id handle]
  (let [new-srp (@session-map (@handle->sesh-id handle))
        tmp-ch (lamina/channel)]
    (lamina/close (new-srp :rec))
    (swap! new-srp assoc-in [sesh-id :tmp-ch] tmp-ch)
    (lamina/siphon nil nil)))


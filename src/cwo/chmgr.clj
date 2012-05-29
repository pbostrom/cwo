(ns cwo.chmgr
  (:require [lamina.core :as lamina]
            [noir.session :as session]))

; nested map for assoc'ing session to handle, send/receive pair, and other-repl
(def session-map (atom {}))
; map to lookup srp from handle
(def handle->srp (atom {}))

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

(defn register []
  (let [handle (session/get "handle")
        srp (get-srp)]
    (when handle 
      (swap! handle->srp assoc handle srp)
      (swap! session-map assoc-in [(session/get "sesh-id") :handle] handle)
      (lamina/enqueue handle-ch (pr-str [:addhandles [handle]])))
    srp))

(defn socket-handler [webch handshake]
  (let [srp (register)]
    (lamina/siphon webch (srp :snd))
    (lamina/siphon (srp :rec) webch)
    (lamina/enqueue (srp :rec) (pr-str [:addhandles (keys @handle->srp)]))))


(defn connect [sesh-id handle]
  (let [peer-handle (get-in @session-map [sesh-id :handle] "anonymous")
        target-ch (@handle->srp handle)]
    (println sesh-id "subscribe to" handle)
    (lamina/enqueue (target-ch :rec) (pr-str [:addpeer peer-handle]))
    (lamina/siphon (target-ch :snd) (get-in @session-map [sesh-id :srp :rec]))))

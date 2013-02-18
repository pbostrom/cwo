(ns cwo.wscmd
  (:require [cwo.utils :refer [jq jslog qry-list select-set get-hash re-html]]
            [crate.core :as crate]
            [cljs.reader :as reader]
            [cwo.repl :as repl]))

; multimethod for dispatching cmds recv'd via websocket
(defmulti wscmd 
  (fn [cmd arg] cmd))

(defmethod wscmd :initclient
  [_ handles]
  (doseq [h handles]
    (-> (jq "#others-list") (.append (crate/html [:option h]))))
  (when-let [hsh (get-hash)]
    (repl/process-hash hsh)))

(defmethod wscmd :adduser
  [_ [list-id handle]]
  (if handle
    (let [list-opts (-> (jq (str list-id " option")) (.not "[class='anon']"))
          all-hdls (conj (select-set list-opts) handle)]
      (.remove list-opts)
      (doseq [h all-hdls]
        (-> (jq list-id) (.append (crate/html [:option h])))))
    :anonymous-case))

(defmethod wscmd :rmuser
  [_ [list-id handle]]
  (-> (qry-list list-id handle)
    (.remove)))

(defmethod wscmd :initusers
  [_ [list-id handles anon]]
  (.remove (jq (str list-id " > option")))
  (when (> anon 0)
    (.append (jq list-id) (crate/html [:option.anon (str anon " anonymous")]))) 
  (doseq [h handles]
    (.append (jq list-id) (crate/html [:option h]))))

(defmethod wscmd :addanon ;TODO consider abstracting next 2 fns
  [_ id]
  (if-let [anon-val (.val (jq (str id " > .anon")))]
    (let [cnt (inc (reader/read-string (let [[n] (.split anon-val " ")] n)))]
      (.text (jq (str id " > .anon")) (str cnt " anonymous")))
    (-> (jq id)
      (.append
        (crate/html [:option.anon (str 1 " anonymous")])))))

(defmethod wscmd :rmanon
  [_ id]
  (when-let [anon-val (.val (jq (str id " > .anon")))]
    (let [cnt (dec (reader/read-string (let [[n] (.split anon-val " ")] n)))]
      (if (> cnt 0)
        (.text (jq (str id " > .anon")) (str cnt " anonymous")) 
        (.remove (jq (str id " > .anon")))))))

(defmethod wscmd :rehtml 
  [_ [id html]]
  (re-html id html))

(defmethod wscmd :transfer 
  [_ _]
  (repl/set-repl-mode :oth :active))

(defmethod wscmd :endtransfer
  [_ _]
  (repl/set-repl-mode :oth :sub))

(defmethod wscmd :reclaim 
  [_ _]
  (.append (jq "#widgets") (jq "#tr-box"))
  (repl/set-repl-mode :you :active))

(defmethod wscmd :expr 
  [_ expr]
  (let [[repl-key expr] (reader/read-string expr)
        repl (repl-key repl/repls)]
    (.SetPromptText repl (str expr))
    (.AbortPrompt repl)))

(defmethod wscmd :result 
  [_ rslt]
  (let [[repl rslt] (reader/read-string rslt)]
    (repl/console-write repl rslt)))

(defmethod wscmd :hist 
  [_ hist-pair & {:keys [repl-key] :or {repl-key :oth}}]
  (let [[expr rslt] (reader/read-string hist-pair)
        repl (repl-key repl/repls)]
    (.SetPromptText repl (str expr))
    (.AbortPrompt repl)
    (if (:error rslt)
      (.Write repl (str (:message rslt) "\n") "jqconsole-error")
      (.Write repl (str rslt "\n") "jqconsole-output"))
    (.Prompt repl true (fn [] nil))))

(defmethod wscmd :chctrl 
  [_ handle & {:keys [repl-key] :or {repl-key :oth}}]
  (when (and (= repl-key :you) (not= handle (.text (jq "#handle"))))
    (.text (jq "#tr-hdl") handle)
    (.attr (jq "#reclaim") "handle" handle)
    (.append (jq "#home-peers") (jq "#tr-box")))
  (.Write (repl-key repl/repls) (str "REPL transferred to " handle "\n") "jqconsole-info"))

(defmethod wscmd :drop-off 
  [_ handle & {:keys [repl-key] :or {repl-key :oth}}]
  (.Write (repl-key repl/repls) (str "REPL owner " handle " has disconnected\n" ) "jqconsole-info")
  (repl/disconnect))

(defmethod wscmd :trepl 
  [_ cmd-vec]
  (apply wscmd (conj cmd-vec :repl-key :you)))

(defmethod wscmd :ts 
  [_ offset]
  (let [now (.getTime (js/Date.))
        t (js/Date. (- now offset))
        date (.toDateString t)
        t-str (.toLocaleTimeString t)]
    (.text (jq "#last-act") t-str)))

(defmethod wscmd :othchat 
  [_ [handle txt]]
  (let [chat (jq "#peer-chat-box pre")]
    (.append chat (str handle ": " txt "\n"))
    (.scrollTop chat (.prop chat "scrollHeight"))))

(defmethod wscmd :youchat 
  [_ [handle txt]]
  (let [chat (jq "#you-chat-box pre")]
    (.append chat (str handle ": " txt "\n"))
    (.scrollTop chat (.prop chat "scrollHeight"))))

(defmethod wscmd :error
  [_ errmsg]
  (js/alert errmsg))

(defmethod wscmd :default
  [cmd _] 
  (throw (js/Error. (str "Command " cmd " not implemented" ))))

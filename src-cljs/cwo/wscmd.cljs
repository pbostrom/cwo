(ns cwo.wscmd
  (:require [cwo.utils :refer [jq jslog select-set get-hash re-html]]
            [crate.core :as crate]
            [cljs.reader :as reader]
            [cwo.repl :as repl]))

(defn- qry-list [list-id opt-val]
  (-> (jq (str list-id " > option"))
    (.filter (fn [idx] (this-as opt (= (.val (jq opt)) opt-val))))))

; update widget
(defmulti update
  (fn [id act] id))

(defmethod update "#home-peer-list"
  [id act]
  (let [cnt (.size (jq (str id " > option")))
        btn (jq "#transfer")]
    (when (and (= cnt 1) (= act :add))
      (.removeAttr btn "disabled"))
    (when (and (= cnt 0) (= act :rm))
      (.attr btn "disabled" "disabled"))))

(defmethod update :default [id f])

; multimethod for dispatching cmds recv'd via websocket
(defmulti wscmd 
  (fn [cmd arg] cmd))

(defmethod wscmd :inithandles
  [_ handles]
  (dorun
    (map #(-> (jq "#others-list")
            (.append
              (crate/html [:option %]))) handles))
  (when-let [hdl (get-hash)]
    (if (contains? (set handles) hdl)
      (do 
        (-> (qry-list "#others-list" hdl) (.click))
        (repl/connect))
      (js/alert (str hdl " is not available")))))

(defmethod wscmd :adduser
  [_ [list-id handle]]
  (if handle
    (let [all-hdls (conj (select-set list-id) handle)]
      (-> (jq (str list-id " option")) (.remove))
      (doseq [h all-hdls]
        (-> (jq list-id) (.append (crate/html [:option h]))))
      (update list-id :add))
    :anonymous-case))

(defmethod wscmd :rmuser
  [_ [list-id handle]]
  (-> (qry-list list-id handle)
    (.remove))
  (update list-id :rm))

(defmethod wscmd :initusers
  [_ [list-id handles]]
  (.remove (jq (str list-id " > option")))
  (doseq [h handles]
    (.append (jq list-id) (crate/html [:option h]))))

(defmethod wscmd :addanonsub ;TODO consider abstracting next 2 fns
  [_ _]
  (if-let [anon-val (.val (jq "#anonsub"))]
    (let [cnt (inc (reader/read-string (let [[n] (.split anon-val " ")] n)))]
      (.text (jq "#anonsub") (str cnt " anonymous")))
    (-> (jq "#home-peer-list")
      (.append
        (crate/html [:option#anonsub (str 1 " anonymous")])))))

(defmethod wscmd :rmanonsub
  [_ _]
  (when-let [anon-val (.val (jq "#anonsub"))]
    (let [cnt (dec (reader/read-string (let [[n] (.split anon-val " ")] n)))]
      (if (> cnt 0)
        (.text (jq "#anonsub") (str cnt " anonymous")) 
        (.remove (jq "#anonsub"))))))

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
  (if (= repl-key :oth)
    (do 
      (.remove (jq "#chctrl"))
      (.append (jq "#status-box tbody")
               (crate/html [:tr#chctrl [:td "Controlled by:"] [:td handle]]))))
  (.Write (repl-key repl/repls) (str "REPL transferred to " handle "\n") "jqconsole-info"))

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
  (let [chat (jq "#oth-chat-box > pre")]
    (.append chat (str handle ": " txt "\n"))
    (.scrollTop chat (.prop chat "scrollHeight"))))

(defmethod wscmd :youchat 
  [_ [handle txt]]
  (let [chat (jq "#you-chat-box > pre")]
    (.append chat (str handle ": " txt "\n"))
    (.scrollTop chat (.prop chat "scrollHeight"))))

(defmethod wscmd :error
  [_ errmsg]
  (js/alert errmsg))

(defmethod wscmd :default
  [cmd _] 
  (throw (js/Error. (str "Command " cmd " not implemented" ))))

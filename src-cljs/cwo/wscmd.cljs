(ns cwo.wscmd
  (:use [cwo.utils :only (jq)])
  (:require [crate.core :as crate]
            [cljs.reader :as reader]
            [cwo.repl :as repl]))

; Do a bit of JS reflection to call method passed in as websocket msg
; Optionally specify a specific repl to run command on.
; Note that we abuse JS lack of arity checking to pass along optional argument
(defn call-wscmd [[cmd arg & opts]]
  (.log js/console (name cmd) ":" (pr-str arg) "-" (pr-str opts))
  (apply (.-value (js/Object.getOwnPropertyDescriptor cwo.wscmd (name cmd))) arg opts))

(defn addhandles [handles]
  (dorun
    (map #(-> (jq "#others-list")
            (.append
              (crate/html [:option %]))) handles)))

; remove an option from a select list
(defn- rmoption [list-id opt-val]
(-> (jq (str list-id " > option"))
    (.filter (fn [idx] (this-as opt (= (.val (jq opt)) opt-val))))
    (.remove)))

(defn addsub [handle]
  (rmoption "#sub-list" handle)
  (-> (jq "#sub-list")
    (.append
      (crate/html [:option handle]))))

(defn rmsub [handle]
  (rmoption "#sub-list" handle))

(defn rmhandle [handle]
  (rmoption "#others-list" handle))

(defn transfer [handle]
  (repl/set-repl-mode :oth :active))

(defn endtransfer [_]
  (repl/set-repl-mode :oth :sub))

(defn reclaim [_]
  (.append (jq "#widgets") (jq "#tr-box"))
  (repl/set-repl-mode :you :active))

(defn result [rslt]
  (let [[repl rslt] (reader/read-string rslt)]
    (repl/console-write repl rslt)))

(defn hist [hist-pair & {:keys [repl-key] :or {repl-key :oth}}]
  (let [[expr rslt] (reader/read-string hist-pair)
        repl (repl-key repl/repls)]
    (.SetPromptText repl (pr-str expr))
    (.AbortPrompt repl)
    (if (:error rslt)
      (.Write repl (str (:message rslt) "\n") "jqconsole-error")
      (.Write repl (str rslt "\n") "jqconsole-output"))
    (.Prompt repl true (fn [] nil))))

(defn chctrl [handle & {:keys [repl-key] :or {repl-key :oth}}]
  (if (= repl-key :oth)
    (do 
      (.remove (jq "#chctrl"))
      (.append (jq "#status-box tbody")
               (crate/html [:tr#chctrl [:td "Controlled by:"] [:td handle]]))))
  (.Write (repl-key repl/repls) (str "REPL transferred to " handle "\n") "jqconsole-info"))

(defn trepl [cmd-vec]
  (call-wscmd (conj cmd-vec :repl-key :you)))

(defn ts [offset]
  (let [now (.getTime (js/Date.))
        t (js/Date. (- now offset))
        date (.toDateString t)
        t-str (.toLocaleTimeString t)]
    (.text (jq "#last-act") t-str)))

(defn thist [hist-pair]
  (let [[expr rslt] (reader/read-string hist-pair)
        repl (:you repl/repls)]
    (.SetPromptText repl (pr-str expr))
    (.AbortPrompt repl)
    (if (:error rslt)
      (.Write repl (str (:message rslt) "\n") "jqconsole-error")
      (.Write repl (str rslt "\n") "jqconsole-output"))
    (.Prompt repl true (fn [] nil))))

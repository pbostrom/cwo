(ns cwo.wscmd
  (:use [cwo.utils :only (jq)])
  (:require [crate.core :as crate]
            [cljs.reader :as reader]
            [cwo.repl :as repl]))

(defn addhandles [handles]
  (dorun
    (map #(-> (jq "#others-list")
            (.append
              (crate/html [:option %]))) handles)))

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

; remove an option from a select list
(defn- rmoption [list-id opt-val]
(-> (jq (str list-id " > option"))
    (.filter (fn [idx] (this-as opt (= (.val (jq opt)) opt-val))))
    (.remove)))

(defn result [rslt]
  (let [[repl rslt] (reader/read-string rslt)]
    (repl/console-write repl rslt)))

(defn hist [hist-pair]
  (let [[expr rslt] (reader/read-string hist-pair)
        repl (:oth repl/repls)]
    (.SetPromptText repl (pr-str expr))
    (.AbortPrompt repl)
    (if (:error rslt)
      (.Write repl (str (:message rslt) "\n") "jqconsole-error")
      (.Write repl (str rslt "\n") "jqconsole-output"))
    (.Prompt repl true (fn [] nil))))

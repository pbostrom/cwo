(ns cwo.wscmd
  (:use [cwo.utils :only (jq)])
  (:require [crate.core :as crate]
            [cwo.repl :as repl]))

(defn addhandles [handles]
  (dorun
    (map #(-> (jq "#others-list")
            (.append
              (crate/html [:option %]))) handles)))

(defn addpeer [handle]
  (rmoption "#peer-list" handle)
  (-> (jq "#peer-list")
    (.append
      (crate/html [:option handle]))))

(defn rmhandle [handle]
  (rmoption "#others-list" handle))

(defn transfer [handle]
  (.Reset repl/others-repl)
  (repl/init-repl repl/others-repl))

; remove an option from a select list
(defn- rmoption [list-id opt-val]
(-> (jq (str list-id " > option"))
    (.filter (fn [idx] (this-as opt (= (.val (jq opt)) opt-val))))
    (.remove)))

(ns cwo.wscmd
  (:use [cwo.utils :only (jq)])
  (:require [crate.core :as crate]))

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

; remove an option from a select list
(defn rmoption [list-id opt-val]
(-> (jq (str list-id " > option"))
    (.filter (fn [idx] (this-as opt (= (.val (jq opt)) opt-val))))
    (.remove)))

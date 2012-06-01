(ns cwo.wscmd
  (:use [cwo.utils :only (jq)])
  (:require [crate.core :as crate]))

(defn addhandles [handles]
  (dorun
    (map #(-> (jq "#others-list")
            (.append
              (crate/html [:option %]))) handles)))

(defn addpeer [handle]
  (-> (jq "#peer-list")
    (.append
      (crate/html [:option handle]))))

(defn rmhandle [handle]
  (-> (jq "#others-list > option")
    (.filter (fn [idx] (this-as opt (= (.val (jq opt)) handle))))
    (.remove)))


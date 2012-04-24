(ns cwo.ajax
  (:require [cljs.reader :as reader])
  (:use [cwo.utils :only (jq map->js)]
        [crate.core :only (html)]))

(defn eval-clojure [code]
  (let [data (atom "")]
    (.ajax jq (map->js {:url "/eval-clj"
                                     :type "POST"
                                     :data (map->js {:expr code})
                                     :async false
                                     :success (fn [res] (reset! data res))}))
    (reader/read-string @data)))

(defn refresh-user [html]
  (-> (jq "#userbox")
    (.empty)
    (.append html)))

(defn login [user]
  (.ajax jq (map->js {:url "/login"
                      :type "POST"
                      :data (map->js {:user user})
                      :async true
                      :success (fn [html]
                                 (refresh-user html)
                                 (-> (jq "#logout")
                                   (.bind "click" logout)))})))

(defn logout []
  (.ajax jq (map->js {:url "/logout"
                      :type "POST"
                      :data (map->js {})
                      :async true
                      :success (fn [html] (refresh-user html)
                                 (-> (jq "#login")
                                   (.bind "click" (fn [] (login (.val (jq "#login-input")))))))})))

(defn sync-ajax [code]
  (.log js/console
        (.ajax jq (map->js {:url "/eval-clj"
                            :type "POST"
                            :data (map->js {:expr code})
                            :async false
                            :success nil}))))

(ns cwo.ajax
  (:require [cljs.reader :as reader])
  (:use [cwo.utils :only (jq map->js)]
        [jayq.core :only (xhr)]
        [crate.core :only (html)]))

(defn eval-clojure [code]
  (let [data (atom "")]
    (.ajax jq (map->js {:url "/eval-clj"
                                     :type "POST"
                                     :data (map->js {:expr code})
                                     :async false
                                     :success (fn [res] (reset! data res))}))
    (reader/read-string @data)))

(defn login [user]
  (.ajax jq (map->js {:url "/login"
                      :type "POST"
                      :data (map->js {:user user})
                      :async true
                      :success (fn [res] (-> (jq "#userbox")
                                           (.empty)
                                           (.append (html [:p user]))))})))

(defn sync-ajax [code]
  (.log js/console
        (.ajax jq (map->js {:url "/eval-clj"
                            :type "POST"
                            :data (map->js {:expr code})
                            :async false
                            :success nil}))))

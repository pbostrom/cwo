(ns cwo.ajax
  (:require [cljs.reader :as reader]
            [cwo.socket :as socket])
  (:use [cwo.utils :only (jq map->js)]))

(defn eval-clojure [code]
  (let [data (atom "")]
    (.ajax jq (map->js {:url "/eval-clj"
                                     :type "POST"
                                     :data (map->js {:expr code})
                                     :async false
                                     :success (fn [res] (reset! data res))}))
    (reader/read-string @data)))

;refresh html of selector
(defn re-html [sel html]
  (-> (jq sel)
    (.html html)))

(defn login [user]
  (.ajax jq (map->js {:url "/login"
                      :type "POST"
                      :data (map->js {:handle user})
                      :success (fn [resp]
                                 (if-not (empty? resp)
                                   (let [{:keys [userbox text]} (reader/read-string resp)]
                                     (re-html "#user-container" userbox)
                                     (re-html "#text-box" text)
                                     (-> (jq "#peer-box") (.show))
                                     (socket/share-console-loop))
                                   (js/alert (str "Handle " user " is taken"))))})))

(defn logout []
  (.ajax jq (map->js {:url "/logout"
                      :type "POST"
                      :success (fn [resp]
                                 (let [{:keys [userbox text]} (reader/read-string resp)]
                                   (re-html "#user-container" userbox)
                                   (re-html "#text-box" text)))})))

(defn sync-ajax [code]
  (.log js/console
        (.ajax jq (map->js {:url "/eval-clj"
                            :type "POST"
                            :data (map->js {:expr code})
                            :async false
                            :success nil}))))

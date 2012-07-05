(ns cwo.ajax
  (:require [cljs.reader :as reader])
  (:use [cwo.utils :only [jq map->js]]))

(defn eval-clojure [code sb]
  (let [data (atom "")]
    (.ajax jq (map->js {:url "/eval-clj"
                        :type "POST"
                        :data (map->js {:expr code :sb (name sb)})
                        :async false
                        :success (fn [res] (reset! data res))}))
    (reader/read-string @data)))

;refresh html of selector
(defn re-html [sel html]
  (-> (jq sel)
    (.html html)))

(defn login [user other]
  (.ajax jq (map->js {:url "/login"
                      :type "POST"
                      :data (map->js {:handle user})
                      :success (fn [resp]
                                 (if-not (empty? resp)
                                   (let [{:keys [userbox text]} (reader/read-string resp)]
                                     (re-html "#user-container" userbox)
                                     (.append (jq "#widgets") (jq "#default-text"))
                                     (.append (jq "#panel-box") (jq "#your-panel")))
                                   (js/alert (str "Handle " user " is taken"))))})))

(defn logout []
  (.ajax jq (map->js {:url "/logout"
                      :type "POST"
                      :success (fn [resp]
                                 (let [{:keys [userbox text]} (reader/read-string resp)]
                                   (re-html "#user-container" userbox)
                                   (.append (jq "#status-you") (jq "#default-text"))
                                   (.append (jq "#widgets") (jq "#statusbox-you"))))})))

(defn sync-ajax [code]
  (.log js/console
        (.ajax jq (map->js {:url "/eval-clj"
                            :type "POST"
                            :data (map->js {:expr code})
                            :async false
                            :success nil}))))

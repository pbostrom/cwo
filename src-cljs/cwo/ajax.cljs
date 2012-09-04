(ns cwo.ajax
  (:require [cljs.reader :as reader]
            [crate.core :as crate]
            [cwo.utils :refer [jq map->js jslog srv-cmd re-html]]))

(defn eval-clojure [code sb]
  (let [data (atom "")]
    (.ajax jq (map->js {:url "/eval-clj"
                        :type "POST"
                        :data (map->js {:expr code :sb (name sb)})
                        :async false
                        :success (fn [res] (reset! data res))}))
    (reader/read-string @data)))

;refresh html of selector

(defn gh-profile [token]
  (.ajax jq (map->js 
              {:url "https://api.github.com/user"
               :type "GET"
               :data (map->js {:access_token token})
               :success (fn [resp]
                          (let [user-map (js->clj resp)
                                handle (user-map "login")
                                ]
                            (.prepend (jq "#logoutbox") 
                                     (crate/html [:img#avatar {:src (user-map "avatar_url")}]))
                            (.text (jq "#handle") handle)
                            (srv-cmd :login handle)))})))

(defn login [user]
  (.ajax jq (map->js {:url "/login"
                      :type "POST"
                      :data (map->js {:handle user})
                      :success (fn [resp]
                                 (if-not (empty? resp)
                                   (let [{:keys [userbox text]} (reader/read-string resp)]
                                     (re-html "#user-container" userbox)
                                     (.removeClass (jq "#bc-radio > button") "disabled")
                                     (.removeClass (jq "#bc-radio > button") "active"))
                                   (js/alert (str "Handle " user " is taken"))))})))

(defn logout []
  (.ajax jq (map->js {:url "/logout"
                      :type "POST"
                      :success (fn [resp]
                                 (let [{:keys [userbox text]} (reader/read-string resp)]
                                   (re-html "#user-container" userbox)
                                   (.addClass (jq "#bc-radio > button") "disabled")
                                   (.append (jq "#status-you") (jq "#default-text"))
                                   (.append (jq "#widgets") (jq "#statusbox-you"))))})))

(defn sync-ajax [code]
  (.log js/console
        (.ajax jq (map->js {:url "/eval-clj"
                            :type "POST"
                            :data (map->js {:expr code})
                            :async false
                            :success nil}))))

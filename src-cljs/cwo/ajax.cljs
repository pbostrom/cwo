(ns cwo.ajax
  (:require [crate.core :as crate]
            [cwo.utils :refer [jq map->js jslog srv-cmd re-html]]))

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

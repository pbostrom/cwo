(ns cwo.http
  (:require [clj-http.client :as client]))

(defn post-access-code [code]
  (client/post "https://github.com/login/oauth/access_token"
               {:form-params {:client_id "462bb4a4d01b06852938"
                              :client_secret  "66328ded3b96e566a883a44fa9036c04b30e9169"
                              :code code }}))

(defn get-user []
  (client/get "https://api.github.com/user"))

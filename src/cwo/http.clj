(ns cwo.http
  (:require [clj-http.client :as client]
            [cwo.config :as cfg]))

(defn post-access-code [code]
  (client/post (:access-url cfg/cfg)
               {:form-params {:client_id (:client_id cfg/cfg)
                              :client_secret (:client_secret cfg/cfg)
                              :code code }}))

(defn get-user [token]
  (client/get (str "https://api.github.com/user?access_token=" token)))

(def tk (get-in @cwo.chmgr/sesh-id->cc ["2d5b3bf0-9963-4b42-b341-d1a04f3cc13d" :gh :token]))
(get-user tk)

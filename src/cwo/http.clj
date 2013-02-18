(ns cwo.http
  (:require [clj-http.client :as client]
            [cheshire.core :as cheshire]
            [cwo.config :as cfg]))

(defn post-access-code [code]
  (client/post (:access-url cfg/cfg)
               {:form-params {:client_id (:client_id cfg/cfg)
                              :client_secret (:client_secret cfg/cfg)
                              :code code }}))

(defn get-user [token]
  (client/get (str "https://api.github.com/user?access_token=" token)))

(defn get-as-clj [url]
  (cheshire/parse-string (:body (client/get url)) true))

(defmulti get-paste
  (fn [host id] host))

(defmethod get-paste :gist
  [host id]
  (let [url (str "https://api.github.com/gists/" id)]
    (map :content (vals (:files (get-as-clj url))))))

(defmethod get-paste :refheap
  [host id]
  (let [url (str "https://www.refheap.com/api/paste/" id)
        forms (get-as-clj url)])) 
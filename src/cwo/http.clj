(ns cwo.http
  (:require [clj-http.client :as client]
            [clojure.core.cache :as cache]
            [cheshire.core :as cheshire]
            [cwo.utils :refer [read-forms]]
            [cwo.config :as cfg]))

(defn post-access-code [code]
  (client/post (:access-url cfg/cfg)
               {:form-params {:client_id (:client_id cfg/cfg)
                              :client_secret (:client_secret cfg/cfg)
                              :code code }}))

(defn get-user [token]
  (client/get (str "https://api.github.com/user?access_token=" token)))

(defn cache-update
  "Get from cache or call miss function"
  [C key missfn]
  (if (cache/has? C key)
    (cache/hit C key)
    (cache/miss C key (missfn))))

(defn cache-fetch!
  "Return value "
  [C-atom key missfn]
  (get (swap! C-atom #(cache-update % key missfn)) key))

(defn http-cache-fetch!
  "Return value "
  [C key missfn])

;; TODO: top-level atom, consider alternatives
(def L2 {:C (cache/lru-cache-factory {}) :missfn #(client/get url)} )
(def L1 {:C (cache/ttl-cache-factory {} :ttl 30000) :missfn } )

(defn get-as-clj [url]
  (cheshire/parse-string (:body (fetch-paste url)) true))

(defmulti read-paste
  (fn [host id] host))

(defmethod read-paste :gist
  [host id]
  (let [url (str "https://api.github.com/gists/" id)]
    (mapcat read-forms (map :content (vals (:files (get-as-clj url)))))))

(defmethod read-paste :refheap
  [host id]
  (let [url (str "https://www.refheap.com/api/paste/" id)]
    (read-forms (:contents (get-as-clj url)))))

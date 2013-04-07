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

(defn cache-fetch!
  [{:keys [C missfn not-stale?]} url]
  (let [update-fn (fn [c] (if (and (cache/has? c url) (not-stale? c url))
                           (cache/hit c url)
                           (cache/miss c url (missfn url))))]
    (cache/lookup (swap! C #(update-fn %)) url)))

(defn etag-not-stale? [c url]
  (let [cached-etag (get-in (cache/lookup c url) [:headers "etag"])
        server-etag (get-in (client/head url) [:headers "etag"])]
    (println "Comparing etags -- cache:" cached-etag "server:" server-etag)
    (= cached-etag server-etag)))

;; TODO: top-level atoms, consider alternatives
(def L2 {:C (atom (cache/lru-cache-factory {}))
         :missfn #(do (println "Get latest from server") (client/get %))
         :not-stale? etag-not-stale?})

;; always assume TTL cache values are not stale (only 30 sec window)
(def L1 {:C (atom (cache/ttl-cache-factory {} :ttl 30000)) :missfn #(cache-fetch! L2 %) :not-stale? (fn [_ _] true)})

(defn get-as-clj [url]
  (cheshire/parse-string (:body (cache-fetch! L1 url)) true))

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

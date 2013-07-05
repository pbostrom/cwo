(ns cwo.twitter
  (:require [oauth.twitter :as oauth]
            [clojure.edn :as edn])
  (:import [java.io InputStreamReader BufferedReader]))

(defn client
  [{:keys [consumer-key consumer-secret access-token access-token-secret]}]
  (oauth/oauth-client consumer-key consumer-secret access-token access-token-secret))

(def credentials (edn/read-string (slurp "credentials.edn")))

(def twitter-client (client credentials))

(defn tweet
  [status]
  (twitter-client {:method :post
           :url "https://api.twitter.com/1.1/statuses/update.json"
           :form-params {:status status}}))

(defn mentions
  []
  (twitter-client {:method :get
           :url "https://api.twitter.com/1.1/statuses/mentions_timeline.json"}))

(defn stream-mentions []
  (let [stream (twitter-client {:method :get
                         :url "https://userstream.twitter.com/1.1/user.json"
                         :as :stream})]
    (line-seq (BufferedReader. (InputStreamReader. stream)))))

(ns cwo.twitter
  (:require [oauth.twitter :as oauth]
            [oauth.v1 :as oauth-v1]
            [cwo.utils :as utils]
            [cwo.eval :as evl]
            [clj-http.client :as client]
            [clj-http.util :refer [url-encode]]
            [clojure.edn :as edn]
            [overtone.at-at :as at-at]
            [clojure.core.async :refer :all])
  (:import [java.util.concurrent Executors TimeUnit]
           [java.io InputStreamReader BufferedReader]))

(defn client
  [{:keys [consumer-key consumer-secret access-token access-token-secret]}]
  (oauth/oauth-client consumer-key consumer-secret access-token access-token-secret))

(def credentials (edn/read-string (slurp "credentials.edn")))

(def twitter-client (client credentials))

(defn get-tweets
  [user]
  (twitter-client {:method :get
                   :url (str "https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=" user)}))

(defn tweet
  [status]
  (twitter-client {:method :post
           :url "https://api.twitter.com/1.1/statuses/update.json"
           :form-params {:status status}}))

(defn mentions
  []
  (twitter-client
   {:method :get
    :url "https://api.twitter.com/1.1/statuses/mentions_timeline.json"
    :query-params {"trim_user" "false"}}))

(defn stream-mentions []
  (let [stream (twitter-client {:method :get
                         :url "https://userstream.twitter.com/1.1/user.json"
                         :as :stream})]
    (line-seq (BufferedReader. (InputStreamReader. stream)))))

(defn timeout-ex []
 (let [t (timeout 1200)
      begin (System/currentTimeMillis)]
  (<!! t)
  (println "Waited" (- (System/currentTimeMillis) begin))))

(defn fake-mentions []
  [{:text "@cwoio (defn lame[]" :id 111 :user {:screen_name "pip"}}])

(defn parse-mentions []
  (doseq [{text :text id :id {screen_name :screen_name} :user} (mentions)]
    (let [forms (subs text 7)
          sb nil]
      (doseq [form (utils/read-forms forms)]
        (let [result (evl/eval-expr form sb)])))
    (println [screen_name (subs text 7) id])))

(defn start-old []
  (let [executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleAtFixedRate executor parse-mentions 0 65 TimeUnit/SECONDS)))


(defonce twtr-pool (at-at/mk-pool))

(defn cycle-task [f period]
  (at-at/every period f twtr-pool :fixed-delay true))


(defn process-mentions []
  (loop [ts (mentions)
        error false]
   (when (and (not error) (seq ts))
     (let [{text :text id :id {screen_name :screen_name} :user} (first ts)
           forms (subs text 7)
           sb nil])
     (println (first ts))
     (recur (rest ts) (< 5 (first ts))))))

(defn request-token []
  (let [{:keys [consumer-key consumer-secret]} credentials]
    (:oauth-token
     ((oauth-v1/make-consumer
       :oauth-consumer-key consumer-key
       :oauth-consumer-secret consumer-secret
       :oauth-callback "http://localhost:8080"
       :form-params {:x_auth_access_type "read"})
      {:method :post :url "https://api.twitter.com/oauth/request_token"}))))

(defn access-token [token verifier]
  (let [{:keys [consumer-key consumer-secret]} credentials]
    (seq
     ((oauth-v1/make-consumer
       :oauth-token token
       :form-params {:oauth_verifier verifier})
      {:method :post :url "https://api.twitter.com/oauth/access_token"}))))

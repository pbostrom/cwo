(ns cwo.twitter
  (:require [oauth.twitter :as oauth]
            [oauth.v1 :as oauth-v1]
            [cwo.utils :as utils]
            [cwo.eval :as evl]
            [cwo.chmgr :as chmgr]
            [cwo.redis :as redis]
            [clj-http.client :as client]
            [clj-time.core :as time]
            [clj-http.util :refer [url-encode]]
            [clojure.edn :as edn]
            [overtone.at-at :as at-at])
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

(defn take-st
  "Return a string of the first n lines of the stack trace"
  [n st]
  (reduce #(str % "\t" (.toString %2) "\n") "" (take n st)))

(defmacro tc-wrap [try-form catch-form]
  `(try
     ~try-form
     (catch Exception e#
       (let [st# (take-st 16 (.getStackTrace e#))
             es# (.toString e#)
             cs# (.getCause e#)]
         (spit "twitter-http.log" (str (.toString (time/now)) "\n")
               :append true)
         (spit "twitter-http.log" (str es# " at\n" st# "Cause: " cs# "\n")
               :append true))
       ~catch-form)))

(defn reply
  [user reply-id status twt-log]
  (tc-wrap
   (do
     (twitter-client {:method :post
                     :url "https://api.twitter.com/1.1/statuses/update.json"
                     :form-params {:status (str "@" user " " status)
                                   :in_reply_to_status_id reply-id}})
     (swap! twt-log update-in [:replies] conj [user reply-id])
     (redis/set :since-id reply-id))
   (swap! twt-log update-in [:errors] conj [user reply-id status])))

(defn mentions
  [since-id]
  (tc-wrap
   (twitter-client
    {:method :get
     :url "https://api.twitter.com/1.1/statuses/mentions_timeline.json"
                                        ;    :throw-exceptions true
     :query-params {"trim_user" "false"
                    "since_id" since-id}})
   []))

(defn wrap-debug [f]
  (with-redefs [mentions (fn [_] (edn/read-string (slurp "debug-tweets")))
                tweet (fn [status]
                        (spit "debug-post-tweet" (str status "\n") :append true))]
    (f)))

(defn parse-mention [t]
  (let [{:keys [text id user entities]} t
        {:keys [screen_name]} user
        offset (-> entities :user_mentions first :indices last)]
    [(subs text offset) text id screen_name]))

(defn save-cycle-log [cycle-id log]
  (doseq [[k v] log]
    (redis/hset k cycle-id v)))

(defn twitter-repl-impl [app-state]
  (let [twt-log (atom {:mentions [] :replies [] :errors []})
        cycle-id (redis/incr "cycle-id")
        since-id (redis/get :since-id)] ; TODO: 
    
    (spit "twitter-http.log"
          (str (.toString (time/now)) " polling mentions; cycle: " cycle-id "\n")
          :append true)
    (doseq [[sexps _ id screen_name] (reverse (map parse-mention (mentions since-id)))]
      (swap! twt-log update-in [:mentions] conj id)
      (let [repl (chmgr/get-twitter-repl app-state screen_name)
            cl-ch (some-> (chmgr/cc-from-handle app-state screen_name) deref :cl-ch)]
        (doseq [sexp (tc-wrap
                      (utils/read-forms sexps)
                      [(str "Read error:" sexps)])]
          (let [result (evl/eval-expr sexp (:sb repl))
                hist-str (pr-str [sexp result])]
            (chmgr/client-cmd (:hist repl) [:hist hist-str])
            (and cl-ch (chmgr/client-cmd cl-ch [:trepl [:hist hist-str]]))
            (and cl-ch (chmgr/client-cmd cl-ch [:activate-repl nil]))
            (reply screen_name id result twt-log)
            (println [screen_name id sexp result])))))
    (save-cycle-log cycle-id @twt-log))
; TODO: grab any failures from DB and retry them here
  )

(defn twitter-repl [app-state]
  (tc-wrap (twitter-repl-impl app-state) nil))

(defn request-token []
  (let [{:keys [consumer-key consumer-secret]} credentials]
    (:oauth-token
     ((oauth-v1/make-consumer
       :oauth-consumer-key consumer-key
       :oauth-consumer-secret consumer-secret
       :oauth-callback "http://cwo.io/siwted"
       :form-params {:x_auth_access_type "read"})
      {:method :post :url "https://api.twitter.com/oauth/request_token"}))))

(defn access-token [token verifier]
  (let [{:keys [consumer-key consumer-secret]} credentials]
    (seq
     ((oauth-v1/make-consumer
       :oauth-token token
       :form-params {:oauth_verifier verifier})
      {:method :post :url "https://api.twitter.com/oauth/access_token"}))))

(defn stream-mentions []
  (let [stream (twitter-client {:method :get
                         :url "https://userstream.twitter.com/1.1/user.json"
                         :as :stream})]
    (line-seq (BufferedReader. (InputStreamReader. stream)))))

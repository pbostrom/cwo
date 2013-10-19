(ns cwo.routes
  (:require [compojure.core :refer [defroutes GET]]
            [cwo.views.enlive :as enlive]
            [cwo.http :as http]
            [cwo.chmgr :as chmgr]
            [cwo.twitter :as twitter]
            [ring.util.response :as resp]
            [ring.util.codec :as codec]
            [ring.middleware.params :as params]
            [cwo.config :as cfg]))

(defn fmap [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn sanitize [s & [max-len]]
  "URL-encode string, optionally truncate encoded string to max-len"
  (codec/url-encode s)
  (when max-len
    (let [s (clojure.string/trim s)
          l (.length s)]
      (.substring s 0 (min l max-len)))))

(def qry-pt (re-pattern "([^=&]*)[=]([^&]*)"))

(defn parseqry
  "Parse a query string into a map with keywords"
  [qry]
  (into {} (for [[_ k v] (re-seq qry-pt qry)] [(keyword k) v])))

(defn fetch-token [code]
  ; TODO: do this in another thread?
  ; TODO: start a thread to fetch github handle
  (when-let [body (:body (http/post-access-code code))]
    (:access_token (parseqry body))))

(defn session-data [{:keys [app-state cookies]}]
  (let [sesh-id (get-in cookies ["ring-session" :value])
        sesh-state (@app-state sesh-id)
        handle (and sesh-state (@sesh-state :handle))]
    [sesh-id handle]))

(defn root [{:as req}]
  (println req)
  (let [[sesh-id handle] (session-data req)]
    (println "sesh-id:" sesh-id)
    (println "handle:" handle)
    {:status 200
     :headers {}
     ; stick dummy value into session so ring generates session key
     :session {"foo" {:value "bar"}}
     :body (enlive/layout {:handle handle})}))

;; enlive rendered routes
(defroutes app-routes
  (GET "/paste*" {:as req}
       (root req))
  (GET "/" {:as req}
       (root req))
  (GET "/ghauth" []
       (let [sesh-id nil]
         (println sesh-id {:status "gh"}))
       (resp/redirect (cfg/auth-url)))
  (GET "/siwt" []
       (let [url "https://api.twitter.com/oauth/authenticate?oauth_token="
             tok (twitter/request-token)]
         (resp/redirect (str url tok))))
  (params/wrap-params
   (GET "/siwted" [oauth_token oauth_verifier :as req]
        (let [content (into {} (twitter/access-token oauth_token oauth_verifier))
              {:keys [screen-name]} content
              {:keys [app-state cookies]} req
              sesh-id (get-in cookies ["ring-session" :value])]
          (chmgr/do-siwt app-state sesh-id screen-name)
          (resp/redirect "/")))))

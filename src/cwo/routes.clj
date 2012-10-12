(ns cwo.routes
  (:require [compojure.core :refer [defroutes GET]]
            [cwo.views.enlive :as enlive]
            [cwo.http :as http]
            [ring.util.response :as resp]
            [ring.util.codec :as codec]
            [cwo.models.user :as user]
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

;; enlive rendered routes
(defroutes app-routes
  (GET "/" {:keys [code] :as req}
       (let [sesh-id nil]
         ;(session/put! "sesh-id" sesh-id)
         (println "req map:" req)
         (if code
           (do
             (when-let [token (fetch-token code)]
               (user/set-user! sesh-id {:token token :status "auth"})) 
             (resp/redirect "/"))
           {:status 200
            :headers {}
            ; stick dummy value into session so ring generates session key
            :session {"foo" {:value "bar"}}
            :body (enlive/layout (and sesh-id (user/get-user sesh-id)))})))
  (GET "/ghauth" []
       (let [sesh-id nil]
         (user/set-user! sesh-id {:status "gh"}))
       (resp/redirect (cfg/auth-url))))

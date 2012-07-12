(ns cwo.views.noir
  (:require [noir.core :refer [defpage]]
            [cwo.chmgr :as chmgr]
            [cwo.models.user :as user]
            [cwo.views.enlive :as enlive]
            [cwo.http :as http]
            [ring.util.response :as resp]
            [ring.util.codec :as codec]
            [noir.session :as session]
            [cwo.config :as cfg]
            [noir.cookies :as cookies]))

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
(defpage "/" {:keys [code]}
  (let [sesh-id (cookies/get "ring-session")]
    (session/put! "sesh-id" sesh-id)
    (if code
      (do
        (when-let [token (fetch-token code)]
          (user/set-user! sesh-id {:token token :status "auth"})) 
        (resp/redirect "/"))
      (enlive/layout (user/get-user sesh-id)))))

(defpage "/ghauth" []
  (let [sesh-id (session/get "sesh-id")]
    (user/set-user! sesh-id {:token token :status "gh"}))
  (resp/redirect (cfg/auth-url)))

(defpage [:post "/login"] {:keys [handle]}
  (let [handle (sanitize handle)])
  (if-not (contains? @chmgr/handle->sesh-id handle)
    (do
      (chmgr/login (session/get "sesh-id") handle)
      (pr-str (fmap (enlive/si-content {:handle handle}) enlive/render-snippet)))
    ""))

(defpage [:post "/logout"] {:keys [user]}
  (chmgr/logout (session/get "sesh-id") nil)
  (pr-str (fmap (enlive/default-content) enlive/render-snippet)))

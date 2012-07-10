(ns cwo.views.noir
  (:require [noir.core :refer [defpage]]
            [cwo.chmgr :as chmgr]
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
  (when-let [body (:body (http/post-access-code code))]
    (:access_token (parseqry body))))

;; enlive rendered routes
(defpage "/" {:keys [code]}
  (let [sesh-id (cookies/get "ring-session")]
    (session/put! "sesh-id" sesh-id)
    (if code
      (do
        (when-let [token (fetch-token code)]
          (swap! chmgr/sesh-id->cc update-in [sesh-id :gh]
                 #(assoc %1 :token %2 :status "auth") token)) 
        (resp/redirect "/")) 
      (enlive/layout (get-in @chmgr/sesh-id->cc [sesh-id :gh :token])))))

(defpage "/ghauth" []
  (let [sesh-id (session/get "sesh-id")]
    (swap! chmgr/sesh-id->cc assoc-in [sesh-id :status] "gh"))
  (resp/redirect (cfg/auth-url)))

(defpage [:post "/login"] {:keys [handle]}
  (let [handle (sanitize handle)])
  (if-not (contains? @chmgr/handle->sesh-id handle)
    (do
      (session/put! "handle" handle)
      (chmgr/login)
      (pr-str (fmap (enlive/si-content handle) enlive/render-snippet)))
    ""))

(defpage [:post "/logout"] {:keys [user]}
  (chmgr/logout)
  (session/remove! "handle")
  (pr-str (fmap (enlive/default-content) enlive/render-snippet)))

(ns cwo.views.noir
  (:require [noir.core :refer [defpage]]
            [cwo.chmgr :as chmgr]
            [cwo.views.enlive :as enlive]
            [cwo.http :as http]
            [ring.util.response :as resp]
            [ring.util.codec :as codec]
            [noir.session :as session]
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
  (session/put! "sesh-id" (cookies/get "ring-session"))
  (if code
    (let [sesh-id (session/get "sesh-id")]
      (println "sesh-id:" sesh-id "code:" code)
      (when-not (= (get-in @chmgr/sesh-id->cc [sesh-id :status]) "gh")
        (println "warning: no github marker"))
      (when-let [token (fetch-token code)]
         (swap! chmgr/sesh-id->cc update-in [sesh-id :gh]
             #(assoc %1 :token %2 :status "auth") token))
      (resp/redirect "/"))
  (enlive/layout (session/get "handle"))))

(defpage "/ghauth" []
  (let [sesh-id (session/get "sesh-id")]
    (swap! assoc-in chmgr/sesh-id->cc [sesh-id :status] "gh"))
  (resp/redirect "https://github.com/login/oauth/authorize?client_id=462bb4a4d01b06852938"))

(defpage "/redir" {:keys [v]}
  (if v
    (resp/redirect "/redir?foo=bar")
    "Hi"))

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

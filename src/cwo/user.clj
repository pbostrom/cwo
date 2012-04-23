(ns cwo.user
  (:require [noir.session :as session]
            [noir.cookies :as cookies]))

(defn cookie-map [value]
  {:value value
   :path "/"
   :max-age 30000 })

(def rm-cookie-map
  {:value "null"
   :path "/"
   :max-age 0 })

(defn get-user []
  (if-let [user (session/get "username")]
    user
    (if-let [user (cookies/get "username")]
      (do
        (println "cookie set with" user)
        (session/put! "username" user)
        user))))

(defn put-user [user]
  (session/put! "username" user)
  (cookies/put! "username" (cookie-map user)))

(defn rm-user []
  (session/remove! "username")
  (cookies/put! "username" rm-cookie-map))

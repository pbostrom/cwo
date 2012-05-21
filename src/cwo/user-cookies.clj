(ns cwo.user-cookies
  (:require [noir.session :as session]
            [noir.cookies :as cookies]))

(def active-users (atom (sorted-set)))

(defn cookie-map [value]
  {:value value
   :path "/"
   :http-only nil
   :max-age 300000})

(def rm-cookie-map
  {:value "null"
   :path "/"
   :max-age 0 })

(defn get-user []
  (if-let [user (session/get "username")]
    user
    (if-let [user (cookies/get "username")]
      (do
        (session/put! "username" user)
        user))))

(defn put-user [user]
  (session/put! "username" user)
  (cookies/put! "username" (cookie-map user))
  (swap! active-users conj user))

(defn rm-user []
  (let [user (session/get "username")]
    (session/remove! "username")
    (cookies/put! "username" rm-cookie-map)
    (swap! active-users disj user)))

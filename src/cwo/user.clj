(ns cwo.user
  (:require [noir.session :as session]
            [noir.cookies :as cookies]))

(defn get-user []
  (if-let [user (session/get "username")]
    user
    (if-let [user (cookies/get "username")]
      (session/put! "username" user))))

(defn put-user [user]
  (session/put! "username" user)
  (cookies/put! "username" user))

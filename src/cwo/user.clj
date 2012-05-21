(ns cwo.user
  (:require [noir.session :as session]))

(def active-users (atom (sorted-set)))

(defn get-user []
  (session/get "username"))

(defn put-user [user]
  (session/put! "username" user)
  (swap! active-users conj user))

(defn rm-user []
  (let [user (session/get "username")]
    (session/remove! "username")
    (swap! active-users disj user)))

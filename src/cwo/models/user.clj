(ns cwo.models.user
  "Use the session id as primary key to model
   a user of the app with these optional fields:
    :handle   Registered handle of user
    :status   Authentication status [default, gh, auth]
    :token    GitHub access token
    :peers    List of peers connected to a REPL
    :bc       Boolean indicating whether REPL broadcast is active"
  (:require [monger.operators :refer [$set $unset $addToSet $pull $inc]]
            [monger.collection :as mc]))

(defn set-user!
  "Upserts a user with the specified fields."
  [session fields]
  (mc/update "users" {:_id session} {$set fields} :upsert true))

(defn unset-user!
  "Unsets the specified fields"
  [session fields]
  (mc/update "users" {:_id session} {$unset fields}))

(defn rm-user!
  "Removes the user from the db"
  [session]
  (mc/remove-by-id "users" session))

(defn get-user
  "Get the user for the specified session."
  [session]
  (mc/find-map-by-id "users" session))

(defn get-handle
  "Get the handle registered with the specified session."
  [session]
  (:handle (mc/find-map-by-id "users" session)))

(defn get-session
  "Get the session id of the specified handle"
  [handle]
  (:_id (mc/find-one-as-map "users" {:handle handle})))

(defn get-broadcasters
  "Returns handles of all users that are broadcasting a REPL"
  []
  (reduce 
    (fn [v {:keys [handle]}] (conj v handle)) []
    (mc/find-maps "users" {} [:handle])))

(defn get-peers
  "Returns list of peers (including anonymous count) connected to a REPL"
  [session]
  (:peers (mc/find-map-by-id "users" session)))

(defn add-peer!
  "Update list of peers connected to a REPL"
  [session peer]
  (:peers (mc/update "users" {:_id session} {$addToSet {:peers peer}})))

(defn add-anon-peer!
  "Update count of anonymous peers connected to a REPL"
  [session]
  (:peers (mc/update "users" {:_id session} {$inc {:anon 1}})))

(defn rm-anon-peer!
  "Update count of anonymous peers connected to a REPL"
  [session]
  (:peers (mc/update "users" {:_id session} {$inc {:anon -1}})))

(defn rm-peer!
  "Update list of peers (including anonymous count) connected to a REPL"
  [session peer]
  (:peers (mc/update "users" {:_id session} {$pull {:peers peer}})))

(defn signed-in? [user]
  (or ((comp not nil?) (:token user))((comp not nil?) (:handle user))))

(defn broadcasting? [user]
  (= (:bc user) true))

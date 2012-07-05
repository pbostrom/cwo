(ns cwo.views.noir
  (:require [cwo.chmgr :as chmgr]
            [cwo.views.enlive :as enlive]
            [noir.session :as session]
            [noir.cookies :as cookies])
  (:use [noir.core  :only (defpage)]))

(defn fmap [m f]
  (into {} (for [[k v] m] [k (f v)])))

;; enlive rendered routes
(defpage "/" []
  (session/put! "sesh-id" (cookies/get "ring-session"))
  (enlive/layout (session/get "handle")))

(defpage [:post "/login"] {:keys [handle]}
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

(ns cwo.views.noir
  (:require [cwo.eval :as evl]
            [cwo.chmgr :as chmgr]
            [cwo.views.enlive :as enlive]
            [noir.session :as session]
            [noir.cookies :as cookies])
  (:use [noir.core  :only (defpage)]))

(defn fmap [m f]
  (into {} (for [[k v] m] [k (f v)])))

;; enlive rendered routes
(defpage "/" []
  (session/put! "sesh-id" (cookies/get "ring-session"))
  (enlive/bootstrap (if-let [handle (session/get "handle")]
                      (enlive/si-content handle)
                      (enlive/default-content))))

(defpage [:post "/login"] {:keys [handle]}
  (if-not (contains? @chmgr/handle->sesh-id handle)
    (do
      (session/put! "handle" handle)
      (chmgr/broadcast)
      (pr-str (fmap (enlive/si-content handle) enlive/render-snippet)))
    ""))

(defpage [:post "/logout"] {:keys [user]}
  (chmgr/end-broadcast)
  (session/remove! "handle")
  (pr-str (fmap (enlive/default-content) enlive/render-snippet)))

;; evaluation route
(defpage [:post "/eval-clj"] {:keys [expr sb]}
  (println expr sb)
  (let [{:keys [expr result error message] :as res} (evl/eval-expr expr (keyword sb))
        data (if error
               res
               (let [[out res] result]
                  (str out (pr-str res))))]
    (pr-str data)))

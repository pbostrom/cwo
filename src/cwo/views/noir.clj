(ns cwo.views.noir
  (:require [cwo.user :as usr]
            [cwo.eval :as evl]
            [cwo.util :as util]
            [cwo.chmgr :as chmgr]
            [cwo.views.enlive :as enlive]
            [noir.session :as session]
            [noir.cookies :as cookies])
  (:use noir.core 
        hiccup.page
        [hiccup.core :only (html)]))

; hiccup rendered routes
(defn others-opts [sel user]
  (conj sel [:option user]))

(defpage "/share-list" []
  (let [user-list (filter #(not= % (usr/get-user)) (keys @cwo.chmgr/channels))]
    (html 
      [:div#user-list
       [:p (reduce
             others-opts
             [:select#others-list {:multiple "multiple"}]
             user-list)]])))

;; enlive rendered routes
(defpage "/" []
  (session/put! "sesh-id" (cookies/get "ring-session"))
  (enlive/bootstrap (if-let [user (usr/get-user)]
                            (enlive/si-content user)
                            (enlive/default-content))))

(defpage [:post "/login"] {:keys [handle]}
  (if-not (contains? @chmgr/handles handle)
    (do
      (session/put! "handle" handle)
      (chmgr/register)
      (pr-str (util/fmap (enlive/si-content handle) enlive/render-snippet)))
    ""))

(defpage [:post "/logout"] {:keys [user]}
  (swap! @chmgr/handles dissoc (session/get "handle"))
  (session/remove! "handle")
  (pr-str (util/fmap (enlive/default-content) enlive/render-snippet)))

;; evaluation route
(defpage [:post "/eval-clj"] {:keys [expr]}
  (let [{:keys [expr result error message] :as res} (evl/eval-request expr)
        data (if error
               res
               (let [[out res] result]
                  (str out (pr-str res))))]
    (println res)
    (println data)
    (pr-str data)))

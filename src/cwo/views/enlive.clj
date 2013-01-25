(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]))

(defn render-snippet [s]
  (apply str (html/emit* s)))

(def pub "cwo/resources/public/")

(html/defsnippet loginbox (str pub "snippets.html") [:#loginbox] [])

(html/defsnippet default-text (str pub "snippets.html") [:#default-text] [])

(html/defsnippet logoutbox (str pub "snippets.html") [:#logoutbox] [user]
  [:#handle] (html/content (:handle user))
  [:#token] (html/set-attr :value (:token user)))

(html/defsnippet signedin-text (str pub "snippets.html") [:#signedin-text] [])

(html/defsnippet home-panel (str pub "layout.html") [:#home-panel] [])

(html/defsnippet status (str pub "snippets.html") [:#status] [])

(html/defsnippet transfer-text (str pub "snippets.html") [:#tr-box] [])

(defn si-content [user]
  {:userbox (logoutbox user)})

(defn login-html [user]
  (render-snippet (logoutbox user)))

(defn logout-html []
  (render-snippet (loginbox)))

(defn default-content []
  {:userbox (loginbox) :text (default-text)})

(html/deftemplate layout (str pub "layout.html")
  [user]
  [:div#user-container] (html/content (if (:handle user) (logoutbox user) (loginbox)))
  [:div#widgets] (html/content (transfer-text)))

(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]))

(defn render-snippet [s]
  (apply str (html/emit* s)))

(defn hfmt [handle]
  (if (= 0 (.indexOf handle "_."))
    (.substring handle 2)
    (str "@" handle)))

(def pub "cwo/resources/public/")

(html/defsnippet loginbox (str pub "snippets.html") [:#loginbox] [])

(html/defsnippet logoutbox (str pub "snippets.html") [:#logoutbox] [user]
  [:#handle] (html/content (hfmt (:handle user)))
  [:#token] (html/set-attr :value (:token user)))

(html/defsnippet transfer-text (str pub "snippets.html") [:#tr-box] [])

(defn login-html [user]
  (render-snippet (logoutbox user)))

(defn logout-html []
  (render-snippet (loginbox)))

(html/deftemplate layout (str pub "layout.html")
  [user]
  [:div#user-container] (html/content (if (:handle user) (logoutbox user) (loginbox)))
  [:div#widgets] (html/content (transfer-text)))

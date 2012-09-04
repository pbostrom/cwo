(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]
            [cwo.models.user :as user]))

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

(defn login-html [hdl]
  (render-snippet (logoutbox hdl)))

(defn default-content []
  {:userbox (loginbox) :text (default-text)})

(html/deftemplate signedin-layout (str pub "layout.html")
  [user]
  [:div#user-container] (html/content (logoutbox user))
  [:div#widgets] (html/content (status) (transfer-text)))
 
(html/deftemplate signedout-layout (str pub "layout.html")
  []
  [:div#user-container] (html/content (loginbox))
  [:div#widgets] (html/content (status) (transfer-text)))

(defn layout [user]
  (if (user/signed-in? user)
    (signedin-layout user)
    (signedout-layout)))
    

(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]
            [cwo.user :as user]))

(defn render-snippet [s]
  (apply str (html/emit* s)))

(def pub "cwo/resources/public/")

(html/defsnippet loginbox (str pub "snippets.html") [:#loginbox] [])

(html/defsnippet default-text (str pub "snippets.html") [:#default-text] [])

(html/defsnippet logoutbox (str pub "snippets.html") [:#logoutbox] [user]
  [:#handle] (html/content (:handle user))
  [:#token] (html/set-attr :value (:token user)))

(html/defsnippet signedin-text (str pub "snippets.html") [:#signedin-text] [])

(html/defsnippet your-panel (str pub "layout.html") [:#your-panel] [])

(html/defsnippet others-repl (str pub "layout.html") [:#others-repl] [])

(html/defsnippet connect-status (str pub "snippets.html") [:#connected] [])

(html/defsnippet transfer-text (str pub "snippets.html") [:#tr-box] [])

(defn si-content [user]
  {:userbox (logoutbox user)})

(defn default-content []
  {:userbox (loginbox) :text (default-text)})

(html/deftemplate signedin-layout (str pub "layout.html")
  [user]
  [:div#user-container] (html/content (logoutbox user))
  [:div#widgets] (html/content (connect-status) (transfer-text) (others-repl)))
 
(html/deftemplate signedout-layout (str pub "layout.html")
  []
  [:div#user-container] (html/content (loginbox))
  [:div#widgets] (html/content (connect-status) (transfer-text) (others-repl)))
 
(html/deftemplate signedout-layout (str pub "layout.html")
  []
  [:div#user-container] (html/content (loginbox))
  [:div#widgets] (html/content (connect-status) (transfer-text) (others-repl)))

(defn layout [user]
  (if (user/signed-in? user)
    (signedin-layout user)
    (signedout-layout)))

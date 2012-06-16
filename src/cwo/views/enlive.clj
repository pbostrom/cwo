(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]))

(defn render-snippet [s]
  (apply str (html/emit* s)))

(html/defsnippet loginbox "cwo/views/snippets.html" [:#loginbox] [])

(html/defsnippet default-text "cwo/views/snippets.html" [:#default-text] [])

(html/defsnippet logoutbox "cwo/views/snippets.html" [:#logoutbox] [username]
  [:span#handle] (html/content username))

(html/defsnippet signedin-text "cwo/views/snippets.html" [:#signedin-text] [])

(defn si-content [username]
  {:userbox (logoutbox username) :text (signedin-text)})

(defn default-content []
  {:userbox (loginbox) :text (default-text)})

(html/deftemplate bootstrap "cwo/views/bootstrap.html"
  [{:keys [userbox text]}]
  [:div#user-container] (html/content userbox)
  [:div#your-text] (html/content text))
    

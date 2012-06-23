(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]))

(defn render-snippet [s]
  (apply str (html/emit* s)))

(def pub "cwo/resources/public/")

(html/defsnippet loginbox (str pub "snippets.html") [:#loginbox] [])

(html/defsnippet default-text (str pub "snippets.html") [:#default-text] [])

(html/defsnippet logoutbox (str pub "snippets.html") [:#logoutbox] [username]
  [:span#handle] (html/content username))

(html/defsnippet signedin-text (str pub "snippets.html") [:#signedin-text] [])


(html/defsnippet 
  connect-status
  (str pub "connected.html") [:#others-tab :.span6.panel] [])

(defn si-content [username]
  {:userbox (logoutbox username) :text (signedin-text)})

(defn default-content []
  {:userbox (loginbox) :text (default-text)})

(html/deftemplate bootstrap (str pub "disconnected.html")
  [{:keys [userbox text]}]
  [:div#user-container] (html/content userbox)
  [:div#your-status] (html/content text)
  [:div#widgets] (html/content (connect-status)))
  

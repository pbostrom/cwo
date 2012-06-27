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

(html/defsnippet statusbox-you (str pub "layout.html") [:#statusbox-you] [])

(html/defsnippet connect-status (str pub "snippets.html") [:#connected] [])

(html/defsnippet transfer-text (str pub "snippets.html") [:#tr-box] [])

(defn si-content [username]
  {:userbox (logoutbox username) :text (signedin-text)})

(defn default-content []
  {:userbox (loginbox) :text (default-text)})

(html/deftemplate signedin-layout (str pub "layout.html")
  [handle]
  [:div#user-container] (html/content (logoutbox handle))
  [:div#widgets] (html/content (connect-status) (transfer-text) (default-text)))
 
(html/deftemplate signedout-layout (str pub "layout.html")
  []
  [:div#user-container] (html/content (loginbox))
  [:div#status-you] (html/content (default-text))
  [:div#widgets] (html/content (connect-status) (transfer-text) (statusbox-you)))

(defn layout [handle]
  (if handle
    (signedin-layout handle)
    (signedout-layout)))

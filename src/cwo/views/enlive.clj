(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]))

(defn render-snippet [s]
  (apply str (html/emit* s)))

(def pub "cwo/resources/public/")

(html/defsnippet loginbox (str pub "snippets.html") [:#loginbox] [])

(html/defsnippet default-text (str pub "snippets.html") [:#default-text] [])

(html/defsnippet logoutbox-gh (str pub "snippets.html") [:#logoutbox] [token]
  [:#handle] (html/content "")
  [:#token] (html/set-attr :value token))

(html/defsnippet logoutbox-default (str pub "snippets.html") [:#logoutbox] [handle]
  [:#handle] (html/content handle))

(html/defsnippet signedin-text (str pub "snippets.html") [:#signedin-text] [])

(html/defsnippet your-panel (str pub "layout.html") [:#your-panel] [])

(html/defsnippet others-repl (str pub "layout.html") [:#others-repl] [])

(html/defsnippet connect-status (str pub "snippets.html") [:#connected] [])

(html/defsnippet transfer-text (str pub "snippets.html") [:#tr-box] [])

(defn si-content [handle]
  {:userbox (logoutbox-default handle)})

(defn default-content []
  {:userbox (loginbox) :text (default-text)})

(html/deftemplate signedin-layout (str pub "layout.html")
  [token]
  [:div#user-container] (html/content (logoutbox-gh token))
  [:div#widgets] (html/content (connect-status) (transfer-text) (others-repl)))
 
(html/deftemplate signedout-layout (str pub "layout.html")
  []
  [:div#user-container] (html/content (loginbox))
  [:div#widgets] (html/content (connect-status) (transfer-text) (others-repl)))

(defn layout [token]
  (if token
    (signedin-layout token)
    (signedout-layout)))
        

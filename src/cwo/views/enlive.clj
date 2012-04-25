(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]))

(defn render-snippet [s]
  (apply str (html/emit* s)))

(html/defsnippet loginbox "cwo/views/snippets.html" [:#loginbox] [])

(html/defsnippet logoutbox "cwo/views/snippets.html" [:#logoutbox] [username]
  [:span.handle] (html/content username))

(html/deftemplate bootstrap "cwo/views/bootstrap.html" [userbox]
  [:div#user-container] (html/content userbox))
;

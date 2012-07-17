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

(html/defsnippet your-panel (str pub "layout.html") [:#your-panel] [])

(html/defsnippet status (str pub "snippets.html") [:#status] [])

(html/defsnippet transfer-text (str pub "snippets.html") [:#tr-box] [])

(html/defsnippet bc-on (str pub "layout.html") [:#bdg-on] [])

(html/defsnippet bc-off (str pub "layout.html") [:#bdg-off] [])


(defn si-content [user]
  {:userbox (logoutbox user)})

(defn default-content []
  {:userbox (loginbox) :text (default-text)})

(defn badge [user state]
  (let [on (bc-on)
        off (bc-off)]
    (if (user/broadcasting? user)
      (state {:visible on  :hidden off})
      (state {:visible off  :hidden on}))))


(html/deftemplate signedin-layout (str pub "layout.html")
  [user]
  [:div#user-container] (html/content (logoutbox user))
  [:span#badge-sp] (html/content (badge user :visible))
  [:div#widgets] (html/content (status) (transfer-text) (badge user :hidden)))
 
(html/deftemplate signedout-layout (str pub "layout.html")
  []
  [:div#user-container] (html/content (loginbox))
  [:div#widgets] (html/content (status) (transfer-text)))

(defn layout [user]
  (if (user/signed-in? user)
    (signedin-layout user)
    (signedout-layout)))
  

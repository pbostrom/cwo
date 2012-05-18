(ns cwo.views.noir
  (:require [cwo.user :as usr]
            [cwo.eval :as evl]
            [cwo.views.enlive :as enlive])
  (:use noir.core 
        hiccup.page
        [hiccup.core :only (html)]))

(defpartial layout [& content]
  (html5 [:head [:title "new page"]
   (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.js")
   (include-css "css/ansi.css" "css/console.css")]

  [:body content]
  (include-js "js/jqconsole-2.7.js"
              "/js/cljs-compiled.js")
  [:script {:type "text/javascript"} "goog.require('myrepl')"]))

(defpartial user-info []
  (if-let [user (usr/get-user)]
    (html [:span (str "Welcome " user)][:button#logout "Logout"])
    (html [:label "Username:"][:input#login-input {:type "text"}][:button#login "Login"])))

(defpage "/" []
  (layout 
    [:div#userbox
     (user-info)]
    [:div#wrapper
     [:div#console.console]]))

(defn linkify [ul user]
  (conj ul [:li user]))

; hiccup rendered routes
(defpage "/share-list" []
  (html [:div#user-list [:p (reduce linkify [:ul] @usr/active-users)]]))

;; enlive rendered routes
(defpage [:post "/login"] {:keys [user]}
  (usr/put-user user)
  (println "user" user "logged in")
  (enlive/render-snippet (enlive/logoutbox user)))

(defpage [:post "/logout"] {:keys [user]}
  (usr/rm-user)
  (println "user" user "logged out")
  (enlive/render-snippet (enlive/loginbox)))

(defpage "/bs" []
  (enlive/bootstrap (if-let [user (usr/get-user)]
                      (enlive/logoutbox user)
                      (enlive/loginbox))))

(defpage "/snip" []
  (enlive/render-snippet (enlive/loginbox)))

;; evaluation route
(defpage [:post "/eval-clj"] {:keys [expr]}
  (let [{:keys [expr result error message] :as res} (evl/eval-request expr)
        data (if error
               res
               (let [[out res] result]
                  (str out (pr-str res))))]
    (println res)
    (println data)
    (pr-str data)))

; test route
(defn some-partial []
  (if-let [user "bare"]
    (html [:input#userinput {:type "text"}] [:button#login "Login"])
    (html [:input#userinput {:type "file"}] [:button#login "Upload"])))
(defpage "/noir-test" []
  (layout
    [:div#wrapper
     (some-partial)]))

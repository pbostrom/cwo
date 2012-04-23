(ns cwo.views.main
  (:require [cwo.user :as usr]
            [cwo.eval :as evl])
  (:use noir.core 
        hiccup.page
        [hiccup.core :only (html)]))

(defpartial layout [& content]
  (html5 [:head [:title "new page"]
   (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.js")
   (include-css "css/ansi.css" "css/console.css")]

  [:body content]
  (include-js "js/jqconsole-2.7.js"
              "/js/bootstrap.js")
  [:script {:type "text/javascript"} "goog.require('myrepl')"]))

(defpartial user-info []
  (if-let [user (usr/get-user)]
    (html [:label user][:button#logout "Logout"])
    (html [:label "Username:"][:input#login-input {:type "text"}][:button#login "Login"])))

(defn some-partial []
  (if-let [user "bar"]
    (html [:input#userinput {:type "text"}] [:button#login "Login"])
    (html [:input#userinput {:type "file"}] [:button#login "Upload"])))

(defpage "/noir-test" []
  (layout
    [:div#wrapper
     (some-partial)]))

  ;check for valid session
  ;else get user info from cookie and add to session

(defpage "/" []
  (layout 
    [:div#userbox
     (user-info)]
    [:div#wrapper
     [:input#text {:type "text"}]
     [:button#disconnect "Disconnect"]
     [:div#console.console]]))

; User managment
(defpage [:post "/login"] {:keys [user]}
  (usr/put-user user)
  (println "user:" user)
  (user-info))

(defpage [:post "/logout"] {:keys [user]}
  (usr/rm-user)
  (println "user:" user)
  (html [:label "Username:"][:input#login-input {:type "text"}][:button#login "Login"]))

(defpage "/shared" []
  (layout 
    [:div#wrapper 
      [:div#user]
      [:input#text {:type "text"}]
      [:button#disconnect "Disconnect"]
      [:div#console2.console]]))

(defpage [:post "/eval-clj"] {:keys [expr]}
  (let [{:keys [expr result error message] :as res} (evl/eval-request expr)
        data (if error
               res
               (let [[out res] result]
                  (str out (pr-str res))))]
    (println res)
    (println data)
    (pr-str data)))


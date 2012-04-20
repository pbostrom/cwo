(ns cwo.views.main
  (:require [noir.session :as session])
  (:use noir.core 
        hiccup.page
        cwo.eval))

(defpartial layout [& content]
  (html5 [:head [:title "new page"]
   (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.js")
   (include-css "css/ansi.css" "css/console.css")]

  [:body content]
  (include-js "js/jqconsole-2.7.js"
              "/js/bootstrap.js")
  [:script {:type "text/javascript"} "goog.require('myrepl')"]))

(defn user-info []
  (if-let [user (session/get "user")]
    [:input#text {:type "text"}]
    [:div#user (user :name)]
    [:div#user [:button#login "Login"]]))

  ;check for valid session
  ;else get user info from cookie and add to session

(defpage "/" []
  (layout 
    [:div#wrapper
     (user-info)
     [:input#text {:type "text"}]
     [:button#disconnect "Disconnect"]
     [:div#console.console]]))

(defpage "/shared" []
  (layout 
    [:div#wrapper 
      [:div#user]
      [:input#text {:type "text"}]
      [:button#disconnect "Disconnect"]
      [:div#console2.console]]))

(defpage [:post "/eval-clj"] {:keys [expr]}
  (let [{:keys [expr result error message] :as res} (eval-request expr)
        data (if error
               res
               (let [[out res] result]
                 {:expr (pr-str expr)
                  :result (str out (pr-str res))}))]
    (println res)
    (println data)
    (pr-str data)))


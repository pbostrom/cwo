(ns cwo.views
  (:use 
    hiccup.core
    hiccup.page)
  (:require [noir.session :as session]))

(defn layout [& content]
  (let [resp-body 
        (html5 [:head [:title "new page"]
          (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.js")
          (include-css "css/ansi.css" "css/console.css")]
;          (include-css "/css/main.css" "/css/tryclojure.css")]
         [:body content]
         (include-js "js/jqconsole-2.7.js"
                     "/js/bootstrap.js")
         [:script {:type "text/javascript"} "goog.require('myrepl')"])]
    (session/put! "idx" 0)
    resp-body))
;    {:status 200
;     :headers {"Content-Type" "text/html"}
;     :body resp-body
;     :session {:idx 0}}))

(def main-view 
  [:div#wrapper [:div#container 
                 [:div#chatLog]
                 [:input#text {:type "text"}]
                 [:button#disconnect "Disconnect"]
                 [:div#console.console]]])

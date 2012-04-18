(ns cwo.views.main
  (:require [noir.session :as session])
  (:use noir.core 
        hiccup.page
        cwo.eval))

(defpartial layout [& content]
  (html5 [:head [:title "new page"]
          (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.js")
          (include-css "css/ansi.css" "css/console.css")]
;          (include-css "/css/main.css" "/css/tryclojure.css")]

         [:body content]
         (include-js "js/jqconsole-2.7.js"
                     "/js/bootstrap.js")
         [:script {:type "text/javascript"} "goog.require('myrepl')"]))

(defn inc-idx [sess]
  (let [idx (get sess "idx")]
    (if idx
      (assoc sess "idx" (inc idx))
      (assoc sess "idx" 0))))

(defpage "/" []
  (session/swap! inc-idx)
  (layout 
    [:div#wrapper 
      [:div#chatLog]
      [:input#text {:type "text"}]
      [:button#disconnect "Disconnect"]
     [:div#console-panel
      [:div#console.console]
      [:div#console2.console]]]))


(defpage [:post "/test-inc"] {:keys [expr]}
  (str expr " " (get (session/swap! inc-idx) "idx")))

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


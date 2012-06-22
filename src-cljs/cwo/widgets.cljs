(ns cwo.widgets
  (:require [crate.core :as crate]))

(defn connect-panel [owner]
  (crate/html
    [:div.span6.panel
     [:div.row
      [:div#status-box.span3
       [:table.status.table.table-condensed
        [:tbody
         [:tr [:td "Owner:"] [:td owner ]]
         [:tr [:td "Last Activity:"][:td#last-act "17:14 10-Jun-12"]]]]
       [:button#discon.btn.btn-small {:handle owner} [:i.icon-off]" Disconnect"]]
      [:div#peers.list-box.span3 [:p "Connected users:"] [:select {:multiple "multiple"}]]]
     [:div.chat [:pre] [:input {:placeholder "chat" :type "text"}]]]))

(defn others-list []
  (crate/html 
    [:div#others-box.list-box.span3.offset1
     [:p "Select a REPL session from the list."]
     [:select#others-list {:multiple "multiple"}]
     [:button#connect.btn "Connect"]]))

(ns cwo.widgets
  (:require [crate.core :as crate]))

(defn connect-panel [owner]
  (crate/html
    [:div.span6 
     [:div.row
      [:div.span3
       [:span " Connected"
        [:button#discon.btn.btn-small {:handle handle} [:i.icon-off]" Disconnect"]]
       [:table.status.table.table-condensed
        [:tbody
         [:tr [:td "Owner"]
          [:td owner 
           ]]
         [:tr [:td "Last Activity"][:td#last-act]]
         [:tr [:td#prmpt {:colspan "2"} "Prompt assigned to"]]]]]
      [:div#peers.list-box.span3 [:select {:multiple "multiple"}]]]
     [:div.chat [:pre] [:input {:placeholder "chat" :type "text"}]]]))

(defn discon-status []
  (crate/html [:span " Not connected"] [:p "Select a REPL session from the list."]))

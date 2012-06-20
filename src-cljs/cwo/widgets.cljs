(ns cwo.widgets
  (:require [crate.core :as crate]))

(defn oth-status [owner]
  (crate/html
    [:table.status.table.table-condensed
     [:tr 
      [:td "Owner"]
      [:td owner [:button#discon.btn.btn-small {:handle handle} [:i.icon-off]" Disconnect"]]]
     [:tr [:td "Last Activity"][:td#last-act]]
     [:tr [:td#prmpt {:colspan "2"} "Prompt assigned to"]]]))

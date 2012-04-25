(ns cwo.views.enlive
  (:require [net.cgrand.enlive-html :as html]))

(html/deftemplate bootstrap "cwo/views/bootstrap.html" [content])
;  [:div#content] (html/content (str "Content goes here:" content)))
;

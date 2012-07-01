(ns cwo.chat
  (:use [cwo.utils :only [jq]]))

(defn chat-listner [e]
  (when (= (.-which e) 13)
    (this-as ta
             (js/alert (.val (jq ta))))))

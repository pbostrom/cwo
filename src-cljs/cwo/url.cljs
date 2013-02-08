(ns cwo.url
  (:require [snout.core :as snout])
  (:use-macros [snout.macros :only [defroute]]))

(defn get-url []
  (snout/get-token))

(defroute "/paste/rh/:id" [id]
  (js/alert (str "refheap" id)))
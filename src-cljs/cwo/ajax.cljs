(ns cwo.ajax
  (:require [cljs.reader :as reader])
  (:use [cwo.utils :only (make-js-map)]))

(def jq js/jQuery)

(defn eval-clojure [code]
  (let [data (atom "")]
    (.ajax jq (make-js-map {:url "/eval-clj"
                            :type "POST"
                            :data (make-js-map {:expr code})
                            :async false
                            :success (fn [res] (reset! data res))}))
    (:result (reader/read-string @data))))

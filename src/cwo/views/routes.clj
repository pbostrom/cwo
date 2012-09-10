(ns cwo.views.routes
  (:require [compojure.core :refer [defroutes GET]]
            [cwo.views.enlive :as enlive]))

(defroutes root-dbg
  (GET "/" {:keys [code]}
       (enlive/layout nil))
  (GET "/foo" {:keys [code]}
       "<!DOCTYPE html><html> Foobar </html>"))

(defroutes root-dbg2
  (GET "/foo" {:keys [code]}
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body "<!DOCTYPE html><html> Foobarz </html>"}))

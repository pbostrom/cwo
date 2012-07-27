(ns cwo.test.server
  (:require [midje.sweet :refer [fact]]
            [cwo.server :as server]
            [cwo.chmgr :as chmgr]))

(fact "socket handler" (fn? (server/get-handler)) => true)

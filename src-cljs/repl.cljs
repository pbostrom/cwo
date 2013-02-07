(ns meetup-cljs.repl
   (:require [clojure.browser.repl :as repl]))
; Use of "localhost" will only work for local development.
; Change the port to match the :repl-listen-port.
(repl/connect "http://localhost:9000/repl")

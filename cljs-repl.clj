(ns cljs-repl
  (:require [cljs.repl :as repl]
            [cljs.repl.browser :as browser]))

(repl/repl (browser/repl-env))

(require 'cljs.repl.browser)
(require 'cemerick.piggieback)

(cemerick.piggieback/cljs-repl
  :repl-env (doto (cljs.repl.browser/repl-env :port 9000)
              cljs.repl/-setup))
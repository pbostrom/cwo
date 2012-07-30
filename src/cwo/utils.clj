(ns cwo.utils)


; avoid eval injections via websocket msgs
(defn safe-read-str
 "Prevent evals when reading an untrusted string "
 [st]
  (binding [*read-eval* false] (read-string st)))

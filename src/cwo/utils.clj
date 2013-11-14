(ns cwo.utils)

; avoid eval injections via websocket msgs
(defn safe-read-str
  "Prevent evals when reading an untrusted string "
  [st]
  (binding [*read-eval* false] (read-string st)))

; avoid eval injections via websocket msgs
(defn safe-read
  "Prevent evals when reading an untrusted source "
  [pbr eof-error? eof-val]
  (binding [*read-eval* false] (read pbr eof-error? eof-val)))

(defn read-forms
  "Returns a vector of forms read from string"
  [str]
  (let [end :dootdootalootdoot]
    (loop [pbr (java.io.PushbackReader. (java.io.StringReader. str))
           nxt (safe-read pbr false end)
           forms []]
      (if (= nxt end)
        forms
        (recur pbr (safe-read pbr false end) (conj forms nxt))))))
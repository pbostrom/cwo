(ns cwo.share
  (:require [crate.core :as crate])
  (:use [cwo.utils :only (jq jslog new-socket)]))

(def main-socket (atom nil))
(def ro-socket (atom nil))

(defn send-console []
  (let [console-nodes (-> (jq "#your-repl .jqconsole-header ~ span") (.clone))
        console-html (-> (jq "<div>") (.append console-nodes) (.remove) (.html))]
    (.send @main-socket (str console-html))))

(defn share-console-loop []
  (send-console)
  (js/setTimeout share-console-loop 1900))

(defn socket-ready []
  (-> (jq "#debug-box")
    (.append
      (crate/html [:p.event "Socket Status: " + 
                   (str (.-readyState @main-socket)) + " (open) " [:div#in]])))
  (share-console-loop))

(defn share-repl []
  (let [handle (-> (jq "#handle") (.text))];TODO: verify login session
    (reset! main-socket (new-socket handle))
    (.text (jq "#share") "Unshare")
    (set! (.-onopen @main-socket) socket-ready)))

(defn unshare-repl []
  (js/alert "Unshare!"))

(defn connect [user]
  (let [socket (new-socket user)]
    (-> (jq "#other-console") (.jqconsole (str user "'s REPL\n") "=> " " "))
    (set! (.-onopen socket) #(-> (jq "#debug-box") (.append "Socket Ready")))
    (set! (.-onerror socket) #(-> (jq "#debug-box") (.append "Socket fubar")))
    (set! (.-onmessage socket)
          (fn [msg]
            ;          (jslog (.-data msg))
            (-> (jq "#other-console .jqconsole-header ~ span")
              (.remove))
            (-> (jq (.-data msg))
              (.insertAfter (jq "#other-console .jqconsole-header")))))))

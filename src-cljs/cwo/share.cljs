(ns cwo.share
  (:require [crate.core :as crate])
  (:use [cwo.utils :only (jq jslog)]))

(def main-socket (atom nil))

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
    (set! (.-onerror @main-socket) (fn [evt] (-> (jq "#debug-box")
                                               (.append
                                                 (crate/html [:p.event "Error: " + evt.data])))))
    (set! (.-onopen @main-socket) socket-ready)))

(defn unshare-repl []
  (.close @main-socket))

(defn connect [user]
  (let [socket @main-socket]
    (set! (.-onopen socket) #(-> (jq "#debug-box") (.append "Socket Ready")))
    (set! (.-onerror socket) #(-> (jq "#debug-box") (.append "Socket fubar")))
    (set! (.-onclose socket) #(-> (jq "#debug-box") (.append "Socket Closed")))
    (set! (.-onmessage socket)
          (fn [msg]
            (if (= ":close" msg.data)
              (js/alert msg.data)
              (do 
                (-> (jq "#other-repl .jqconsole-header ~ span")
                  (.remove))
                (-> (jq (.-data msg))
                  (.insertAfter (jq "#other-repl .jqconsole-header")))))))))

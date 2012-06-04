(ns cwo.socket
  (:require [crate.core :as crate])
  (:use [cwo.utils :only (jq jslog)]))

(def sock (atom nil))
(def publish-console? (atom true))

(defn send-console []
  (let [console-nodes (-> (jq "#your-repl .jqconsole-header ~ span") (.clone))
        console-html (-> (jq "<div>") (.append console-nodes) (.remove) (.html))]
    (.send @sock (str console-html))))

(defn share-console-loop []
  (when @publish-console?
    (send-console)
    (js/setTimeout share-console-loop 1900)))

(defn connect [handle]
  (.send @sock (pr-str [:connect handle])))

(defn transfer [handle]
  (reset! publish-console? false)
  (.send @sock (pr-str [:transfer handle])))

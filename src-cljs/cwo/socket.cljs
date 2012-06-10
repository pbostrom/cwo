(ns cwo.socket
  (:use [cwo.utils :only (jq jslog)]))

(def sock (atom nil))
(def publish-console? (atom true))

(defn send-console []
  (let [console-nodes (-> (jq "#your-repl .jqconsole-header ~ span") (.clone))
        console-html (-> (jq "<div>") (.append console-nodes) (.remove) (.html))]
    (.send @sock console-html)))

(defn send-alt-console []
  (let [console-nodes (-> (jq "#others-repl .jqconsole-header ~ span") (.clone))
        console-html (-> (jq "<div>") (.append console-nodes) (.remove) (.html))
        alt-str (pr-str {:alt console-html})]
    (.send @sock alt-str)))

(defn share-console-loop []
  (when @publish-console?
    (send-console)
    (js/setTimeout share-console-loop 1900)))

(defn share-alt-console-loop []
  (send-alt-console)
  (js/setTimeout share-alt-console-loop 1900))

(defn connect [handle]
  (.send @sock (pr-str [:connect handle])))

(defn transfer [handle]
  (reset! publish-console? false)
  (.send @sock (pr-str [:transfer handle])))

(ns cwo.socket
  (:use [cwo.utils :only (jq jslog)]))

(def sock (atom nil))
(def publish-console? (atom true))
(def eval-result (atom nil))

(defn send-console-old []
  (let [console-nodes (-> (jq "#your-repl .jqconsole-header ~ span") (.clone))
        console-html (-> (jq "<div>") (.append console-nodes) (.remove) (.html))]
    (.send @sock console-html)))

(defn send-alt-console []
  (let [console-nodes (-> (jq "#others-repl .jqconsole-header ~ span") (.clone))
        console-html (-> (jq "<div>") (.append console-nodes) (.remove) (.html))
        alt-str (pr-str {:alt console-html})]
    (.send @sock alt-str)))

(defn share-alt-console-loop []
  (send-alt-console)
  (js/setTimeout share-alt-console-loop 1900))

(defn eval-wait []
  (when-not eval-result ;TODO: nil could be a valid result!
    (js/setTimeout eval-wait 100)))

(defn eval-clj [expr sb]
  (.send @sock (pr-str [:eval-clj [expr sb]]))
  (eval-wait)
  (let [res @eval-result]
    (reset! eval-result nil)
    res))

(defn subscribe [handle]
  (.send @sock (pr-str [:subscribe handle])))

(defn transfer [handle]
  (reset! publish-console? false)
  (.send @sock (pr-str [:transfer handle])))

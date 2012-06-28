(ns cwo.repl
  (:use [cwo.utils :only (jq jslog sock srv-cmd)])
  (:require [crate.core :as crate]
            [cwo.widgets :as widgets]))

(def publish-console? (atom {:you true :oth false}))

(def repls
  {:oth (-> (jq "#others-repl") (.jqconsole "" "=> " " "))
   :you (-> (jq "#your-repl") (.jqconsole "Your Clojure REPL\n" "=> " " "))})

(defn send-prompt [console-kw]
  (let [repl (repls console-kw)
        prompt-msg (console-kw {:you (fn [m] {:p m})
                                :oth (fn [m] {:t {:p m}})})]
    (when-let [prompt-text (and (= (.GetState repl) "prompt")(.GetPromptText repl))]
      (.send @sock (pr-str (prompt-msg prompt-text))))))

(defn share-console-loop [console-kw]
  (when (@publish-console? console-kw)
    (send-prompt console-kw)
    (js/setTimeout #(share-console-loop console-kw) 1900)))

(defn paren-match? [expr]
  (>=
    (count (filter #(= % ")") expr))
    (count (filter #(= % "(") expr))))

(defn expr-indent [expr]
  (let [lines (js->clj (.split expr "\n"))
        line (.trim jq (last lines))
        offset (if (= (count lines) 1) 2 0)
        indent-vec (reduce 
                     (fn [v x]
                       (let [idx (first v)
                             stack (second v)]
                         (cond 
                           (= x "(") [(inc idx) (cons idx stack)]
                           (= x ")") [(inc idx) (rest stack)]
                           true [(inc idx) stack]))) 
                     [0 []] (seq line))
        indent-val (+ (first (second indent-vec)) 2 offset)]
    indent-val))

(defn prompt [repl]
  (let [handler #(srv-cmd :eval-clj [% repl])]
    (.Prompt (repl repls) true handler (fn [expr]
                                 (if (paren-match? expr)
                                   false
                                   (expr-indent expr))))))

(defn console-write [repl output]
  (if (:error output)
    (.Write (repl repls) (str (:message output) "\n") "jqconsole-error")
    (.Write (repl repls) (str output "\n") "jqconsole-output"))
  (prompt repl))

(defn init-active-mode [repl-kw]
  (let [repl (repl-kw repls)]
    (when (= (.GetState repl) "prompt") (.AbortPrompt repl))
    (.Enable repl)
    (.SetIndentWidth repl 1)
    (prompt repl-kw))
  (swap! publish-console? assoc repl-kw true)
  (share-console-loop repl-kw))

(defn init-sub-mode [repl-kw]
  (swap! publish-console? assoc repl-kw false)
  (let [repl (repl-kw repls)]
    (.Reset repl)
    (.Prompt repl true (fn [] nil))
    (.Disable repl)))

(defn set-repl-mode [repl mode]
  (let [modef (mode {:active init-active-mode :sub init-sub-mode})]
    (modef repl)))

(defn connect []
  (set-repl-mode :oth :sub)
  (let [handle (-> (jq "#others-list option:selected") (.val))]
    (.append (jq "#widgets") (jq "#disconnected"))
    (.text (jq "#owner") handle)
    (.attr (jq "#discon") "handle" handle)
    (.append (jq "#others-tab > .row") (jq "#connected"))
    (srv-cmd :subscribe handle)))

(defn disconnect []
  (set-repl-mode :oth :sub)
  (.append (jq "#widgets") (jq "#connected"))
  (.append (jq "#others-tab > .row") (jq "#disconnected"))
  (this-as btn (let [handle (-> (jq btn) (.attr "handle"))]
                 (srv-cmd :disconnect handle))))
  
(defn transfer []
  (let [handle (-> (jq "#sub-list option:selected") (.val))]
    ; convert console to subscribe mode
    (set-repl-mode :you :sub)
    ; configure transfer on server
    (srv-cmd :transfer handle)
    (.text (jq "#tr-hdl") handle)
    (.attr (jq "#reclaim") "handle" handle)
    (.append (jq "#your-status") (jq "#tr-box"))))

(defn reclaim []
  (this-as btn 
           (let [handle (-> (jq btn) (.attr "handle"))]
             (.append (jq "widgets") (jq "#tr-box"))
             (jslog "srv-cmd :reclaim")
             (srv-cmd :reclaim handle))))

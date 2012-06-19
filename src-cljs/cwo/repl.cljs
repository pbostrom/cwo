(ns cwo.repl
  (:use [cwo.utils :only (jq jslog sock srv-cmd)])
  (:require [crate.core :as crate]))

(def publish-console? (atom true))

(def repls
  {:oth (-> (jq "#others-repl") (.jqconsole "Not connected\n" "=> " " "))
   :you (-> (jq "#your-repl") (.jqconsole "Your Clojure REPL\n" "=> " " "))})

(defn send-prompt []
  (let [repl (repls :you)]
    (when-let [prompt-text (and (= (.GetState repl) "prompt")(.GetPromptText repl))]
      (.send @sock (pr-str {:p prompt-text})))))

(defn share-console-loop []
  (when @publish-console?
    (send-prompt)
    (js/setTimeout share-console-loop 1900)))

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
    (prompt repl-kw)))

(defn init-sub-mode [repl-kw]
  (let [repl (repl-kw repls)]
    (.Reset repl)
    (.Prompt repl true (fn [] nil))
    (.Disable repl)))

(defn set-repl-mode [repl mode]
  (let [modef (mode {:active init-active-mode :sub init-sub-mode})]
    (modef repl)))

(defn subscribe []
  (set-repl-mode :oth :sub)
  (let [handle (-> (jq "#others-list option:selected") (.val))
        header (jq "#others-repl .jqconsole-header")
        new-hdr [:div.jqconsole-header (str handle "'s REPL")
                 [:button#discon.btn.btn-small {:handle handle} [:i.icon-off]" Disconnect"]]]
    (.replaceWith header (crate/html new-hdr))
    (srv-cmd :subscribe handle)))

(defn disconnect []
  (this-as btn (let [handle (-> (jq btn) (.attr "handle"))]
                 (.remove (jq btn))
                 (srv-cmd :disconnect handle)))
  (let [header (jq "#others-repl .jqconsole-header")]
    (set-repl-mode :oth :sub)
    (.text header "Not connected\n")))

(defn transfer []
  (reset! publish-console? false)
  (let [handle (-> (jq "#sub-list option:selected") (.val))
        header (jq "#your-repl .jqconsole-header")
        new-hdr [:div.jqconsole-header "Your REPL"
                 [:button#reclaim.btn.btn-small [:img {:src "img/grab.png"}]" Reclaim"]]]
    (srv-cmd :transfer handle)
    (.replaceWith header (crate/html new-hdr))))

(defn reclaim []
  (reset! publish-console? true)
  (this-as btn 
           (.remove (jq btn))
           (srv-cmd :reclaim)))

(ns cwo.repl
  (:require [cwo.utils :refer [jq jslog qry-list select-set sock srv-cmd hfmt]]
            [clojure.string :as string]))

(def publish-console? (atom {:you true :oth false}))

(def repls
  {:oth (-> (jq "#others-repl") (.jqconsole "Peer REPL sessions are displayed in the console below.\n" "=> " " "))
   :you (-> (jq "#your-repl") (.jqconsole "Enter your Clojure code at the prompt.\n" "=> " " "))})

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
                     (fn [[idx stack] x]
                       (cond 
                         (= x "(") [(inc idx) (cons idx stack)]
                         (= x ")") [(inc idx) (or (next stack) [(- (first stack) 2)])]
                         true [(inc idx) stack])) 
                     [0 [-2]] (seq line))]
    (+ (first (second indent-vec)) 2 offset)))

(declare prompt)
(defn eval-hdlr [expr repl]
  (if-not (empty? (.trim expr)) 
    (srv-cmd :read-eval-clj [expr repl])
    (prompt repl)))

(defn prompt [repl]
  (.Prompt (repl repls) true #(eval-hdlr % repl) 
           (fn [expr]
             (if (paren-match? expr)
               false
               (expr-indent expr)))))

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

(defn login []
  (srv-cmd :login (.val (jq "#login-input"))))

(defn logout []
  (srv-cmd :logout nil))

(defn join
  ([_]
     (join (-> (jq "#others-list option:selected") (.val)) nil))
  ([handle _]
     (-> (jq "#repl-tabs a[href=\"#peer\"]") (.tab "show"))
     (-> (jq "#peer-status") (.css "visibility" "visible"))
     (set-repl-mode :oth :sub)
     (.text (jq "#owner") (hfmt handle))
     (.attr (jq "#discon") "handle" handle)
     (srv-cmd :subscribe handle)))

(defn disconnect []
  ;  (set-repl-mode :oth :sub)
  (-> (jq "#peer-status") (.css "visibility" "hidden"))
  (.html (jq "#peer-chat-box pre") nil)
  (.html (jq "#peer-list") nil)
  (let [handle (-> (jq "#discon") (.attr "handle"))]
    (srv-cmd :disconnect handle)))

(defn transfer []
  (let [handle (-> (jq "#home-peer-list option:selected") (.val))]
    ; convert console to subscribe mode
    (set-repl-mode :you :sub)
    ; configure transfer on server
    (srv-cmd :transfer handle)))

(defn reclaim [e]
  (let [btn (.-target e)] 
    (let [handle (-> (jq btn) (.attr "handle"))]
      (.append (jq "#widgets") (jq "#tr-box"))
      (srv-cmd :reclaim handle))))

(defn hash-connect [[hdl]]
  (let [handles (select-set (jq "#others-list > option"))]
    (if (contains? handles hdl)
      (join hdl nil)
      (js/alert (str hdl " is not available")))))

(defn process-hash
  "Process hash string of url"
  [hsh]
  (let [hshvec (string/split hsh "/")
        action (first hshvec)
        args (vec (rest hshvec))]
    (cond
     (= "paste" action) (srv-cmd :paste (conj args :you))
     (= "connect" action) (hash-connect args))))

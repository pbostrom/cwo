(ns cwo.repl
  (:require [cwo.utils :refer [jq jslog qry-list select-set sock srv-cmd hfmt]]
            [clojure.string :as string]))

(def publish-console? (atom {:you true :oth false}))

(def repls
  {:oth (-> (jq "#others-repl") (identity))
   :you (-> (jq "#your-repl") (identity))})

(defn send-editor-state [console-kw state]
  (let [prompt-msg (console-kw {:you {:p state}
                                :oth {:t {:p state}}})]
    (.send @sock (pr-str prompt-msg))))

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

(defn do-eval [expr]
  (if-not (empty? (.trim expr)) 
    (srv-cmd :read-eval-clj [expr :you])))

(defn expr-contains-curs? [{:keys [from to]} curs]
  (apply <= (map :line [from curs to])))

(defn find-tl-expr
  "finds top level expression containing cursor"
  [cm line]
  (if (< line 0)
    nil
    (if-let [match (.findMatchingBracket cm (clj->js {:line line :ch 0}))]
      (when (expr-contains-curs? match (.getCursor cm))
        match)
      (recur cm (dec line)))))

(defn editor-eval [editor evt] ;;TODO: make js objects behave like maps
  (.preventDefault evt)
  (.stopPropagation evt)
  (if-let [{:keys [from to]} (find-tl-expr editor (.-line (.getCursor editor)))]
    (do
      (aset to "ch" (inc (:ch to)))
      (do-eval (.getRange editor from to)))
    (js/alert "no tl expr")))

(defn editor-load [editor evt]
  (.preventDefault evt)
  (.stopPropagation evt)
  (.html (jq "#you-repl-output") "")
  (let [exprs (.getValue editor)]
    (srv-cmd :read-eval-clj [exprs :you])))

(defn prompt [repl]
  (.Prompt (repl repls) true #(eval-hdlr % repl) 
           (fn [expr]
             (if (paren-match? expr)
               false
               (expr-indent expr)))))

(defn console-write [repl output]
  (if (:error output)
    (.Write (repl repls) (str (:message output) "\n") "jqconsole-error")
    (.append (jq "#you-repl-output") (str output "<br>"))))

(defn init-active-mode [repl-kw]
  (let [repl (repl-kw repls)]
    (when (= (.GetState repl) "prompt") (.AbortPrompt repl))
    (.Enable repl)
    (.SetIndentWidth repl 1)
    (prompt repl-kw))
  (swap! publish-console? assoc repl-kw true)
  (share-console-loop repl-kw))

(defn init-sub-mode [repl-kw]
  (swap! publish-console? assoc repl-kw false))

(defn set-repl-mode [repl mode]
  (let [modef (mode {:active init-active-mode :sub init-sub-mode})]
    (modef repl)))

(defn login []
  (srv-cmd :login (.val (jq "#login-input"))))

(defn logout []
  (srv-cmd :logout nil))

(defn join
  ([_ cm]
     (join (-> (jq "#others-list option:selected") (.val)) cm nil))
  ([handle cm _]
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

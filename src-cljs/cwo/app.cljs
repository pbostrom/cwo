(ns cwo.app
  (:use [cwo.utils :only (socket jq make-js-map clj->js jslog)])
  (:require [cwo.ajax :as ajax]
            [cwo.share :as share]
            [cwo.repl :as repl]))

; init repl
(repl/init)

; navigation
(defn nav-handler []
  (let [hsh js/window.location.hash]
    (cond
      (= hsh "#others") (do 
                          (-> (jq "#your-container") (.hide))
                          (-> (jq "#other-container")(.show))
                          (ajax/share-list))
      (empty? hsh) (do 
                     (-> (jq "#your-container")(.show))
                     (-> (jq "#other-container")(.hide))))))

(set! (.-onpopstate js/window) nav-handler)

; button listeners
; use body to register for ajax deferred event listeners
(def body (jq "body"))
; hacky way to prevent muli-selects
(-> body
  (.on "click" "#share-list" (fn [evt] 
                               (-> (jq "#share-list option:selected") (.removeAttr "selected"))
                               (-> (jq evt.target) (.attr "selected" "selected")))))

; connect button
(-> body
  (.on "click" "#connect" (fn [] (share/connect (-> (jq "#share-list option:selected") (.val))))))

; share button
(-> body
  (.on "click" "#share" (fn []
                          (if (= (.text (jq "#share")) "Share")
                            (share/share-repl)
                            (share/unshare-repl))))) 

; login/out buttons 
(-> body
  (.on "click" "#login" (fn [] (ajax/login (.val (jq "#login-input"))))))

(-> body
  (.on "click" "#logout" (fn [] (ajax/logout))))

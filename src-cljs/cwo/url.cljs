(ns cwo.url)

(defn hash-hdlr [evt]
  (js/alert (.-hash js/location)))

(set! (.-onhashchange js/window) hash-hdlr)

(defmulti route
  (fn []))
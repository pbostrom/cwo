(ns cwo.utils)

(def jq js/jQuery)
(def ws-url (str "ws://" js/window.location.host "/socket"))
(def sock (atom nil))

(defn srv-cmd
  "Send a command to be executed on the server in the
   form of [:command arg]."
  [cmd arg]
  (.send @sock (pr-str [cmd arg])))
 
(defn get-hash
  "Get the hash value of the url if it exists"
  []
  (let [hsh (.-hash js/window.location)]
    (when-not (empty? hsh) (.substring hsh 1))))

(defn others-set
  "Sorted set of all signed in users"
  []
  (into (sorted-set) (map #(.-value %) 
                          (js->clj (.makeArray jq (jq "#others-list option"))))))

(defn clj->js
  "Recursively transforms ClojureScript maps into Javascript objects,
   other ClojureScript colls into JavaScript arrays, and ClojureScript
   keywords into JavaScript strings."
  [x]
  (cond
    (string? x) x
    (keyword? x) (name x)
    (map? x) (.strobj (reduce (fn [m [k v]]
               (assoc m (clj->js k) (clj->js v))) {} x))
    (coll? x) (apply array (map clj->js x))
    :else x))

(defn map->js [m]
  (let [out (js-obj)]
    (doseq [[k v] m]
      (aset out (name k) v))
    out))

(defn make-js-map
  "makes a javascript map from a clojure one"
  [cljmap]
  (let [out (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) cljmap))
    out))

(defn jslog [out]
  (.log js/console out))

(ns cwo.utils)

(def jq js/jQuery)
(def ws-url (str "ws://" js/window.location.host "/socket"))
(def sock (atom nil))
(def ghcode (atom nil))

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

(defn select-set
  "Returns sorted set of all handles in the select list"
  [sel-list]
  (into (sorted-set) (for [o (js->clj 
                               (.makeArray jq (jq (str sel-list " option"))))]
                       (.-value o))))

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

(defn jslog [out]
  (.log js/console out))

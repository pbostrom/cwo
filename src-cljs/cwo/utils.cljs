(ns cwo.utils)

(def jq js/jQuery)
(def ws-url (str "ws://" js/window.location.host "/socket"))
(def sock (atom nil))
(def ghcode (atom nil))

(defn srv-cmd
  "Send a command to be executed on the server in the
   form of [:command arg]."
  [cmd arg]
;  (if (not (.-ready @sock))) ; TODO: make sure socket is open
  (.send @sock (pr-str [cmd arg])))
 
(defn get-hash
  "Get the hash value of the url if it exists"
  []
  (let [hsh (.-hash js/window.location)]
    (when-not (empty? hsh) (.substring hsh 1))))

(defn select-set
  "Returns sorted set of all handles in the select list"
  [list-opts]
  (into (sorted-set) (for [o (js->clj 
                               (.makeArray jq list-opts))]
                       (.-value o))))

(defn map->js [m]
  (let [out (js-obj)]
    (doseq [[k v] m]
      (aset out (name k) v))
    out))

(defn jslog [out]
  (.log js/console out))

(defn re-html [sel html]
  (-> (jq sel)
    (.html html)))

(defn jq-ajax [url succ fail]
  (.ajax jq (clj->js {:url url :dataType "jsonp" :success succ :error fail})))
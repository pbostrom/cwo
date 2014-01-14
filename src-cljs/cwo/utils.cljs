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

(defn- qry-list [list-id opt-val]
  (-> (jq (str list-id " > option"))
    (.filter (fn [idx] (this-as opt (= (.val (jq opt)) opt-val))))))

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

(defn hfmt [handle]
  (when handle
    (if (= 0 (.indexOf handle "_."))
      (.substr handle 2)
      (str "@" handle))))

(defn sel-opt [handle]
  [:option {:value handle} (hfmt handle)])

(defn strkey
  "Helper fn that converts keywords into strings"
  [x]
  (if (keyword? x)
    (name x)
    x))

(extend-type object
  ILookup
  (-lookup
    ([o k]
       (aget o (strkey k)))
    ([o k not-found]
       (let [s (strkey k)]
         (if (goog.object.containsKey o s)
           (aget o s)
           not-found)))))

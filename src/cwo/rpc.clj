(ns cwo.rpc
  (:use [cwo.eval :only (eval-request)]))

(defn eval-clj-new [expr]
  (let [{:keys [expr result error message] :as res} (eval-request expr)
        data (if error
               res
               (let [[out res] result]
                 {:expr (pr-str expr)
                  :result (str out (pr-str res))}))]
    data))

(defn eval-clj [expr session]
  (let [idx (inc (:idx session))]
    (println (str expr " " idx))
    {:body expr
     :session {:idx idx}}))

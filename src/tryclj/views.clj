(ns tryclj.views
  (:use [tryclj.eval :only (eval-request)]))

(defn eval-view [expr]
  (let [{:keys [expr result error message] :as res} (eval-request expr)
        data (if error
               res
               (let [[out res] result]
                 {:expr (pr-str expr)
                  :result (str out (pr-str res))}))]))

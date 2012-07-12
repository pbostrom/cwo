(ns cwo.user)

(defn signed-in? [user]
  (or ((comp not nil?) (:token user))((comp not nil?) (:handle user))))

(defn broadcasting? [user]
  (= (:bc user) true))

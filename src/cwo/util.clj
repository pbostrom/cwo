(ns cwo.util)

(defn fmap [m f]
  (into {} (for [[k v] m] [k (f v)])))

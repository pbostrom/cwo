(ns cwo.config)

(def cfg
  {:access-url "https://github.com/login/oauth/access_token"
   :auth-base "https://github.com/login/oauth/authorize"
   :client_id "462bb4a4d01b06852938"
   :client_secret "66328ded3b96e566a883a44fa9036c04b30e9169" })

(defn auth-url []
  (str (:auth-base cfg) "?client_id=" (:client_id cfg)))

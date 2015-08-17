(ns stonecutter.util.gen-key-pair
  (:require [stonecutter.jwt :as jwt]))

(defn -main [& args]
  (let [key-id (first args)
        key-pair (jwt/generate-rsa-key-pair key-id)]
    (println (jwt/key-pair->json key-pair))))

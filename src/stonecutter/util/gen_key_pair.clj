(ns stonecutter.util.gen-key-pair
  (:require [stonecutter.jwt :as jwt]))

(defn -main [& args]
  (let [key-id (first args)
        key-pair (jwt/generate-rsa-key-pair key-id)]
    (println)
    (println)
    (println "JWK public key for client:")
    (println "==========================")
    (println (jwt/key-pair->json key-pair))
    (println)
    (println)
    (println "JWK including private key for stonecutter:")
    (println "==========================================")
    (println (jwt/key-pair->json key-pair :include-private-key))))

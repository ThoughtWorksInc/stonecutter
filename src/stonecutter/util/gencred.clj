(ns stonecutter.util.gencred
  (:require [crypto.random :as random]))

(defn -main
  [& args]
  (let [client-id (random/base32 20)
        client-secret (random/base32 20)]
    (prn :client-id client-id
         :client-secret client-secret)))
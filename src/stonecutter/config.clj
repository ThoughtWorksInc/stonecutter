(ns stonecutter.config
  (:require [environ.core :as env]
            [clojure.tools.logging :as log]))

(defn port []
  (Integer. (get env/env :port "3000")))

(defn host []
  (get env/env :host "127.0.0.1"))

(defn- get-docker-mongo-uri []
  (when-let [mongo-ip (get env/env :mongo-port-27017-tcp-addr)]
    (format "mongodb://%s:27017/stonecutter" mongo-ip)))

(defn mongo-uri []
  (or
    (get-docker-mongo-uri)
    (get env/env :mongo-uri)
    "mongodb://localhost:27017/stonecutter"))

(defn client-credentials-file-path []
  (get env/env :client-credentials-file-path "client-credentials.yml"))

(defn theme []
  (get env/env :theme))

(defn secure?
  "Returns true unless 'secure' environment variable set to 'false'"
  ([]
   (secure? env/env))
  ([env-map]
   (not (= "false" (get env-map :secure "true")))))

(defn email-script-path []
  (if-let [script-path (get env/env :email-script-path)]
    script-path
    (log/warn "No email script path provided - Stonecutter will be unable to send emails")))

(ns stonecutter.config
  (:require [environ.core :as env]
            [clojure.tools.logging :as log]))

(defn create-config []
  (select-keys env/env [:port :host :mongo-port-27017-tcp-addr
                        :mongo-uri :client-credentials-file-path
                        :theme :secure :email-script-path]))

(defn port [config-m]
  (Integer. (get config-m :port "3000")))

(defn host [config-m]
  (get config-m :host "127.0.0.1"))

(defn- get-docker-mongo-uri [config-m]
  (when-let [mongo-ip (:mongo-port-27017-tcp-addr config-m)]
    (format "mongodb://%s:27017/stonecutter" mongo-ip)))

(defn mongo-uri [config-m]
  (or
    (get-docker-mongo-uri config-m)
    (get config-m :mongo-uri)
    "mongodb://localhost:27017/stonecutter"))

(defn client-credentials-file-path [config-m]
  (get config-m :client-credentials-file-path "client-credentials.yml"))

(defn theme [config-m]
  (:theme config-m))

(defn secure?
  "Returns true unless 'secure' environment variable set to 'false'"
  [config-m]
  (not (= "false" (get config-m :secure "true"))))

(defn email-script-path [config-m]
  (if-let [script-path (:email-script-path config-m)]
    script-path
    (log/warn "No email script path provided - Stonecutter will be unable to send emails")))

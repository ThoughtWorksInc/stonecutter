(ns stonecutter.config
  (:require [environ.core :as env]
            [clojure.tools.logging :as log]))

(def env-vars #{:port :host :base-url :mongo-port-27017-tcp-addr
                :mongo-uri :client-credentials-file-path
                :theme :secure :email-script-path :app-name
                :header-bg-color :inactive-tab-font-color
                :static-resources-dir-path :logo-file-name
                :favicon-file-name})

(defn create-config []
  (select-keys env/env env-vars))

(defn get-env
  "Like a normal 'get' except it also ensures the key is in the env-vars set"
  ([config-m key]
   (get config-m (env-vars key)))
  ([config-m key default]
   (get config-m (env-vars key) default)))

(defn port [config-m]
  (Integer. (get-env config-m :port "3000")))

(defn host [config-m]
  (get-env config-m :host "127.0.0.1"))

(defn base-url [config-m]
  (get-env config-m :base-url "http://localhost:3000"))

(defn- get-docker-mongo-uri [config-m]
  (when-let [mongo-ip (:mongo-port-27017-tcp-addr config-m)]
    (format "mongodb://%s:27017/stonecutter" mongo-ip)))

(defn mongo-uri [config-m]
  (or
    (get-docker-mongo-uri config-m)
    (get-env config-m :mongo-uri)
    "mongodb://localhost:27017/stonecutter"))

(defn client-credentials-file-path [config-m]
  (get-env config-m :client-credentials-file-path "client-credentials.yml"))

(defn theme [config-m]
  (get-env config-m :theme))

(defn app-name [config-m]
  (get-env config-m :app-name "Stonecutter"))

(defn secure?
  "Returns true unless 'secure' environment variable set to 'false'"
  [config-m]
  (not (= "false" (get-env config-m :secure "true"))))

(defn email-script-path [config-m]
  (if-let [script-path (get-env config-m :email-script-path)]
    script-path
    (log/warn "No email script path provided - Stonecutter will be unable to send emails")))

(defn header-bg-color [config-m]
  (get-env config-m :header-bg-color))

(defn inactive-tab-font-color [config-m]
  (get-env config-m :inactive-tab-font-color))

(defn static-resources-dir-path [config-m]
  (get-env config-m :static-resources-dir-path))

(defn logo-file-name [config-m]
  (get-env config-m :logo-file-name))

(defn favicon-file-name [config-m]
  (get-env config-m :favicon-file-name))

(defn admin-login [config-m]
  (get-env config-m :admin-login))

(defn admin-password [config-m]
  (get-env config-m :admin-password))

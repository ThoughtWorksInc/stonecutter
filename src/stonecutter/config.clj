(ns stonecutter.config
  (:require [environ.core :as env]
            [clojure.tools.logging :as log]))

(def env-vars #{:port :host :base-url :mongo-port-27017-tcp-addr
                :mongo-uri :client-credentials-file-path
                :theme :secure :email-script-path :app-name
                :header-bg-color
                :static-resources-dir-path :logo-file-name
                :favicon-file-name :admin-login :admin-password
                :password-reset-expiry
                :open-id-connect-id-token-lifetime-minutes
                :rsa-keypair-file-path})

(def roles {:default "default"
            :admin "admin"})

(defn create-config []
  (select-keys env/env env-vars))

(defn get-env
  "Like a normal 'get' except it also ensures the key is in the env-vars set"
  ([config-m key]
   (get-env config-m key nil))
  ([config-m key default]
   (when-not (env-vars key)
     (throw (Exception. (format "Trying to get-env with key '%s' which is not in the env-vars set" key))))
   (get config-m (env-vars key) default)))

(defn port [config-m]
  (Integer. (get-env config-m :port "3000")))

(defn host [config-m]
  (get-env config-m :host "127.0.0.1"))

(defn base-url [config-m]
  (get-env config-m :base-url "http://localhost:3000"))

(defn- get-docker-mongo-uri [config-m]
  (when-let [mongo-ip (get-env config-m :mongo-port-27017-tcp-addr)]
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

(defn static-resources-dir-path [config-m]
  (get-env config-m :static-resources-dir-path))

(defn logo-file-name [config-m]
  (get-env config-m :logo-file-name))

(defn favicon-file-name [config-m]
  (get-env config-m :favicon-file-name))

(defn admin-login [config-m]
  (if-let [result (get-env config-m :admin-login)]
    result
    (log/warn "no ADMIN_LOGIN provided. Please provide this environment variable to create an admin account.")))

(defn admin-password [config-m]
  (if-let [result (get-env config-m :admin-password)]
    result
    (log/warn "no ADMIN_PASSWORD provided. Please provide this environment variable to create an admin account.")))

(defn password-reset-expiry [config-m]
  (Integer. (get-env config-m :password-reset-expiry "24")))

(defn open-id-connect-id-token-lifetime-minutes [config-m]
  (Integer. (get-env config-m :open-id-connect-id-token-lifetime-minutes "10")))

(defn rsa-keypair-file-path [config-m]
  (if-let [path (get-env config-m :rsa-keypair-file-path)]
    path
    (do (log/error "No RSA-keypair json file provided.")
        (throw (Exception. "No RSA-keypair provided.  App startup aborted.")))))

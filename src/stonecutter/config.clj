(ns stonecutter.config
  (:require [environ.core :as env]
            [clojure.tools.logging :as log]
            [clojure.string :as s]))

(def vars [:port "The port that app listens"
           :host "The host IP that app listens on"
           :base-url "Base url that app is deployed at"
           :mongo-port-27017-tcp-addr "IP address of mongo container linked on port 27017 (should be supplied by container linking)"
           :mongo-uri "URI of mongo database"
           :client-credentials-file-path "Location of file containing client registrations"
           :secure "If set to true will only allow traffic proxied through HTTPS"
           :email-script-path "Location of script used to send e-mails"
           :app-name "The name of the application"
           :header-bg-color "Background colour of top bar"
           :header-font-color "Colour of font in top bar"
           :header-font-color-hover "Hover colour of font in top bar"
           :static-resources-dir-path "Optional location of additional static resources (i.e. logo and favicon)"
           :logo-file-name "Name of logo file in static resources directory"
           :favicon-file-name "Name of favicon file in static resources directory"
           :admin-first-name "First name of admin user"
           :admin-last-name "Last name of admin user"
           :admin-login "Username of admin user"
           :admin-password "Password of admin user"
           :password-reset-expiry "Time (in hours) before password-reset email expires"
           :open-id-connect-id-token-lifetime-minutes "Time (in minutes) before Open ID Connect token expires"
           :invite-expiry "Time (in days) before invite email expires"
           :rsa-keypair-file-path "Location of json file containing RSA keypair (for Open ID Connect)"])

(def env-var-set (->> vars (partition 2) (map first) set))

(def lookup-extension {:jpeg ".jpg"
                       :gif  ".gif"
                       :png  ".png"})

(def roles {:untrusted "untrusted"
            :trusted   "trusted"
            :admin     "admin"})

(def default-profile-picture "/images/temp-avatar-300x300.png")

(def profile-picture-directory "images/profile/")

(defn create-config []
  (select-keys env/env env-var-set))

(defn get-env
  "Like a normal 'get' except it also ensures the key is in the env-vars set"
  ([config-m key]
   (get-env config-m key nil))
  ([config-m key default]
   (when-not (env-var-set key)
     (throw (Exception. (format "Trying to get-env with key '%s' which is not in the env-vars set" key))))
   (get config-m (env-var-set key) default)))

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
  (get-env config-m :header-bg-color "#eee"))

(defn header-font-color [config-m]
  (get-env config-m :header-font-color "#222"))

(defn header-font-color-hover [config-m]
  (get-env config-m :header-font-color-hover "#00d3ca"))

(defn static-resources-dir-path [config-m]
  (get-env config-m :static-resources-dir-path))

(defn logo-file-name [config-m]
  (get-env config-m :logo-file-name))

(defn favicon-file-name [config-m]
  (get-env config-m :favicon-file-name))

(defn admin-first-name [config-m]
  (get-env config-m :admin-first-name "Mighty"))

(defn admin-last-name [config-m]
  (get-env config-m :admin-last-name "Admin"))

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

(defn invite-expiry [config-m]
  (Integer. (get-env config-m :invite-expiry "7")))

(defn open-id-connect-id-token-lifetime-minutes [config-m]
  (Integer. (get-env config-m :open-id-connect-id-token-lifetime-minutes "10")))

(defn rsa-keypair-file-path [config-m]
  (if-let [path (get-env config-m :rsa-keypair-file-path)]
    path
    (do (log/error "No RSA-keypair json file provided.")
        (throw (Exception. "No RSA-keypair provided.  App startup aborted.")))))

(defn to-env [k]
  (-> k
      name
      (s/upper-case)
      (s/replace #"\-" "_")))

(defn gen-config-line [env-m [var-key description]]
  (str "# " description "\n"
       (if-let [val (var-key env-m)]
         (str (to-env var-key) "=" val)
         (str "# " (to-env var-key) "="))))

(defn gen-config-text [env-m vars]
  (->> vars
       (partition 2)
       (map (partial gen-config-line env-m))
       (s/join "\n\n")))

(defn gen-config! [env-m vars config-file]
  (let [text (gen-config-text env-m vars)]
    (spit config-file text)))

(defn -main [config-file & other-args]
  (gen-config! env/env vars config-file))

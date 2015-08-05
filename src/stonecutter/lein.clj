(ns stonecutter.lein
  (:require [stonecutter.config :as config]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.db.storage :as s]
            [stonecutter.email :as email]
            [stonecutter.db.client-seed :as client-seed]
            [stonecutter.handler :as h]))

(defonce lein-app nil)

(defn lein-ring-init
  "Function called when running app with 'lein ring server'"
  []
  (let [config-m (config/create-config)]
    (vh/disable-template-caching!)
    (s/setup-in-memory-stores!)
    (email/configure-email (config/email-script-path config-m))
    (client-seed/load-client-credentials-and-store-clients @s/client-store (config/client-credentials-file-path config-m))
    (alter-var-root #'lein-app (constantly (h/create-app (config/create-config)
                                                         (s/create-in-memory-stores)
                                                         :dev-mode? true)))))
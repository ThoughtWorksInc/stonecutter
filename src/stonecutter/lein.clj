(ns stonecutter.lein
  (:require [stonecutter.config :as config]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.db.storage :as s]
            [stonecutter.email :as email]
            [stonecutter.db.client-seed :as client-seed]
            [stonecutter.admin :as admin]
            [stonecutter.handler :as h]))

(defonce lein-app nil)

(defn lein-ring-init
  "Function called when running app with 'lein ring server'"
  []
  (let [config-m (config/create-config)
        stores-m (s/create-in-memory-stores)
        email-sender (email/bash-sender-factory nil)]
    (vh/disable-template-caching!)
    (admin/create-admin-user config-m (s/get-user-store stores-m))
    (client-seed/load-client-credentials-and-store-clients (s/get-client-store stores-m) (config/client-credentials-file-path config-m))
    (alter-var-root #'lein-app (constantly (h/create-app (config/create-config) stores-m email-sender true)))))

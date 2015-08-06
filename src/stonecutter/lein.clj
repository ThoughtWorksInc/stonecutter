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
  (let [config-m (config/create-config)
        stores (s/create-in-memory-stores)]
    (vh/disable-template-caching!)
    (email/configure-email (config/email-script-path config-m))
    (client-seed/load-client-credentials-and-store-clients (s/get-client-store stores) (config/client-credentials-file-path config-m))
    (alter-var-root #'lein-app (constantly (h/create-app (config/create-config) stores :dev-mode? true)))))
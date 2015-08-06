(ns stonecutter.admin
  (:require [stonecutter.config :as config]
            [stonecutter.db.user :as u]))

(defn create-admin-user [config-m user-store]
  (let [admin-login (config/admin-login config-m)
        admin-password (config/admin-password config-m)]
    (when (and admin-login admin-password)
      (when-not (u/is-duplicate-user? user-store admin-login)
        (u/store-admin!
          user-store
          admin-login
          admin-password)))))

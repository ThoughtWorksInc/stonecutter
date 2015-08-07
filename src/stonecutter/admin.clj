(ns stonecutter.admin
  (:require [stonecutter.config :as config]
            [stonecutter.validation :as v]
            [clojure.tools.logging :as log]
            [stonecutter.db.user :as u]))

(defn create-admin-user [config-m user-store]
  (let [admin-login (config/admin-login config-m)
        admin-password (config/admin-password config-m)
        duplication-checker (partial u/is-duplicate-user? user-store)
        errors (v/validate-registration-email admin-login duplication-checker)]
    (when (and admin-login admin-password)
      (case errors
        :duplicate (log/info "Admin account already exists.")
        :invalid (throw (Exception. "INVALID ADMIN DETAILS - please check that admin login is in the correct format"))
        nil (u/store-admin!
              user-store
              admin-login
              admin-password)))))

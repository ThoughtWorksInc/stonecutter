(ns stonecutter.db.forgotten-password
  (:require [stonecutter.db.expiry :as e]
            [stonecutter.util.time :as time]))

(defn store-id-for-user! [forgotten-password-store clock forgotten-password-id login expiry-hours]
  (let [doc {:forgotten-password-id forgotten-password-id :login login}]
    (e/store-with-expiry! forgotten-password-store clock :forgotten-password-id doc (* time/hour expiry-hours))))

(defn forgotten-password-doc-by-login [forgotten-password-store clock login]
  (let [docs (e/query-with-expiry forgotten-password-store clock :forgotten-password-id {:login login})]
    (case (count docs)
      0 nil
      1 (first docs)
      (throw (Exception. (format "Multiple forgotten password records found for login [%s]" login)))))) ;; FIXME JOHN 12/08/2015 make this more general

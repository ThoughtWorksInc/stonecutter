(ns stonecutter.db.forgotten-password
  (:require [clauth.store :as cl-store]
            [stonecutter.db.mongo :as m]))

(defn store-id-for-user! [forgotten-password-store forgotten-password-id login]
  (let [doc {:forgotten-password-id forgotten-password-id :login login}]
    (cl-store/store! forgotten-password-store :forgotten-password-id doc)))

(defn forgotten-password-doc-by-login [forgotten-password-store login]
  (let [docs (m/query forgotten-password-store {:login login})]
    (case (count docs)
      0 nil
      1 (first docs)
      (throw (Exception. (format "Multiple forgotten password records found for login [%s]" login))))))

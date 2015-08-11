(ns stonecutter.db.forgotten-password
  (:require [clauth.store :as cl-store]))

(defn store-id-for-user! [forgotten-password-store forgotten-password-id login]
  (let [doc {:forgotten-password-id forgotten-password-id :login login}]
    (cl-store/store! forgotten-password-store :forgotten-password-id doc)))

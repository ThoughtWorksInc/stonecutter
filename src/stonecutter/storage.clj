(ns stonecutter.storage
  (:require [clauth.user :as user-store]
            [clauth.store :as store]
            [stonecutter.mongo :as m]
            [clojure.string :as s]))

(defn setup-mongo-stores! [mongo-uri]
  (let [db (m/get-mongo-db mongo-uri)]
    (swap! user-store/user-store (constantly (m/create-mongo-user-store db)))))

(defn setup-in-memory-stores! []
  (swap! user-store/user-store (constantly (store/create-memory-store))))

(defn is-duplicate-user? [email]
  (not (nil? (user-store/fetch-user (s/lower-case email)))))

(defn store-user! [email password]
  (-> email
      s/lower-case
      (user-store/new-user password)
      user-store/store-user))

(defn retrieve-user [email password]
  (let [user (user-store/authenticate-user email password)]
    (when user
      {:email (:login user)})))

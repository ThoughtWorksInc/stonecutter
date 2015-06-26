(ns stonecutter.storage
  (:require [clauth.user :as user-store]
            [clauth.token :as token-store]
            [clauth.client :as client-store]
            [clauth.auth-code :as auth-code-store]
            [clauth.store :as store]
            [stonecutter.mongo :as m]
            [clojure.string :as s]))

(defn setup-mongo-stores! [mongo-uri]
  (let [db (m/get-mongo-db mongo-uri)]
    (swap! user-store/user-store (constantly (m/create-mongo-user-store db)))
    (swap! token-store/token-store (constantly (m/create-token-store db)))
    (swap! auth-code-store/auth-code-store (constantly (m/create-auth-code-store db)))
    (swap! client-store/client-store (constantly (m/create-client-store db)))))

(defn setup-in-memory-stores! []
  (swap! user-store/user-store (constantly (store/create-memory-store)))
  (swap! token-store/token-store (constantly (store/create-memory-store)))
  (swap! auth-code-store/auth-code-store (constantly (store/create-memory-store)))
  (swap! client-store/client-store (constantly (store/create-memory-store))))

(defn reset-in-memory-stores! []
  (reset! user-store/user-store (store/create-memory-store))
  (reset! token-store/token-store  (store/create-memory-store))
  (reset! auth-code-store/auth-code-store  (store/create-memory-store))
  (reset! client-store/client-store (store/create-memory-store)))

(defn is-duplicate-user? [email]
  (not (nil? (user-store/fetch-user (s/lower-case email)))))

(defn store-user! [email password]
  (-> email
      s/lower-case
      (user-store/new-user password)
      user-store/store-user))

(defn authenticate-and-retrieve-user [email password]
  (let [user (user-store/authenticate-user email password)]
    (when user
      {:email (:login user)})))

(ns stonecutter.storage
  (:require [clauth.user :as user-store]
            [clauth.token :as token-store]
            [clauth.client :as client-store]
            [clauth.auth-code :as auth-code-store]
            [clauth.store :as store]
            [clojure.string :as s]
            [clojure.tools.logging :as log]
            [stonecutter.mongo :as m])
  (:import (java.util UUID)))

(defn setup-mongo-stores! [mongo-uri]
  (let [db (m/get-mongo-db mongo-uri)]
    (swap! user-store/user-store (constantly (m/create-mongo-user-store db)))
    (swap! token-store/token-store (constantly (m/create-token-store db)))
    (swap! auth-code-store/auth-code-store (constantly (m/create-auth-code-store db)))
    (swap! client-store/client-store (constantly (m/create-client-store db)))))

(defn reset-mongo-stores! [mongo-uri]
  (log/info "Resetting mongo stores")
  (let [db (m/get-mongo-db mongo-uri)]
    (store/reset-store! @user-store/user-store)
    (store/reset-store! @token-store/token-store)
    (store/reset-store! @auth-code-store/auth-code-store)
    (store/reset-store! @client-store/client-store)
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

(defn create-user [id-gen email password]
  (let [lower-email (s/lower-case email)]
    (->
      (user-store/new-user lower-email password)
      (assoc :uid (id-gen)))))

(defn uuid []
  (str (UUID/randomUUID)))

(defn store-user! [email password]
  (-> (create-user uuid email password)
      user-store/store-user
      (dissoc :password)))

(defn authenticate-and-retrieve-user [email password]
  (-> (user-store/authenticate-user email password)
      (dissoc :password)))

(defn retrieve-user-with-auth-code [code]
  (-> (auth-code-store/fetch-auth-code code) :subject))

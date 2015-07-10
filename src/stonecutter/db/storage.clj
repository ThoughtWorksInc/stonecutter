(ns stonecutter.db.storage
  (:require [clauth.user :as cl-user]
            [clauth.token :as cl-token]
            [clauth.client :as cl-client]
            [clauth.auth-code :as cl-auth-code]
            [clauth.store :as cl-store]
            [clojure.string :as s]
            [clojure.tools.logging :as log]
            [stonecutter.db.mongo :as m])
  (:import (java.util UUID)))

(defn setup-mongo-stores! [db]
  (swap! cl-user/user-store (constantly (m/create-mongo-user-store db)))
  (swap! cl-token/token-store (constantly (m/create-token-store db)))
  (swap! cl-auth-code/auth-code-store (constantly (m/create-auth-code-store db)))
  (swap! cl-client/client-store (constantly (m/create-client-store db))))

(defn reset-mongo-stores! [mongo-uri]
  (log/info "Resetting mongo stores")
  (let [db (m/get-mongo-db mongo-uri)]
    (cl-store/reset-store! @cl-user/user-store)
    (cl-store/reset-store! @cl-token/token-store)
    (cl-store/reset-store! @cl-auth-code/auth-code-store)
    (cl-store/reset-store! @cl-client/client-store)
    (swap! cl-user/user-store (constantly (m/create-mongo-user-store db)))
    (swap! cl-token/token-store (constantly (m/create-token-store db)))
    (swap! cl-auth-code/auth-code-store (constantly (m/create-auth-code-store db)))
    (swap! cl-client/client-store (constantly (m/create-client-store db)))))

(defn setup-in-memory-stores! []
  (swap! cl-user/user-store (constantly (cl-store/create-memory-store)))
  (swap! cl-token/token-store (constantly (cl-store/create-memory-store)))
  (swap! cl-auth-code/auth-code-store (constantly (cl-store/create-memory-store)))
  (swap! cl-client/client-store (constantly (cl-store/create-memory-store))))

(defn reset-in-memory-stores! []
  (reset! cl-user/user-store (cl-store/create-memory-store))
  (reset! cl-token/token-store (cl-store/create-memory-store))
  (reset! cl-auth-code/auth-code-store (cl-store/create-memory-store))
  (reset! cl-client/client-store (cl-store/create-memory-store)))

(defn is-duplicate-user? [email]
  (not (nil? (cl-user/fetch-user (s/lower-case email)))))

(defn create-user [id-gen email password]
  (let [lower-email (s/lower-case email)]
    (->
      (cl-user/new-user lower-email password)
      (assoc :uid (id-gen)))))

(defn uuid []
  (str (UUID/randomUUID)))

(defn store-user! [email password]
  (-> (create-user uuid email password)
      cl-user/store-user
      (dissoc :password)))

(defn delete-user! [email]
  (cl-store/revoke! @cl-user/user-store email))

(defn authenticate-and-retrieve-user [email password]
  (-> (cl-user/authenticate-user email password)
      (dissoc :password)))

(defn retrieve-user-with-auth-code [code]
  (-> (cl-auth-code/fetch-auth-code code) :subject))

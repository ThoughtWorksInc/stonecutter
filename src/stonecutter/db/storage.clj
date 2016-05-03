(ns stonecutter.db.storage
  (:require [stonecutter.db.mongo :as m]
            [ring.middleware.session.memory :as mem-session]))

(defn create-mongo-stores [db conn db-name]
  {:auth-code-store          (m/create-auth-code-store db)
   :user-store               (m/create-user-store db)
   :client-store             (m/create-client-store db)
   :token-store              (m/create-token-store db)
   :confirmation-store       (m/create-confirmation-store db)
   :session-store            (m/create-session-store db)
   :forgotten-password-store (m/create-forgotten-password-store db)
   :invitation-store         (m/create-invitation-store db)
   :profile-picture-store    (m/create-profile-picture-store conn db-name)})

(defn create-in-memory-stores [conn]
  {:auth-code-store          (m/create-memory-store)
   :user-store               (m/create-memory-store)
   :client-store             (m/create-memory-store)
   :token-store              (m/create-memory-store)
   :confirmation-store       (m/create-memory-store)
   :forgotten-password-store (m/create-memory-store)
   :session-store            (mem-session/memory-store)
   :invitation-store         (m/create-memory-store)
   :profile-picture-store    (m/create-profile-picture-store conn "stonecutter")})

(defn get-auth-code-store [store-m]
  (:auth-code-store store-m))

(defn get-user-store [store-m]
  (:user-store store-m))

(defn get-profile-picture-store [store-m]
  (:profile-picture-store store-m))

(defn get-client-store [store-m]
  (:client-store store-m))

(defn get-token-store [store-m]
  (:token-store store-m))

(defn get-confirmation-store [store-m]
  (:confirmation-store store-m))

(defn get-forgotten-password-store [store-m]
  (:forgotten-password-store store-m))

(defn get-session-store [store-m]
  (:session-store store-m))

(defn get-invitation-store [store-m]
  (:invitation-store store-m))

(ns stonecutter.db.storage
  (:require [stonecutter.db.mongo :as m]
            [stonecutter.db.session :as session]
            [ring.middleware.session.memory :as mem-session]))

(defonce auth-code-store (atom (m/create-memory-store)))
(defonce client-store (atom (m/create-memory-store)))

(defn setup-mongo-stores! [db]
  (reset! auth-code-store (m/create-auth-code-store db))
  (reset! client-store (m/create-client-store db)))

(defn setup-in-memory-stores! []
  (reset! auth-code-store (m/create-memory-store))
  (reset! client-store (m/create-memory-store)))

(defn reset-in-memory-stores! []
  (setup-in-memory-stores!))

(defn create-mongo-stores [db]
  {:auth-code-store    (m/create-auth-code-store db)
   :user-store         (m/create-user-store db)
   :client-store       @client-store
   :token-store        (m/create-token-store db)
   :confirmation-store (m/create-confirmation-store db)})

(defn create-in-memory-stores []
  {:auth-code-store    (m/create-memory-store)
   :user-store         (m/create-memory-store)
   :client-store       @client-store
   :token-store        (m/create-memory-store)
   :confirmation-store (m/create-memory-store)})

(defn get-auth-code-store [store-m]
  (:auth-code-store store-m))

(defn get-user-store [store-m]
  (:user-store store-m))

(defn get-client-store [store-m]
  (:client-store store-m))

(defn get-token-store [store-m]
  (:token-store store-m))

(defn get-confirmation-store [store-m]
  (:confirmation-store store-m))

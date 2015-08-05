(ns stonecutter.db.storage
  (:require [stonecutter.db.mongo :as m]
            [stonecutter.db.session :as session]
            [ring.middleware.session.memory :as mem-session]))

(defonce user-store (atom (m/create-memory-store)))
(defonce token-store (atom (m/create-memory-store)))
(defonce auth-code-store (atom (m/create-memory-store)))
(defonce client-store (atom (m/create-memory-store)))

(defonce session-store (atom nil))

(defn get-session-store []
  @session-store)

(def confirmation-store (atom m/create-memory-store))

(defn setup-mongo-stores! [db]
  (reset! user-store (m/create-mongo-user-store db))
  (reset! token-store (m/create-token-store db))
  (reset! auth-code-store (m/create-auth-code-store db))
  (reset! client-store (m/create-client-store db))
  (reset! confirmation-store (m/create-confirmation-store db))
  (reset! session-store (session/mongo-session-store db)))

(defn setup-in-memory-stores! []
  (reset! user-store (m/create-memory-store))
  (reset! token-store (m/create-memory-store))
  (reset! auth-code-store (m/create-memory-store))
  (reset! client-store (m/create-memory-store))
  (reset! confirmation-store (m/create-memory-store))
  (reset! session-store (mem-session/memory-store (atom {}))))

(defn reset-in-memory-stores! []
  (setup-in-memory-stores!))

(defn create-mongo-stores [db]
  {:user-store @user-store
   :client-store @client-store})

(defn create-in-memory-stores []
  {:user-store @user-store
   :client-store @client-store})

(defn get-user-store [store-m]
  (:user-store store-m))

(defn get-client-store [store-m]
  (:client-store store-m))

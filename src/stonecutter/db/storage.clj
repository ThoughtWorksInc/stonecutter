(ns stonecutter.db.storage
  (:require [clauth.user :as cl-user]
            [clauth.token :as cl-token]
            [clauth.client :as cl-client]
            [clauth.auth-code :as cl-auth-code]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.session :as session]
            [ring.middleware.session.memory :as mem-session]))

(defonce session-store (atom nil))

(defn get-session-store []
  @session-store)

(def confirmation-store (atom m/create-memory-store))

(defn setup-mongo-stores! [db]
  (swap! cl-user/user-store (constantly (m/create-mongo-user-store db)))
  (swap! cl-token/token-store (constantly (m/create-token-store db)))
  (swap! cl-auth-code/auth-code-store (constantly (m/create-auth-code-store db)))
  (swap! cl-client/client-store (constantly (m/create-client-store db)))
  (swap! confirmation-store (constantly (m/create-confirmation-store db)))
  (swap! session-store (constantly (session/mongo-session-store db))))

(defn setup-in-memory-stores! []
  (reset! cl-user/user-store (m/create-memory-store))
  (reset! cl-token/token-store (m/create-memory-store))
  (reset! cl-auth-code/auth-code-store (m/create-memory-store))
  (reset! cl-client/client-store (m/create-memory-store))
  (reset! confirmation-store (m/create-memory-store))
  (reset! session-store (mem-session/memory-store (atom {}))))

(defn reset-in-memory-stores! []
  (setup-in-memory-stores!))

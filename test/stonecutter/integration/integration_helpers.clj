(ns stonecutter.integration.integration-helpers
  (:require [monger.core :as monger]
            [monger.collection :as mc]
            [stonecutter.db.mongo :as sc-m]))

(def test-db-name "stonecutter-test")
(def test-db-uri (format "mongodb://localhost:27017/%s" test-db-name))

(def db-and-conn (atom nil))

(defn get-test-db []
  (:db @db-and-conn))

(defn get-test-db-connection []
  (:conn @db-and-conn))


(defn- drop-db [db-and-conn]
  (monger/drop-db (:conn db-and-conn) test-db-name)
  db-and-conn)

(defn setup-db []
  (->>
   (monger/connect-via-uri test-db-uri)
   drop-db
   (reset! db-and-conn)))

(defn teardown-db []
  (drop-db @db-and-conn)
  (monger/disconnect (get-test-db-connection))
  (reset! db-and-conn nil))

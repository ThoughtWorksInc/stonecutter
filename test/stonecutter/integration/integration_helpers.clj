(ns stonecutter.integration.integration-helpers
  (:require [monger.core :as monger]
            [monger.collection :as mc]
            [stonecutter.jwt :as jwt]
            [stonecutter.util.time :as t]
            [stonecutter.handler :as h]
            [stonecutter.db.storage :as s]
            [stonecutter.test.email :as e]
            [stonecutter.config :as c]
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

(defn default-app-config-m []
  (let [json-web-key (jwt/load-key-pair "test-resources/test-key.json")]
    {:prone-stack-tracing? false
     :config-m {:secure "false"}
     :stores-m (s/create-in-memory-stores)
     :email-sender (e/create-test-email-sender)
     :clock (t/new-clock)
     :token-generator (jwt/create-generator (t/new-clock) json-web-key (c/base-url {}))
     :json-web-key-set (jwt/json-web-key->json-web-key-set json-web-key)}))

(defn build-app [app-config-override-m]
  (let [{:keys [prone-stack-tracing? config-m stores-m json-web-key-set
                email-sender token-generator clock ]} (merge (default-app-config-m) app-config-override-m)]
    (h/create-app config-m clock stores-m email-sender token-generator json-web-key-set prone-stack-tracing?)))

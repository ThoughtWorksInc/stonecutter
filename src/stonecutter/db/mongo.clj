(ns stonecutter.db.mongo
  (:require [clauth.store :as cl-s]
            [monger.collection :as mc]
            [monger.core :as mongo]
            [clojure.tools.logging :as log]))

(def user-collection "users")
(def token-collection "tokens")
(def auth-code-collection "auth-codes")
(def client-collection "clients")

(defrecord MongoStore [mongo-db coll]
  cl-s/Store
  (fetch [this t]
    (when t
      (-> (mc/find-map-by-id mongo-db coll t)
          (dissoc :_id))))
  (revoke! [this t]
    (when t
      (mc/remove-by-id mongo-db coll t)))
  (store! [this k item]
    (-> (mc/insert-and-return mongo-db coll (assoc item :_id (k item)))
        (dissoc :_id)))
  (entries [this]
    (->> (mc/find-maps mongo-db coll)
         (map #(dissoc % :_id))))
  (reset-store! [this]
    (mc/remove mongo-db coll)))

(defn new-mongo-store [mongo-db coll]
  (MongoStore. mongo-db coll))

(defn get-mongo-db [mongo-uri]
  (log/debug "Connecting to mongo")
  (let [db (-> (mongo/connect-via-uri mongo-uri) :db)]
    (log/debug "Connected to mongo.")
    db))

(defn create-mongo-user-store [db]
  (mc/ensure-index db user-collection {:login 1} {:unique true})
  (new-mongo-store db user-collection))

(defn create-token-store [db]
  (new-mongo-store db token-collection))

(defn create-auth-code-store [db]
  (new-mongo-store db auth-code-collection))

(defn create-client-store [db]
  (new-mongo-store db client-collection))

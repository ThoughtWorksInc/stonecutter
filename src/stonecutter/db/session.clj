(ns stonecutter.db.session
  (:require [ring.middleware.session.store :as store]
            [monger.collection :as monger-c]
            [stonecutter.db.storage :as db]))

(def collection "session")

(defrecord MongoSessionStore [mongo-db]
  store/SessionStore
  (read-session [_ key]
    (->
      (monger-c/find-map-by-id mongo-db collection key)
      (dissoc :_id)))
  (write-session [_ key data]
    (let [k (or key (db/uuid))]
      (monger-c/insert mongo-db collection (assoc data :_id k))))
  (delete-session [_ key]
    (monger-c/remove-by-id mongo-db collection key)))

(defn mongo-session-store [mongo-db]
  (MongoSessionStore. mongo-db))



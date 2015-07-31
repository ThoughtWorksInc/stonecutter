(ns stonecutter.db.session
  (:require [ring.middleware.session.store :as store]
            [monger.collection :as monger-c]
            [stonecutter.util.uuid :as uuid]))

(def collection "session")

(defrecord MongoSessionStore [mongo-db]
  store/SessionStore
  (read-session [_ key]
    (->
      (monger-c/find-map-by-id mongo-db collection key)
      (dissoc :_id)))
  (write-session [_ key data]
    (let [k (or key (uuid/uuid))]
      (monger-c/insert mongo-db collection (assoc data :_id k))))
  (delete-session [_ key]
    (monger-c/remove-by-id mongo-db collection key)))

(defn mongo-session-store [mongo-db]
  (MongoSessionStore. mongo-db))



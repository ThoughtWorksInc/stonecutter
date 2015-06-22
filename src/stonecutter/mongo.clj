(ns stonecutter.mongo
  (:require [clauth.store :as s]
            [monger.collection :as mc]))

(def user-collection "users")

(defrecord MongoStore [mongo-db]
  s/Store
  (fetch [this t]
    (->
      (mc/find-one-as-map mongo-db user-collection {:login t})
      (dissoc :_id)))
  (revoke! [this k]
    (mc/remove mongo-db user-collection {:login k}))
  (store! [this k item]
    (->
      (mc/insert-and-return mongo-db user-collection item)
      (dissoc :_id)))
  (entries [this]
    (->> (mc/find-maps mongo-db user-collection)
         (map #(dissoc % :_id))))
  (reset-store! [this]
    (mc/remove mongo-db user-collection)))

(defn new-mongo-store [mongo-db]
  (MongoStore. mongo-db))
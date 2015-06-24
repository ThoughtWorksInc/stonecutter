(ns stonecutter.mongo
  (:require [clauth.store :as s]
            [monger.collection :as mc]
            [monger.core :as mongo]
            [clojure.tools.logging :as log]))

(def user-collection "users")

(defrecord MongoStore [mongo-db coll]
  s/Store
  (fetch [this t]
    (->
      (mc/find-one-as-map mongo-db coll {:login t})
      (dissoc :_id)))
  (revoke! [this k]
    (mc/remove mongo-db coll {:login k}))
  (store! [this k item]
    (->
      (mc/insert-and-return mongo-db coll item)
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

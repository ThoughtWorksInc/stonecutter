(ns stonecutter.db.mongo
  (:require [clauth.store :as cl-s]
            [monger.collection :as mc]
            [monger.core :as mongo]
            [clojure.tools.logging :as log]))

(def user-collection "users")
(def token-collection "tokens")
(def auth-code-collection "auth-codes")
(def client-collection "clients")
(def confirmation-collection "confirmations")

(defprotocol StonecutterStore
  (update! [e k update-fn]
           "Update the item found using key k by running the update-fn on it and storing it"))

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
    (mc/remove mongo-db coll))
  StonecutterStore
  (update! [this t update-fn]
    (when-let [item (mc/find-map-by-id mongo-db coll t)]
      (let [updated-item (update-fn item)]
        (mc/update-by-id mongo-db coll t updated-item)
        (-> (mc/find-map-by-id mongo-db coll t)
            (dissoc :_id))))))

(defrecord MemoryStore [data]
  cl-s/Store
  (fetch [this t] (@data t))
  (revoke! [this t] (swap! data dissoc t))
  (store! [this key_param item]
    (do
      (swap! data assoc (key_param item) item)
      item))
  (entries [this] (or (vals @data) []))
  (reset-store! [this] (reset! data {}))
  StonecutterStore
  (update! [this t update-fn]
    (when-let [item (@data t)]
      (let [updated-item (update-fn item)]
        (swap! data assoc t updated-item)
        updated-item))))

(defn create-memory-store
  "Create a memory token store"
  ([] (create-memory-store {}))
  ([data]
     (MemoryStore. (atom data))))

(defn new-mongo-store [mongo-db coll]
  (MongoStore. mongo-db coll))

(defn get-mongo-db [mongo-uri]
  (log/debug "Connecting to mongo")
  (let [db (-> (mongo/connect-via-uri mongo-uri) :db)]
    (log/debug "Connected to mongo.")
    db))

(defn- create-mongo-store [db collection index]
  (mc/ensure-index db collection {index 1} {:unique true})
  (new-mongo-store db collection))

(defn create-mongo-user-store [db]
  (create-mongo-store db user-collection :login))

(defn create-token-store [db]
  (create-mongo-store db token-collection :token))

(defn create-auth-code-store [db]
  (create-mongo-store db auth-code-collection :code))

(defn create-client-store [db]
  (create-mongo-store db client-collection :client-id))

(defn create-confirmation-store [db]
  (create-mongo-store db confirmation-collection :confirmation-id))

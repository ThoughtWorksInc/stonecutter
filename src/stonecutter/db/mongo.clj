(ns stonecutter.db.mongo
  (:require [clauth.store :as cl-s]
            [monger.collection :as mc]
            [monger.core :as mongo]
            [clojure.tools.logging :as log]
            [monger.ring.session-store :as mongo-session]))

(def ^:private user-collection "users")
(def ^:private token-collection "tokens")
(def ^:private auth-code-collection "auth-codes")
(def ^:private client-collection "clients")
(def ^:private confirmation-collection "confirmations")
(def ^:private forgotten-password-collection "forgotten-passwords")
(def ^:private session-collection "sessions")

(defprotocol StonecutterStore
  (update! [e k update-fn]
    "Update the item found using key k by running the update-fn on it and storing it")
  (query [e query]
    "Items are returned using a query map"))

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
        (-> (mc/save-and-return mongo-db coll updated-item)
            (dissoc :_id)))))
  (query [this query]
    (->> (mc/find-maps mongo-db coll query)
         (map #(dissoc % :_id)))))

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
        updated-item)))
  (query [this query]
    (filter #(= query (select-keys % (keys query))) (vals @data))))

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

(defn- create-mongo-store [db collection]
  (new-mongo-store db collection))

(defn create-user-store [db]
  (create-mongo-store db user-collection))

(defn create-token-store [db]
  (create-mongo-store db token-collection))

(defn create-auth-code-store [db]
  (create-mongo-store db auth-code-collection))

(defn create-client-store [db]
  (create-mongo-store db client-collection))

(defn create-confirmation-store [db]
  (create-mongo-store db confirmation-collection))

(defn create-forgotten-password-store [db]
  (create-mongo-store db forgotten-password-collection))

(defn create-session-store [db]
  (mongo-session/session-store db session-collection))

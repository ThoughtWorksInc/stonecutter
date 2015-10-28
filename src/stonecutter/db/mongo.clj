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
(def ^:private invitation-collection "invitations")

(defprotocol StonecutterStore
  (update! [e k update-fn key]
    "Update the item found using key k by running the update-fn on it and storing it. It will update the _id using the key")
  (query [e query]
    "Items are returned using a query map"))

(defrecord MongoStore [mongo-db coll k]
  cl-s/Store
  (fetch [this t]
    (when t
      (-> (mc/find-map-by-id mongo-db coll t)
          (dissoc :_id))))
  (revoke! [this t]
    (when t
      (mc/remove-by-id mongo-db coll t)))
  (store! [this _ item]
    (-> (mc/insert-and-return mongo-db coll (assoc item :_id (k item)))
        (dissoc :_id)))
  (entries [this]
    (->> (mc/find-maps mongo-db coll)
         (map #(dissoc % :_id))))
  (reset-store! [this]
    (mc/remove mongo-db coll))
  StonecutterStore
  (update! [this t update-fn key]
    (when-let [item (mc/find-map-by-id mongo-db coll t)]
      (let [updated-item (update-fn item)]
        (if (= (key item) (key updated-item))
          (-> (mc/save-and-return mongo-db coll updated-item)
              (dissoc :_id))
          (do (mc/remove-by-id mongo-db coll t)
              (-> (mc/insert-and-return mongo-db coll (assoc updated-item :_id (key updated-item)))
                  (dissoc :_id)))))))
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
  (update! [this t update-fn key]
    (when-let [item (@data t)]
      (let [updated-item (update-fn item)]
        (cl-s/revoke! this t)
        (cl-s/store! this key updated-item))))
  (query [this query]
    (filter #(= query (select-keys % (keys query))) (vals @data))))

(defn create-memory-store
  "Create a memory token store"
  ([] (create-memory-store {}))
  ([data]
   (MemoryStore. (atom data))))

(defn new-mongo-store [mongo-db coll k]
  (MongoStore. mongo-db coll k))

(defn get-mongo-db [mongo-uri]
  (log/debug "Connecting to mongo")
  (let [db (-> (mongo/connect-via-uri mongo-uri) :db)]
    (log/debug "Connected to mongo.")
    db))

(defn create-user-store [db]
  (new-mongo-store db user-collection :login))

(defn create-token-store [db]
  (new-mongo-store db token-collection :token))

(defn create-auth-code-store [db]
  (new-mongo-store db auth-code-collection :code))

(defn create-client-store [db]
  (new-mongo-store db client-collection :client-id))

(defn create-confirmation-store [db]
  (new-mongo-store db confirmation-collection :confirmation-id))

(defn create-forgotten-password-store [db]
  (new-mongo-store db forgotten-password-collection :forgotten-password-id))

(defn create-session-store [db]
  (mongo-session/session-store db session-collection))

(defn create-invitation-store [db]
  (new-mongo-store db invitation-collection :invite-id))

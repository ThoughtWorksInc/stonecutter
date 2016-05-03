(ns stonecutter.db.mongo
  (:require [clauth.store :as cl-s]
            [monger.collection :as mc]
            [monger.core :as mongo]
            [clojure.tools.logging :as log]
            [monger.ring.session-store :as mongo-session]
            [monger.core :as monger]))

(def user-collection "users")
(def token-collection "tokens")
(def auth-code-collection "auth-codes")
(def client-collection "clients")
(def confirmation-collection "confirmations")
(def forgotten-password-collection "forgotten-passwords")
(def session-collection "sessions")
(def invitation-collection "invitations")

(def user-primary-key :login)
(def token-primary-key :token)
(def auth-code-primary-key :code)
(def client-primary-key :client-id)
(def confirmation-primary-key :confirmation-id)
(def invite-primary-key :invite-id)
(def forgotten-password-primary-key :forgotten-password-id)

(defprotocol StonecutterStore
  (update! [e k update-fn]
    "Update the item found using key k by running the update-fn on it and storing it. It will update the _id using the key")
  (query [e query]
    "Items are returned using a query map"))

(defrecord MongoStore [mongo-db coll primary-key]
  cl-s/Store
  (fetch [this t]
    (when t
      (-> (mc/find-maps mongo-db coll {primary-key t})
          first
          (dissoc :_id))))
  (revoke! [this t]
    (when t
      (mc/remove mongo-db coll {primary-key t})))
  (store! [this _ item]
    (-> (mc/insert-and-return mongo-db coll item)
        (dissoc :_id)))
  (entries [this]
    (->> (mc/find-maps mongo-db coll)
         (map #(dissoc % :_id))))
  (reset-store! [this]
    (mc/remove mongo-db coll))
  StonecutterStore
  (update! [this t update-fn]
    (when-let [item (first (mc/find-maps mongo-db coll {primary-key t}))]
      (let [updated-item (update-fn item)]
        (-> (mc/save-and-return mongo-db coll updated-item)
            (dissoc :_id)))))
  (query [this query]
    (->> (mc/find-maps mongo-db coll query)
         (map #(dissoc % :_id)))))

(defrecord MemoryStore [primary-key-atom data]
  cl-s/Store
  (fetch [this t] (@data t))
  (revoke! [this t] (swap! data dissoc t))
  (store! [this key_param item]
    (do
      (swap! data assoc (key_param item) item)
      (reset! primary-key-atom key_param)
      item))
  (entries [this] (or (vals @data) []))
  (reset-store! [this] (reset! data {}))
  StonecutterStore
  (update! [this t update-fn]
    (when-let [item (@data t)]
      (let [updated-item (update-fn item)]
        (cl-s/revoke! this t)
        (cl-s/store! this @primary-key-atom updated-item))))
  (query [this query]
    (filter #(= query (select-keys % (keys query))) (vals @data))))

(defn create-memory-store
  "Create a memory token store"
  ([] (create-memory-store {}))
  ([data]
   (MemoryStore. (atom nil) (atom data))))

(defn new-mongo-store [mongo-db coll k]
  (MongoStore. mongo-db coll k))

(defn get-mongo-db [mongo-uri]
  (log/debug "Connecting to mongo")
  (let [db-and-conn-map (mongo/connect-via-uri mongo-uri)]
    (log/debug "Connected to mongo.")
    db-and-conn-map))

(defn create-user-store [db]
  (new-mongo-store db user-collection user-primary-key))

(defn create-token-store [db]
  (new-mongo-store db token-collection token-primary-key))

(defn create-auth-code-store [db]
  (new-mongo-store db auth-code-collection auth-code-primary-key))

(defn create-client-store [db]
  (new-mongo-store db client-collection client-primary-key))

(defn create-confirmation-store [db]
  (new-mongo-store db confirmation-collection confirmation-primary-key))

(defn create-forgotten-password-store [db]
  (new-mongo-store db forgotten-password-collection forgotten-password-primary-key))

(defn create-invitation-store [db]
  (new-mongo-store db invitation-collection invite-primary-key))

(defn create-session-store [db]
  (mongo-session/session-store db session-collection))

(defn create-profile-picture-store [conn db-name]
  (when conn
    (monger/get-gridfs conn db-name)))

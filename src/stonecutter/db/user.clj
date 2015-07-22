(ns stonecutter.db.user
  (:require [clauth.user :as cl-user]
            [clauth.auth-code :as cl-auth-code]
            [clauth.store :as cl-store]
            [clojure.string :as s]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.mongo :as m]))

(defn create-user [id-gen email password]
  (let [lower-email (s/lower-case email)]
    (->
      (cl-user/new-user lower-email password)
      (assoc :uid (id-gen)))))

(defn store-user! [email password]
  (-> (create-user storage/uuid email password)
      cl-user/store-user
      (dissoc :password)))

(defn retrieve-user [email]
  (cl-user/fetch-user email))

(defn is-duplicate-user? [email]
  (not (nil? (retrieve-user (s/lower-case email)))))

(defn delete-user! [email]
  (cl-store/revoke! @cl-user/user-store email))

(defn authenticate-and-retrieve-user [email password]
  (-> (cl-user/authenticate-user email password)
      (dissoc :password)))

(defn retrieve-user-with-auth-code [code]
  (-> (cl-auth-code/fetch-auth-code code) :subject))

(defn unique-conj [things thing]
  (let [unique-things (set things)
        unique-things-list (into [] unique-things)]
    (if (unique-things thing)
      unique-things-list
      (conj unique-things-list thing))))

(defn add-client-id [client-id]
  (fn [user]
    (update-in user [:authorised-clients] unique-conj client-id)))

(defn add-authorised-client-for-user! [email client-id]
  (m/update! @cl-user/user-store email (add-client-id client-id)))

(defn remove-client-id [client-id]
  (fn [user]
    (update-in user [:authorised-clients] (partial remove #(= % client-id)))))

(defn remove-authorised-client-for-user! [email client-id]
  (m/update! @cl-user/user-store email (remove-client-id client-id)))

(defn is-authorised-client-for-user? [email client-id]
  (let [user (retrieve-user email)
        authorised-clients (set (:authorised-clients user))]
    (boolean (authorised-clients client-id))))

(defn update-password [password]
  (fn [user]
    (assoc user :password (cl-user/bcrypt password))))

(defn change-password! [email new-password]
  (m/update! @cl-user/user-store email (update-password new-password)))

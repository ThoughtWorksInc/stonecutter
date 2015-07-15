(ns stonecutter.db.user
  (:require [clauth.user :as cl-user]
            [clauth.auth-code :as cl-auth-code]
            [clauth.store :as cl-store]
            [clojure.string :as s]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.mongo :as m]))

(defn is-duplicate-user? [email]
  (not (nil? (cl-user/fetch-user (s/lower-case email)))))

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
  (-> (m/update! @cl-user/user-store email (add-client-id client-id))))

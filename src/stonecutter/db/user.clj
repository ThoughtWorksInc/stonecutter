(ns stonecutter.db.user
  (:require [clauth.user :as cl-user]
            [clauth.auth-code :as cl-auth-code]
            [clauth.store :as cl-store]
            [clojure.string :as s]
            [stonecutter.config :as config]
            [stonecutter.db.mongo :as m]
            [stonecutter.util.uuid :as uuid]))

(defn create-user
  ([id-gen email password]
   (create-user id-gen email password (:default config/roles)))
  ([id-gen email password role]
   (let [lower-email (s/lower-case email)]
     (->
       (cl-user/new-user lower-email password)
       (assoc :confirmed? false)
       (assoc :uid (id-gen))
       (assoc :role role)))))

(defn create-admin [id-gen email password]
  (create-user id-gen email password (:admin config/roles)))

(defn store-user! [user-store email password]
  (let [user (create-user uuid/uuid email password)]
    (-> (cl-user/store-user user-store user)
        (dissoc :password))))

(defn store-admin! [user-store email password]
  (let [user (create-admin uuid/uuid email password)]
    (-> (cl-user/store-user user-store user)
        (dissoc :password))))

(defn retrieve-user [user-store email]
  (cl-user/fetch-user user-store (s/lower-case email)))

(defn user-exists? [user-store email]
  (not (nil? (retrieve-user user-store (s/lower-case email)))))

(defn delete-user! [user-store email]
  (cl-store/revoke! user-store email))

(defn authenticate-and-retrieve-user [user-store email password]
  (-> (cl-user/authenticate-user user-store (s/lower-case email) password)
      (dissoc :password)))

(defn retrieve-user-with-auth-code [auth-code-store code]
  (-> (cl-auth-code/fetch-auth-code auth-code-store code) :subject))

(defn retrieve-auth-code [auth-code-store code]
  (cl-auth-code/fetch-auth-code auth-code-store code))

(defn confirm-email! [user-store user]
  (m/update! user-store (:login user)
             (fn [user] (-> user
                            (assoc :confirmed? true)))))

(defn unique-conj [things thing]
  (let [unique-things (set things)
        unique-things-list (into [] unique-things)]
    (if (unique-things thing)
      unique-things-list
      (conj unique-things-list thing))))

(defn add-client-id [client-id]
  (fn [user]
    (update-in user [:authorised-clients] unique-conj client-id)))

(defn add-authorised-client-for-user! [user-store email client-id]
  (m/update! user-store email (add-client-id client-id)))

(defn remove-client-id [client-id]
  (fn [user]
    (update-in user [:authorised-clients] (partial remove #(= % client-id)))))

(defn remove-authorised-client-for-user! [user-store email client-id]
  (m/update! user-store email (remove-client-id client-id)))

(defn is-authorised-client-for-user? [user-store email client-id]
  (let [user (retrieve-user user-store email)
        authorised-clients (set (:authorised-clients user))]
    (boolean (authorised-clients client-id))))

(defn update-password [password]
  (fn [user]
    (assoc user :password (cl-user/bcrypt password))))

(defn change-password! [user-store email new-password]
  (m/update! user-store email (update-password new-password)))

(defn has-admin-role? [user-m]
  (= (:role user-m) (:admin config/roles)))

(defn retrieve-users [user-store]
  (->> (cl-store/entries user-store)
       (map #(dissoc % :password))))
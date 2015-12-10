(ns stonecutter.db.user
  (:require [clauth.user :as cl-user]
            [clauth.auth-code :as cl-auth-code]
            [clauth.store :as cl-store]
            [clojure.string :as s]
            [stonecutter.config :as config]
            [stonecutter.db.mongo :as m]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.session :as session]
            [monger.gridfs :as grid-fs]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [monger.operators :refer :all])
  (:import [org.apache.commons.io IOUtils]
           [org.apache.commons.codec.binary Base64]))

(defn create-user [id-gen first-name last-name email password role]
  (let [lower-email (s/lower-case email)]
    (->
      (cl-user/new-user lower-email password)
      (dissoc :name)
      (assoc :uid (id-gen))
      (assoc :first-name first-name)
      (assoc :last-name last-name)
      (assoc :confirmed? false)
      (assoc :role role))))

(defn create-admin [id-gen first-name last-name email password]
  (create-user id-gen first-name last-name email password (:admin config/roles)))

(defn store-user!
  ([user-store first-name last-name email password]
   (store-user! user-store first-name last-name email password (:untrusted config/roles)))
  ([user-store first-name last-name email password role]
   (let [user (create-user uuid/uuid first-name last-name email password role)]
     (-> (cl-user/store-user user-store user)
         (dissoc :password)))))

(defn store-admin! [user-store first-name last-name email password]
  (let [user (create-admin uuid/uuid first-name last-name email password)]

    (-> (cl-user/store-user user-store user)
        (dissoc :password))))

(defn retrieve-user [user-store email]
  (cl-user/fetch-user user-store (s/lower-case email)))

(defn user-exists? [user-store email]
  (not (nil? (retrieve-user user-store (s/lower-case email)))))

(defn delete-user! [user-store email]
  (cl-store/revoke! user-store (s/lower-case email)))

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
  (vec (conj (set things) thing)))

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

(defn authorisation-checker [user-store]
  (comp has-admin-role? (partial retrieve-user user-store) session/request->user-login))

(defn update-user-role [role]
  (fn [user]
    (assoc user :role role)))

(defn update-user-role! [user-store email role]
  (m/update! user-store email (update-user-role role)))

(defn update-user-email [email]
  (fn [user]
    (-> (assoc user :login email)
        (assoc :confirmed? false))))

(defn update-user-email! [user-store email new-email]
  (-> (m/update! user-store email (update-user-email new-email))
      (dissoc :password)))

(defn update-profile-picture! [request profile-picture-store uid]
  (let [uploaded-file-path (get-in request [:params :profile-photo :tempfile])
        content-type (get-in request [:params :profile-photo :content-type])
        file-extension ((keyword (last (string/split content-type #"/"))) config/lookup-extension)
        filename (str uid file-extension)]
    (grid-fs/remove profile-picture-store {:filename {$regex (str uid ".*")}})
    (grid-fs/store-file (grid-fs/make-input-file profile-picture-store (io/file uploaded-file-path))
                        (grid-fs/filename filename)
                        (grid-fs/content-type content-type))))

(defn retrieve-profile-picture [profile-picture-store uid]
  (when-let [file (first (grid-fs/find-by-filename profile-picture-store {$regex (str uid ".*")}))]
    (->> file
         .getInputStream
         IOUtils/toByteArray
         Base64/encodeBase64
         (map char)
         (apply str "data:" (.getContentType file) ";base64,"))))

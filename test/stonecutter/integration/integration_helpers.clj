(ns stonecutter.integration.integration-helpers
  (:require [monger.core :as monger]
            [stonecutter.jwt :as jwt]
            [stonecutter.util.time :as t]
            [stonecutter.handler :as h]
            [stonecutter.db.storage :as s]
            [stonecutter.test.email :as e]
            [stonecutter.config :as c]
            [stonecutter.db.storage :as storage]
            [stonecutter.admin :as admin]
            [monger.gridfs :as grid-fs]
            [clojure.java.io :as io]
            [stonecutter.db.user :as user]
            [stonecutter.config :as config]))

(def test-db-name "stonecutter-test")
(def test-db-uri (format "mongodb://localhost:27017/%s" test-db-name))

(def db-and-conn (atom nil))

(defn get-test-db []
  (:db @db-and-conn))

(defn get-test-db-connection []
  (:conn @db-and-conn))


(defn- drop-db [db-and-conn]
  (monger/drop-db (:conn db-and-conn) test-db-name)
  db-and-conn)

(defn setup-db []
  (->>
    (monger/connect-via-uri test-db-uri)
    drop-db
    (reset! db-and-conn)))

(defn teardown-db []
  (drop-db @db-and-conn)
  (monger/disconnect (get-test-db-connection))
  (reset! db-and-conn nil))

(defn default-app-config-m []
  (let [json-web-key (jwt/load-key-pair "test-resources/test-key.json")]
    {:prone-stack-tracing? false
     :config-m             {:secure "false"
                            :profile-image-path "resources/public"
                            :mongo-uri "mongodb://localhost:27017/stonecutter-test"
                            :mongo-name "stonecutter-test"}
     :stores-m             (s/create-in-memory-stores (get-test-db-connection))
     :email-sender         (e/create-test-email-sender)
     :clock                (t/new-clock)
     :token-generator      (jwt/create-generator (t/new-clock) json-web-key (c/base-url {}))
     :json-web-key-set     (jwt/json-web-key->json-web-key-set json-web-key)}))

(defn build-app [app-config-override-m]
  (let [{:keys [prone-stack-tracing? config-m stores-m json-web-key-set
                email-sender token-generator clock]} (merge (default-app-config-m) app-config-override-m)]
    (admin/create-admin-user config-m (storage/get-user-store stores-m))
    (h/create-app config-m clock stores-m email-sender token-generator json-web-key-set prone-stack-tracing?)))

(defn get-uid [user-store email]
  (:uid (user/retrieve-user user-store email)))

(defn add-profile-image [state profile-picture-store uid]
  (grid-fs/store-file (grid-fs/make-input-file profile-picture-store (io/file (io/resource "avatar.png")))
                      (grid-fs/filename (str uid ".png"))
                      (grid-fs/content-type "image/png"))
  state)

(defn remove-profile-image [profile-picture-store uid]
  (grid-fs/remove profile-picture-store {:filename (str uid ".png")})
  (io/delete-file (str "resources/public" config/profile-picture-directory uid ".png")))
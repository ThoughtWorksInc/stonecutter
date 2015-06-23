(ns stonecutter.storage
  (:require [clauth.user :as user-store]
            [clauth.store :as store]
            [stonecutter.mongo :refer [mongo-store-from-uri]]
            [clojure.string :as s]))

(defn start-mongo-datastore! [mongo-uri]
  (swap! user-store/user-store (constantly (mongo-store-from-uri mongo-uri))))

(defn start-in-memory-datastore! []
  (swap! user-store/user-store (constantly (store/create-memory-store))))

(defn is-duplicate-user? [username]
  (not (nil? (user-store/fetch-user (s/lower-case username)))))

(defn store-user! [email password]
  (-> email
      s/lower-case
      (user-store/new-user password)
      user-store/store-user))


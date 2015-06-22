(ns stonecutter.storage
  (:require [clauth.user :as user-store]
            [clojure.string :as s]))

(defn is-duplicate-user? [username]
  (not (nil? (user-store/fetch-user (s/lower-case username)))))

(defn p [v] (prn v) v)

(defn store-user! [email password]
  (-> email
      s/lower-case
      (user-store/new-user password)
      user-store/store-user))

(ns stonecutter.db.confirmation
  (:require [clauth.store :as cl-store]
            [stonecutter.db.mongo :as sm]))

(defn store! [confirmation-store login confirmation-id]
  (cl-store/store! confirmation-store :confirmation-id {:login login :confirmation-id confirmation-id}))

(defn fetch [confirmation-store confirmation-id]
 (cl-store/fetch confirmation-store confirmation-id))

(defn revoke! [confirmation-store confirmation-id]
  (cl-store/revoke! confirmation-store confirmation-id))

(defn retrieve-by-user-email [confirmation-store login]
  (first (sm/query confirmation-store {:login login})))

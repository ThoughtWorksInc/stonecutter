(ns stonecutter.db.confirmation
  (:require [clauth.store :as cl-store]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.mongo :as mongo]))

(defn store! [login confirmation-id]
  (cl-store/store! @storage/confirmation-store :confirmation-id {:login login :confirmation-id confirmation-id}))

(defn fetch [confirmation-id] 
 (cl-store/fetch @storage/confirmation-store confirmation-id))

(defn revoke! [confirmation-id]
  (cl-store/revoke! @storage/confirmation-store confirmation-id))

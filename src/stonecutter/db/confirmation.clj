(ns stonecutter.db.confirmation
  (:require [clauth.store :as cl-store]))

(defn store! [confirmation-store login confirmation-id]
  (cl-store/store! confirmation-store :confirmation-id {:login login :confirmation-id confirmation-id}))

(defn fetch [confirmation-store confirmation-id]
 (cl-store/fetch confirmation-store confirmation-id))

(defn revoke! [confirmation-store confirmation-id]
  (cl-store/revoke! confirmation-store confirmation-id))

(ns stonecutter.db.invitations
  (:require [clauth.store :as cl-store]
            [stonecutter.db.mongo :as sm]
            [stonecutter.util.uuid :as uuid]))

(defn generate-invite-id! [invite-store email]
  (let [invite-id (uuid/uuid)]
    (cl-store/store! invite-store :invite-id {:email email :invite-id invite-id})
    invite-id)
  )

(defn fetch-by-id [invite-store id]
  (first (sm/query invite-store {:invite-id id})))

(defn remove-invite! [invite-store invite-id]
  (cl-store/revoke! invite-store invite-id))


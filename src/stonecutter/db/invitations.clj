(ns stonecutter.db.invitations
  (:require [clauth.store :as cl-store]
            [stonecutter.db.mongo :as sm]
            [crypto.random :as random]))

(defn generate-invite-id! [invite-store email]
  (let [invite-id (random/base32 10)]
    (cl-store/store! invite-store :invite-id {:email email :invite-id invite-id})
    invite-id)
  )

(defn fetch-by-id [invite-store id]
  (first (sm/query invite-store {:invite-id id})))


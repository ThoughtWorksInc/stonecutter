(ns stonecutter.db.invitations
  (:require [clauth.store :as cl-store]
            [stonecutter.db.mongo :as sm]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.db.expiry :as e]
            [stonecutter.util.time :as time]))

(defn generate-invite-id! [invite-store email clock expiry-days id-generation-fn]
  (let [invite-id (id-generation-fn)
        expiry (* expiry-days time/day)]
    (e/store-with-expiry! invite-store clock :invite-id {:email email :invite-id invite-id} expiry)
    invite-id))

(defn fetch-by-id [invite-store id]
  (first (sm/query invite-store {:invite-id id})))

(defn fetch-by-email [invite-store email]
  (first (sm/query invite-store {:email email})))

(defn remove-invite! [invite-store invite-id]
  (cl-store/revoke! invite-store invite-id))


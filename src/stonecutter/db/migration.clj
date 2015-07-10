(ns stonecutter.db.migration
  (:require [monger.ragtime :as monger-ragtime]
            [monger.collection :as coll]
            [ragtime.core :as ragtime]
            [clojure.tools.logging :as log]
            [stonecutter.db.storage :as storage]))

(defn do-to-coll [db coll f]
  (let [records (coll/find-maps db coll)]
    (doseq [record records]
      (coll/update-by-id db coll (:_id record) (f record)))))

(defn add-user-id [record]
  (if (:uid record)
    record
    (assoc record :uid (storage/uuid))))

(defn add-user-uids [db]
  (log/info "Running migration add-user-id")
  (do-to-coll db "users" add-user-id)
  (log/info "Finished running migration add-user-id"))

;; IMPORTANT DO *NOT* MODIFY THE EXISTING MIGRATION IDS IN THIS LIST
(def migrations
  [{:id "add-user-uid" :up add-user-uids}]
  )

(defn run-migrations
  ([db]
    (run-migrations db migrations))
  ([db migrations]
   (let [index (ragtime/into-index migrations)]
     (ragtime/migrate-all db index migrations))))



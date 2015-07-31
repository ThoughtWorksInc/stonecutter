(ns stonecutter.db.confirmation
  (:require [stonecutter.db.storage :as storage]
            [stonecutter.db.mongo :as mongo]))

(defn store! [login confirmation-id]
 (@storage/confirmation-store) 
  )

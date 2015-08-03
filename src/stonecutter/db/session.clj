(ns stonecutter.db.session
  (:require [monger.ring.session-store :as mongo-session]))

(def collection "session")

(defn mongo-session-store [mongo-db]
  (mongo-session/session-store mongo-db collection))



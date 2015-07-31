(ns stonecutter.integration.db.session
  (:require [midje.sweet :refer :all]
            [stonecutter.db.session :as session]
            [ring.middleware.session.store :as store]
            [stonecutter.integration.test-helpers :as ith]
            [stonecutter.db.storage :as db]))

(background (before :facts (ith/setup-db)
                    :after (ith/teardown-db)))

(fact "can save session to store with id"
      (let [session-store (session/mongo-session-store (ith/get-test-db))]
        (store/write-session session-store "id1" {:some "data"})
        (store/read-session session-store "id1")) => {:some "data"})

(fact "if id is does not exist then uuid is used"
      (let [session-store (session/mongo-session-store (ith/get-test-db))]
        (store/write-session session-store nil {:some "data"}) => anything
        (provided (db/uuid) => "id1")
        (store/read-session session-store "id1") => {:some "data"}))

(fact "can delete session by id"
      (let [session-store (session/mongo-session-store (ith/get-test-db))]
        (store/write-session session-store "id1" {:some "data"})
        (store/read-session session-store "id1") => {:some "data"}
        (store/delete-session session-store "id1")
        (store/read-session session-store "id1") => nil))

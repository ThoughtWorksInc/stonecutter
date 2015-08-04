(ns stonecutter.test.db.confirmation
  (:require [midje.sweet :refer :all]
            [stonecutter.db.storage :as s]
            [stonecutter.db.confirmation :as confirmation]
            [stonecutter.db.storage :as storage]))

(facts "about storage of confirmations"
       (s/setup-in-memory-stores!)
       (fact "can store a confirmation"
             (confirmation/store! @storage/confirmation-store  "user@email.com" "confirmation-id")
             => {:login "user@email.com"
                           :confirmation-id "confirmation-id"})
       (fact "can retrieve a confirmation once it has been stored"
             (confirmation/store! @storage/confirmation-store "user12@email.com" "confirmation-id12")
             (confirmation/fetch @storage/confirmation-store "confirmation-id12")
             => {:login "user12@email.com"
                 :confirmation-id "confirmation-id12"}))


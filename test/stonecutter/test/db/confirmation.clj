(ns stonecutter.test.db.confirmation
  (:require [midje.sweet :refer :all]
            [stonecutter.db.storage :as s]
            [stonecutter.db.confirmation :as confirmation]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.mongo :as m]))

(facts "about storage of confirmations"
       (let [confirmation-store (m/create-memory-store)]
         (fact "can store a confirmation"
               (confirmation/store! confirmation-store "user@email.com" "confirmation-id")
               => {:login           "user@email.com"
                   :confirmation-id "confirmation-id"})
         (fact "can retrieve a confirmation once it has been stored"
               (confirmation/store! confirmation-store "user12@email.com" "confirmation-id12")
               (confirmation/fetch confirmation-store "confirmation-id12")
               => {:login           "user12@email.com"
                   :confirmation-id "confirmation-id12"})))


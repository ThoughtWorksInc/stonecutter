(ns stonecutter.test.db.confirmation
  (:require [midje.sweet :refer :all]
            [stonecutter.db.storage :as s]
            [stonecutter.db.confirmation :as confirmation]))

(facts "about storage of confirmations"
       (s/setup-in-memory-stores!)
       (fact "can store a confirmation"
             (confirmation/store! "user@email.com" "confirmation-id")
             => {:login "user@email.com"
                           :confirmation-id "confirmation-id"})
       (fact "can retrieve a confirmation once it has been stored"
             (confirmation/store! "user12@email.com" "confirmation-id12")
             (confirmation/fetch "confirmation-id12") 
             => {:login "user12@email.com"
                 :confirmation-id "confirmation-id12"}))


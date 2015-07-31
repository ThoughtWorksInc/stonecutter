(ns stonecutter.test.db.confirmation
  (:require [midje.sweet :refer :all]
            [stonecutter.db.storage :as s]
            [stonecutter.db.confirmation :as confirmation]))

(future-facts "about storage of confirmations"
       (s/setup-in-memory-stores!)
       (fact "can store a confirmation"
             (confirmation/store! "user@email.com" "confirmation-id")
             => (contains {:login "user@email.com"
                           :confirmation-id "confirmation-id"})))

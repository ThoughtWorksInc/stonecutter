(ns stonecutter.test.db.invitations
  (:require [midje.sweet :refer :all]
            [stonecutter.db.invitations :as i]
            [stonecutter.db.mongo :as m]
            [clauth.store :as cl-store]
            [stonecutter.util.time :as time]
            [stonecutter.test.util.time :as test-time]))

(def invitation-store (m/create-memory-store))
(def test-clock (test-time/new-stub-clock 0))

(background
  (before :facts (cl-store/reset-store! invitation-store)))

(facts "can store an invite with generated unique invite-id"
       (let [email-1 "user@usersemail.co.uk"
             email-2 "user2@usersemail.co.uk"
             expiry-days 7
             invite-id-1 (i/generate-invite-id! invitation-store email-1 test-clock expiry-days)
             invite-id-2 (i/generate-invite-id! invitation-store email-2 test-clock expiry-days)]
         (i/fetch-by-id invitation-store invite-id-1) => (just {:email   email-1 :invite-id invite-id-1
                                                                :_expiry (* expiry-days time/day)})
         invite-id-1 =not=> invite-id-2))

(fact "can delete an invite"
             (let [email-1 "user@usersemail.co.uk"
                   expiry-days 7
                   invite-id-1 (i/generate-invite-id! invitation-store email-1 test-clock expiry-days)]
               (i/fetch-by-id invitation-store invite-id-1) => (just {:email email-1 :invite-id invite-id-1
                                                                      :_expiry (* expiry-days time/day)})
               (i/remove-invite! invitation-store invite-id-1)
               (i/fetch-by-id invitation-store invite-id-1) => nil))
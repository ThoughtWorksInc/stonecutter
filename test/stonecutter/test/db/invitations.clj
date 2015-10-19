(ns stonecutter.test.db.invitations
  (:require [midje.sweet :refer :all]
            [stonecutter.db.invitations :as i]
            [stonecutter.db.mongo :as m]
            [clauth.store :as cl-store]))

(def invitation-store (m/create-memory-store))

(background
  (before :facts (cl-store/reset-store! invitation-store)))

(facts "can store an invite with generated unique invite-id"
       (let [email-1 "user@usersemail.co.uk"
             email-2 "user2@usersemail.co.uk"
             invite-id-1 (i/generate-invite-id! invitation-store email-1)
             invite-id-2 (i/generate-invite-id! invitation-store email-2)]
         (i/fetch-by-id invitation-store invite-id-1) => (just {:email email-1 :invite-id invite-id-1})
         invite-id-1 =not=> invite-id-2))

(fact "can delete an invite"
             (let [email-1 "user@usersemail.co.uk"
                   invite-id-1 (i/generate-invite-id! invitation-store email-1)]
               (i/fetch-by-id invitation-store invite-id-1) => (just {:email email-1 :invite-id invite-id-1})
               (i/remove-invite! invitation-store invite-id-1)
               (i/fetch-by-id invitation-store invite-id-1) => nil))
(ns stonecutter.test.db.forgotten-password
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [clauth.store :as cl-store]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.forgotten-password :as fp]
            [stonecutter.test.util.time :as test-time]
            ))

(def now 12345678)
(def day-in-millis (* 24 60 60 1000))
(def tomorrow (+ now day-in-millis))
(def test-clock (test-time/new-stub-clock now))

(facts "about storing id for user"
       (let [forgotten-password-store (m/create-memory-store)
             forgotten-password-id "abcdefggasdf"
             login "bob@burgers.com"]
         (fp/store-id-for-user! forgotten-password-store test-clock forgotten-password-id login)
         (cl-store/entries forgotten-password-store) => [{:forgotten-password-id forgotten-password-id
                                                          :login                 login
                                                          :expiry                tomorrow}]))

(facts "about fetching forgotten password doc by id"
       (fact "can fetch single doc by username"
             (let [forgotten-password-store (m/create-memory-store)
                   forgotten-password-id "asdf"
                   login "bob@burgers.com"]
               (fp/store-id-for-user! forgotten-password-store test-clock forgotten-password-id login)
               (fp/forgotten-password-doc-by-login forgotten-password-store login) => {:forgotten-password-id forgotten-password-id
                                                                                       :login                 login
                                                                                       :expiry                tomorrow}
               ))
       (fact "if there are no matching docs then nil is returned"
             (let [forgotten-password-store (m/create-memory-store)]
               (fp/forgotten-password-doc-by-login forgotten-password-store "a@a.com") => nil))
       (fact "if there are multiple matching docs then exception is thrown"
             (let [forgotten-password-store (m/create-memory-store)
                   login "bob@burgers.com"]
               (fp/store-id-for-user! forgotten-password-store test-clock "id1" login)
               (fp/store-id-for-user! forgotten-password-store test-clock "id2" login)
               (fp/forgotten-password-doc-by-login forgotten-password-store login) => (throws Exception))))
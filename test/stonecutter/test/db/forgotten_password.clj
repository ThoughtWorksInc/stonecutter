(ns stonecutter.test.db.forgotten-password
  (:require [midje.sweet :refer :all]
            [clauth.store :as cl-store]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.forgotten-password :as fp]
            [stonecutter.test.util.time :as test-time]
            [stonecutter.util.time :as time]))

(def test-clock (test-time/new-stub-clock 0))

(facts "about storing id for user"
       (let [forgotten-password-store (m/create-memory-store)
             forgotten-password-id "abcdefggasdf"
             login "bob@burgers.com"
             expiry-hours 7]
         (fp/store-id-for-user! forgotten-password-store test-clock forgotten-password-id login expiry-hours)
         (cl-store/entries forgotten-password-store) => [{:forgotten-password-id forgotten-password-id
                                                          :login                 login
                                                          :_expiry               (* expiry-hours time/hour)}]))

(facts "about fetching forgotten password doc by id"
       (fact "can fetch single doc by username"
             (let [forgotten-password-store (m/create-memory-store)
                   forgotten-password-id "asdf"
                   login "bob@burgers.com"]
               (fp/store-id-for-user! forgotten-password-store test-clock forgotten-password-id login 240)
               (fp/forgotten-password-doc-by-login forgotten-password-store test-clock login) => {:forgotten-password-id forgotten-password-id
                                                                                                  :login                 login}
               ))
       (fact "if there are no matching docs then nil is returned"
             (let [forgotten-password-store (m/create-memory-store)]
               (fp/forgotten-password-doc-by-login forgotten-password-store test-clock "a@a.com") => nil))
       (fact "if there are multiple matching docs then exception is thrown"
             (let [forgotten-password-store (m/create-memory-store)
                   login "bob@burgers.com"]
               (fp/store-id-for-user! forgotten-password-store test-clock "id1" login 24)
               (fp/store-id-for-user! forgotten-password-store test-clock "id2" login 24)
               (fp/forgotten-password-doc-by-login forgotten-password-store test-clock login) => (throws Exception))))
(ns stonecutter.test.db.forgotten-password
  (:require [midje.sweet :refer :all]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.forgotten-password :as fp]
            [clauth.store :as cl-store]))

(facts "about storing id for user"
       (let [forgotten-password-store (m/create-memory-store)
             forgotten-password-id "abcdefggasdf"
             login "bob@burgers.com"]
         (fp/store-id-for-user! forgotten-password-store forgotten-password-id login)
         (cl-store/entries forgotten-password-store) => [{:forgotten-password-id forgotten-password-id
                                                          :login login}]))
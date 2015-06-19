(ns stonecutter.test.storage
  (:require [midje.sweet :refer :all]
            [clauth.user :as user-store]
            [stonecutter.storage :as s]))

(facts "about is-duplicate-user?"
       (fact "unique username in not a duplicate"
             (s/is-duplicate-user? "john") => false)
       (fact "duplicate username is a duplicate"
             (s/store-user! "valid@email.com" "password")
             (s/is-duplicate-user? "valid@email.com") => true
             (s/is-duplicate-user? "VALID@EMAIL.COM") => true))

(fact "can save user into user storage"
      (s/store-user! "email@server.com" "password")
      (-> (user-store/fetch-user "email@server.com") :login) => "email@server.com"
      (fact "the email is always lower-cased"
            (s/store-user! "NEWEMAIL@A.COM" "password")
            (-> (user-store/fetch-user "newemail@a.com") :login) => "newemail@a.com"))
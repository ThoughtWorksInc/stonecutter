(ns stonecutter.test.storage
  (:require [midje.sweet :refer :all]
            [clauth.user :as user-store]
            [clauth.auth-code :as auth-code-store]
            [stonecutter.storage :as s]))

(facts "about is-duplicate-user?"
       (fact "unique email in not a duplicate"
             (s/is-duplicate-user? "unique@email.com") => false
             (provided
               (user-store/fetch-user "unique@email.com") => nil))

       (fact "duplicate email is a duplicate"
             (s/is-duplicate-user? "valid@email.com") => true
             (provided
               (user-store/fetch-user "valid@email.com") => :a-user))

       (fact "the email is always lower-cased"
             (s/is-duplicate-user? "VALID@EMAIL.COM") => true
             (provided
               (user-store/fetch-user "valid@email.com") => :a-user)))

(def a-user {:login "email@server.com" :name nil :url nil})

(facts "about storing users"
       (fact "users are stored in the user-store"
             (s/store-user! "email@server.com" "password") => a-user
             (provided
               (user-store/new-user "email@server.com" "password") => ...user...
               (user-store/store-user ...user...) => a-user))

       (fact "the email is always lower-cased"
             (s/store-user! "UPPER@CASE.COM" "password") => a-user
             (provided
               (user-store/new-user "upper@case.com" "password") => ...user...
               (user-store/store-user ...user...) => a-user))

       (fact "password is removed before returning user"
             (-> (s/store-user! "email@server.com" "password")
                 :password) => nil
             (provided
               (user-store/new-user "email@server.com" "password") => ...user...
               (user-store/store-user ...user...) => {:password "hashedAndSaltedPassword"})))

(facts "about authenticating and retrieving users"
       (fact "with valid credentials"
             (s/authenticate-and-retrieve-user "email@server.com" "password") => a-user
             (provided
               (user-store/authenticate-user "email@server.com" "password") => a-user))

       (fact "password is removed before returning user"
             (-> (s/authenticate-and-retrieve-user "email@server.com" "password")
                 :password) => nil
             (provided
               (user-store/authenticate-user "email@server.com" "password") => {:password "hashedAndSaltedPassword"}))

       (fact "with invalid credentials returns nil"
             (s/authenticate-and-retrieve-user "invalid@credentials.com" "password") => nil
             (provided
               (user-store/authenticate-user "invalid@credentials.com" "password") => nil)))

(fact "can retrieve user using auth-code"
      (let [auth-code-record (auth-code-store/create-auth-code ...client... ...user... ...redirect-uri...)]
        (s/retrieve-user-with-auth-code "code") => ...user...
        (provided
          (auth-code-store/fetch-auth-code "code") => auth-code-record)))

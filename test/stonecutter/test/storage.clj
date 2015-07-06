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

(facts "about storing users"
       (fact "users are stored in the user-store"
             (s/store-user! "email@server.com" "password") => :a-stored-user
             (provided
               (user-store/new-user "email@server.com" "password") => :a-user
               (user-store/store-user :a-user) => :a-stored-user))

       (fact "the email is always lower-cased"
             (s/store-user! "UPPER@CASE.COM" "password") => :a-stored-user
             (provided
               (user-store/new-user "upper@case.com" "password") => :a-user
               (user-store/store-user :a-user) => :a-stored-user)))

(fact "can retrieve user with valid credentials"
      (s/authenticate-and-retrieve-user "email@server.com" "password") => (contains {:login "email@server.com"})
      (provided
        (user-store/authenticate-user "email@server.com" "password") => {:login "email@server.com"
                                                                         :name nil
                                                                         :password anything
                                                                         :url nil}))

(fact "retrieving user with invalid credentials returns nil"
      (s/authenticate-and-retrieve-user "invalid@credentials.com" "password") => nil
      (provided
        (user-store/authenticate-user "invalid@credentials.com" "password") => nil))

(fact "can retrieve user using auth-code"
      (let [auth-code-record (auth-code-store/create-auth-code ...client... ...user... ...redirect-uri...)]
        (s/retrieve-user-with-auth-code "code") => ...user...
        (provided
          (auth-code-store/fetch-auth-code "code") => auth-code-record)))

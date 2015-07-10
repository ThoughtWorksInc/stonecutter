(ns stonecutter.test.db.storage
  (:require [midje.sweet :refer :all]
            [clauth.user :as cl-user]
            [clauth.client :as cl-client]
            [clauth.auth-code :as cl-auth-code]
            [stonecutter.client :as client]
            [stonecutter.db.storage :as s]))

(fact "can store, authenticate/retrieve and delete users - in memory store is used here"
      (s/setup-in-memory-stores!)
      (s/store-user! "email@server.com" "password") => (contains {:login "email@server.com"
                                                                  :name nil
                                                                  :url nil})
      (s/authenticate-and-retrieve-user "email@server.com" "password") => (contains {:login "email@server.com"
                                                                                     :name nil
                                                                                     :url nil})
      (s/delete-user! "email@server.com") => {}
      (s/authenticate-and-retrieve-user "email@server.com" "password") => nil)

(facts "about is-duplicate-user?"
       (fact "unique email in not a duplicate"
             (s/is-duplicate-user? "unique@email.com") => false
             (provided
               (cl-user/fetch-user "unique@email.com") => nil))

       (fact "duplicate email is a duplicate"
             (s/is-duplicate-user? "valid@email.com") => true
             (provided
               (cl-user/fetch-user "valid@email.com") => :a-user))

       (fact "the email is always lower-cased"
             (s/is-duplicate-user? "VALID@EMAIL.COM") => true
             (provided
               (cl-user/fetch-user "valid@email.com") => :a-user)))

(def a-user {:login "email@server.com" :name nil :url nil})

(fact "about creating a user record"
      (let [id-gen (constantly "id")]
        (fact "a uuid is added"
              (s/create-user id-gen "email" "password") => {:login "email" :password "encrypted_password" :uid "id" :name nil :url nil}
              (provided (cl-user/bcrypt "password") => "encrypted_password"))
        (fact "email is lower-cased"
              (s/create-user id-gen "EMAIL" "password") => (contains {:login "email"}))))

(facts "about storing users"
       (fact "users are stored in the user-store"
             (s/store-user! "email@server.com" "password") => a-user
             (provided
               (s/create-user s/uuid "email@server.com" "password") => ...user...
               (cl-user/store-user ...user...) => a-user))

       (fact "password is removed before returning user"
             (-> (s/store-user! "email@server.com" "password")
                 :password) => nil
             (provided
               (s/create-user s/uuid "email@server.com" "password") => ...user...
               (cl-user/store-user ...user...) => {:password "hashedAndSaltedPassword"})))

(facts "about authenticating and retrieving users"
       (fact "with valid credentials"
             (s/authenticate-and-retrieve-user "email@server.com" "password") => a-user
             (provided
               (cl-user/authenticate-user "email@server.com" "password") => a-user))

       (fact "password is removed before returning user"
             (-> (s/authenticate-and-retrieve-user "email@server.com" "password")
                 :password) => nil
             (provided
               (cl-user/authenticate-user "email@server.com" "password") => {:password "hashedAndSaltedPassword"}))

       (fact "with invalid credentials returns nil"
             (s/authenticate-and-retrieve-user "invalid@credentials.com" "password") => nil
             (provided
               (cl-user/authenticate-user "invalid@credentials.com" "password") => nil)))

(fact "can retrieve user using auth-code"
      (let [auth-code-record (cl-auth-code/create-auth-code ...client... ...user... ...redirect-uri...)]
        (s/retrieve-user-with-auth-code "code") => ...user...
        (provided
          (cl-auth-code/fetch-auth-code "code") => auth-code-record)))

(fact "can retrieve client using client-id"
      (let [client-entry {:name           "name"
                          :client-id      "client-id"
                          :client-secret  "client-secret"
                          :url            nil}]
        (cl-client/store-client client-entry)
        (client/retrieve-client-with-client-id "client-id") => client-entry))

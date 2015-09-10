(ns stonecutter.test.db.user
  (:require [midje.sweet :refer :all]
            [clauth.user :as cl-user]
            [clauth.auth-code :as cl-auth-code]
            [stonecutter.db.mongo :as m]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.config :as config]
            [stonecutter.db.user :as user]))

(def user-store (m/create-memory-store))

(facts "about storage of users - user storage journey"
       (fact "can store a user"
             (user/store-user! user-store "email@server.com" "password")
             => (just {:login      "email@server.com"
                       :name       nil
                       :url        nil
                       :confirmed? false
                       :uid        anything
                       :role       (:default config/roles)}))

       (fact "can authenticate a user"
             (user/authenticate-and-retrieve-user user-store "email@server.com" "password")
             => (contains {:login "email@server.com"
                           :name  nil
                           :url   nil}))

       (fact "can retrieve a user"
             (user/retrieve-user user-store "email@server.com")
             => (contains {:login "email@server.com"
                           :name  nil
                           :url   nil}))

       (fact "can confirm user's account"
             (let [confirmed-user-record (->> (user/store-user! user-store "email@server.com" "password")
                                              (user/confirm-email! user-store))]
               confirmed-user-record =not=> (contains {:confirmation-id anything})
               confirmed-user-record => (contains {:confirmed? true})))

       (fact "can add authorised client for user"
             (user/add-authorised-client-for-user! user-store "email@server.com" "a-client-id")
             => (contains {:login              "email@server.com"
                           :name               nil
                           :url                nil
                           :authorised-clients ["a-client-id"]}))

       (fact "can remove authorised client for user"
             (user/remove-authorised-client-for-user! user-store "email@server.com" "a-client-id")
             => (contains {:login              "email@server.com"
                           :name               nil
                           :url                nil
                           :authorised-clients []}))

       (fact "can change user's password"
             (user/change-password! user-store "email@server.com" "new-password")
             (user/authenticate-and-retrieve-user user-store "email@server.com" "password") => nil
             (user/authenticate-and-retrieve-user user-store "email@server.com" "new-password")
             => (contains {:login "email@server.com" :name nil :url nil}))


       (fact "can delete a user"
             (user/delete-user! user-store "email@server.com") => {}
             (user/authenticate-and-retrieve-user user-store "email@server.com" "password") => nil))

(facts "about retrieving all users"
       (fact "can retrieve all users"
             (let [user-store (m/create-memory-store)
                   user-1 (user/store-user! user-store "email1@server.com" "password1")
                   user-2 (user/store-user! user-store "email2@server.com" "password2")
                   user-3 (user/store-user! user-store "email3@server.com" "password3")]
               (user/retrieve-users user-store) => (contains [user-1 user-2 user-3] :in-any-order)))

       (fact "users are retrieved without password hashes"
             (let [user-store (m/create-memory-store)
                   _user (user/store-user! user-store "email@email.com" "password")]
               (first (user/retrieve-users user-store)) =not=> (contains {:password anything}))))

(facts "about is-duplicate-user?"
       (let [user-store (m/create-memory-store)]
         (user/store-user! user-store "valid@email.com" "1234")
         (fact "non-existant email in not a duplicate"
               (user/user-exists? user-store "unique@email.com") => false)

         (fact "duplicate email is a duplicate"
               (user/user-exists? user-store "valid@email.com") => true)

         (fact "test is case-ignorant"
               (user/user-exists? user-store "VALID@EMAIL.COM") => true)))

(fact "about creating a user record"
      (let [id-gen (constantly "id")]
        (fact "a uuid is added"
              (user/create-user id-gen "email" "password") => {:login "email" :password "encrypted_password" :uid "id" :name nil :url nil :confirmed? false :role (:default config/roles)}
              (provided (cl-user/bcrypt "password") => "encrypted_password"))
        (fact "email is lower-cased"
              (user/create-user id-gen "EMAIL" "password") => (contains {:login "email"}))))

(facts "about storing users"
       (let [user-store (m/create-memory-store)]
         (fact "users are stored in the user-store"
               (user/retrieve-user user-store "email@server.com") => nil
               (user/store-user! user-store "email@server.com" "password") => (contains {:login "email@server.com"})
               (user/retrieve-user user-store "email@server.com") => (contains {:login "email@server.com"}))

         (fact "password is removed before returning user"
               (-> (user/store-user! user-store "email@server.com" "password") :password) => nil)))

(facts "about authenticating and retrieving users"
       (let [user-store (m/create-memory-store)]
         (fact "with valid credentials"
               (user/store-user! user-store "email@server.com" "password")
               (let [user (user/authenticate-and-retrieve-user user-store "email@server.com" "password")]
                 user => (contains {:login "email@server.com"})
                 (fact "password is removed before returning user"
                       (:password user) => nil)))
         (fact "authentication is case-insensitive"
               (user/store-user! user-store "email2@server.com" "password")
               (user/authenticate-and-retrieve-user user-store "EMAIL2@server.COM" "password") => (contains {:login "email2@server.com"}))

         (fact "with invalid credentials returns nil"
               (user/authenticate-and-retrieve-user user-store "email@server.com" "wrong-password") => nil)))

(fact "can retrieve user without authentication"
      (let [user-store (m/create-memory-store)]
        (user/store-user! user-store "email@server.com" "password")
        (user/retrieve-user user-store "email@server.com") => (contains {:login "email@server.com"})
        (fact "retrieval is case insensitive"
              (user/retrieve-user user-store "EMAIL@SERVER.COM") => (contains {:login "email@server.com"}))))

(fact "can retrieve user using auth-code"
      (let [auth-code-store (m/create-memory-store)
            auth-code-record (cl-auth-code/create-auth-code auth-code-store ...client... ...user... ...redirect-uri...)]
        (user/retrieve-user-with-auth-code auth-code-store "code") => ...user...
        (provided
          (cl-auth-code/fetch-auth-code auth-code-store "code") => auth-code-record)))

(facts "about adding client ids to users with add-client-id"
       (fact "returns a function which adds client-id to a user's authorised clients"
             (let [client-id "client-id"
                   add-client-id-function (user/add-client-id client-id)
                   user {:some-key "some-value"}]
               (add-client-id-function user) => {:some-key "some-value" :authorised-clients [client-id]}))

       (fact "does not add duplicates"
             (let [client-id "client-id"
                   add-client-id-function (user/add-client-id client-id)
                   user {:some-key "some-value"}]
               (-> user
                   add-client-id-function
                   add-client-id-function) => {:some-key "some-value" :authorised-clients [client-id]}))

       (fact "removes any duplicates"
             (let [client-id "client-id"
                   add-client-id-function (user/add-client-id client-id)
                   user {:some-key           "some-value"
                         :authorised-clients [client-id client-id]}]
               (-> user
                   add-client-id-function
                   add-client-id-function) => {:some-key           "some-value"
                                               :authorised-clients [client-id]})))

(facts "about removing client ids from users with remove-client-id"
       (fact "returns a function which removes client-id from a user's authorised clients"
             (let [client-id "client-id"
                   user {:some-key           "some-value"
                         :authorised-clients [client-id "another-client-id"]}
                   remove-client-id-function (user/remove-client-id client-id)]
               (remove-client-id-function user) => {:some-key           "some-value"
                                                    :authorised-clients ["another-client-id"]})))

(facts "about is-authorised-client-for-user?"
       (fact "returns true if client-id is in the users authorised-clients list"
             (user/is-authorised-client-for-user? ...user-store... ...email... ...client-id...) => true
             (provided
               (user/retrieve-user ...user-store... ...email...) => {:authorised-clients [...client-id...]}))

       (fact "returns false if client-id is in not in the users authorised-clients list"
             (user/is-authorised-client-for-user? ...user-store... ...email... ...client-id...) => false
             (provided
               (user/retrieve-user ...user-store... ...email...) => {:authorised-clients [...a-different-client-id...]})))

(facts "about changing password"
       (fact "update-password returns a function that hashes and updates the user's password"
             (let [password "new-raw-password"
                   update-password-function (user/update-password password)
                   user {:password "current-hashed-password" :some-key "some-value"}]
               (update-password-function user) => {:password "new-hashed-password" :some-key "some-value"}
               (provided
                 (cl-user/bcrypt "new-raw-password") => "new-hashed-password"))))

(facts "about admins"
       (fact "creating an admin user includes the role admin"
             (let [email "email@admin.com"
                   password "stubpassword"
                   hashed-password "ABE1234SJD1234"
                   id "random-uuid-1234"
                   id-gen (constantly id)]

               (user/create-admin id-gen email password) => {:login      email
                                                             :password   hashed-password
                                                             :confirmed? false
                                                             :uid        id
                                                             :role       (:admin config/roles)}
               (provided
                 (cl-user/new-user email password) => {:login email :password hashed-password})))

       (let [admin-login "admin@email.com"
             password "password456"
             hashed-password "PA134SN"]
         (fact "storing an admin calls create admin user"
               (against-background
                 (user/create-admin anything admin-login password) => {:login admin-login :password hashed-password :role (:admin config/roles)})

               (user/store-admin! user-store admin-login password)
               (user/retrieve-user user-store admin-login) => {:login    admin-login
                                                               :password hashed-password
                                                               :role     (:admin config/roles)})))

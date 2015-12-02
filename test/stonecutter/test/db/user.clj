(ns stonecutter.test.db.user
  (:require [midje.sweet :refer :all]
            [clauth.user :as cl-user]
            [clauth.auth-code :as cl-auth-code]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.config :as config]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as user]))

(def user-store (m/create-memory-store))

(facts "about storage of users - user storage journey"
       (fact "can store a user"
             (user/store-user! user-store "Frank" "Lasty" "email@server.com" "password")
             => (just {:login           "email@server.com"
                       :first-name      "Frank"
                       :last-name       "Lasty"
                       :url             nil
                       :confirmed?      false
                       :uid             anything
                       :role            (:untrusted config/roles)
                       :profile-picture config/default-profile-picture}))

       (fact "can authenticate a user"
             (user/authenticate-and-retrieve-user user-store "email@server.com" "password")
             => (contains {:login "email@server.com"
                           :first-name "Frank"
                           :last-name  "Lasty"
                           :url   nil}))

       (fact "can retrieve a user"
             (user/retrieve-user user-store "email@server.com")
             => (contains {:login "email@server.com"
                           :first-name "Frank"
                           :last-name  "Lasty"
                           :url   nil}))

       (fact "can confirm user's account"
             (let [confirmed-user-record (->> (user/store-user! user-store "Frank" "Lasty" "email@server.com" "password")
                                              (user/confirm-email! user-store))]
               confirmed-user-record =not=> (contains {:confirmation-id anything})
               confirmed-user-record => (contains {:confirmed? true})))

       (fact "can add authorised client for user"
             (user/add-authorised-client-for-user! user-store "email@server.com" "a-client-id")
             => (contains {:login              "email@server.com"
                           :first-name         "Frank"
                           :last-name          "Lasty"
                           :url                nil
                           :authorised-clients ["a-client-id"]}))

       (fact "can remove authorised client for user"
             (user/remove-authorised-client-for-user! user-store "email@server.com" "a-client-id")
             => (contains {:login              "email@server.com"
                           :first-name         "Frank"
                           :last-name          "Lasty"
                           :url                nil
                           :authorised-clients []}))

       (fact "can change user's password"
             (user/change-password! user-store "email@server.com" "new-password")
             (user/authenticate-and-retrieve-user user-store "email@server.com" "password") => nil
             (user/authenticate-and-retrieve-user user-store "email@server.com" "new-password")
             => (contains {:login "email@server.com"}))


       (fact "can delete a user"
             (user/retrieve-user user-store "email@server.com") =not=> nil
             (user/delete-user! user-store "email@server.com") => {}
             (user/retrieve-user user-store "email@server.com") => nil)

       (fact "deletion is case-insensitive"
             (th/store-user! user-store "email@server.com" "password")
             (user/delete-user! user-store "EMAIL@SERVER.COM")
             (user/retrieve-user user-store "email@server.com") => nil))

(facts "about retrieving all users"
       (fact "can retrieve all users"
             (let [user-store (m/create-memory-store)
                   user-1 (th/store-user! user-store "email1@server.com" "password1")
                   user-2 (th/store-user! user-store "email2@server.com" "password2")
                   user-3 (th/store-user! user-store "email3@server.com" "password3")]
               (user/retrieve-users user-store) => (contains [user-1 user-2 user-3] :in-any-order)))

       (fact "users are retrieved without password hashes"
             (let [user-store (m/create-memory-store)
                   _user (th/store-user! user-store "email@email.com" "password")]
               (first (user/retrieve-users user-store)) =not=> (contains {:password anything}))))

(facts "about is-duplicate-user?"
       (let [user-store (m/create-memory-store)]
         (th/store-user! user-store "valid@email.com" "1234")
         (fact "non-existant email in not a duplicate"
               (user/user-exists? user-store "unique@email.com") => false)

         (fact "duplicate email is a duplicate"
               (user/user-exists? user-store "valid@email.com") => true)

         (fact "test is case-ignorant"
               (user/user-exists? user-store "VALID@EMAIL.COM") => true)))

(fact "about creating a user record"
      (let [id-gen (constantly "id")]
        (fact "email is lower-cased"
              (user/create-user id-gen "first-name" "last-name" "EMAIL" "password" (:untrusted config/roles) config/default-profile-picture) => (contains {:login "email"}))))

(facts "about storing users"
       (let [user-store (m/create-memory-store)]
         (fact "users are stored in the user-store"
               (user/retrieve-user user-store "email@server.com") => nil
               (th/store-user! user-store "email@server.com" "password") => (contains {:login "email@server.com"})
               (user/retrieve-user user-store "email@server.com") => (contains {:login "email@server.com"}))

         (fact "password is removed before returning user"
               (-> (th/store-user! user-store "email@server.com" "password") :password) => nil)))

(facts "about authenticating and retrieving users"
       (let [user-store (m/create-memory-store)]
         (fact "with valid credentials"
               (th/store-user! user-store "email@server.com" "password")
               (let [user (user/authenticate-and-retrieve-user user-store "email@server.com" "password")]
                 user => (contains {:login "email@server.com"})
                 (fact "password is removed before returning user"
                       (:password user) => nil)))
         (fact "authentication is case-insensitive"
               (th/store-user! user-store "email2@server.com" "password")
               (user/authenticate-and-retrieve-user user-store "EMAIL2@server.COM" "password") => (contains {:login "email2@server.com"}))

         (fact "with invalid credentials returns nil"
               (user/authenticate-and-retrieve-user user-store "email@server.com" "wrong-password") => nil)))

(fact "can retrieve user without authentication"
      (let [user-store (m/create-memory-store)]
        (th/store-user! user-store "email@server.com" "password")
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

(facts "about updating user role"
       (fact "update-user-role returns a function that changes user role accordingly"
             (let [user {:login "email@email.com" :role "untrusted"}
                   update-role-fn (user/update-user-role "trusted")]

               (update-role-fn user) => {:login "email@email.com" :role "trusted"})))

(facts "about updating user email"
       (fact "update-user-email changes user email accordingly"
             (let [user-store (m/create-memory-store)
                   email "current-email@email.com"
                   user (th/store-user! user-store email "password")
                   new-email "new-email@email.com"
                   new-user (assoc user :login new-email)]
               (user/update-user-email! user-store email new-email) => new-user
               (user/retrieve-user user-store email) => nil
               (user/retrieve-user user-store new-email) =not=> nil
               ))


       (fact "when email is updated it is no longer confirmed"
             (let [user-store (m/create-memory-store)
                   email "current-email@email.com"
                   user (th/store-user! user-store email "password")
                   new-email "new-email@email.com"
                   new-user (assoc user :login new-email :confirmed? false)]

               (user/confirm-email! user-store user)
               (user/update-user-email! user-store email new-email) => new-user)))

(facts "about admins"
       (fact "creating an admin user includes the role admin"
             (let [email "email@admin.com"
                   password "stubpassword"
                   hashed-password "ABE1234SJD1234"
                   id "random-uuid-1234"
                   id-gen (constantly id)]

               (user/create-admin id-gen "admin-first-name" "admin-last-name" email password) => {:first-name      "admin-first-name"
                                                                                                  :last-name       "admin-last-name"
                                                                                                  :login           email
                                                                                                  :password        hashed-password
                                                                                                  :confirmed?      false
                                                                                                  :uid             id
                                                                                                  :role            (:admin config/roles)
                                                                                                  :profile-picture config/default-profile-picture}
               (provided
                 (cl-user/new-user email password) => {:login email :password hashed-password})))

       (let [admin-login "admin@email.com"
             password "password456"
             hashed-password "PA134SN"
             admin-first-name "first name"
             admin-last-name "last name"]
         (fact "storing an admin calls create admin user"
               (user/store-admin! user-store admin-first-name admin-last-name admin-login password) => {:first-name admin-first-name
                                                                                                        :last-name  admin-last-name
                                                                                                        :login      admin-login
                                                                                                        :role       (:admin config/roles)}
               (provided
                 (user/create-admin anything admin-first-name admin-last-name admin-login password) => {:first-name admin-first-name
                                                                                                        :last-name  admin-last-name
                                                                                                        :login      admin-login
                                                                                                        :password   hashed-password
                                                                                                        :role       (:admin config/roles)})
               )))

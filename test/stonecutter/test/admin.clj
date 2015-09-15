(ns stonecutter.test.admin
  (:require [midje.sweet :refer :all]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as u]
            [stonecutter.admin :as admin]))

(def admin-login "admin@stonecutter.com")
(def invalid-admin-login "admin")
(def admin-password "validpassword123")
(def user-store (m/create-memory-store))

(facts "about registering admins"
       (fact "valid login and password stores an admin"
             (admin/create-admin-user {:admin-login      admin-login
                                       :admin-password   admin-password} user-store)
             => :return-value

             (provided
               (u/store-admin! user-store "Mighty" "Admin" admin-login admin-password) => :return-value))

       (fact "duplicating login is not stored"
             (admin/create-admin-user {:admin-first-name ...first-name...
                                       :admin-last-name  ...last-name...
                                       :admin-login      admin-login
                                       :admin-password   admin-password
                                       } user-store)
             => nil

             (provided
               (u/user-exists? user-store admin-login) => true
               (u/store-admin! anything anything anything anything anything) => :return :times 0))

       (fact "login not in email format throws an exception"
             (against-background
               (u/user-exists? user-store invalid-admin-login) => false)

             (admin/create-admin-user
               {:admin-login      invalid-admin-login
                :admin-first-name ...first-name...
                :admin-last-name  ...last-name...
                :admin-password   admin-password} user-store) => (throws Exception)
             (provided
               (u/store-admin! anything anything anything anything anything) => :return :times 0))

       (def string-of-255 (apply str (repeat 255 "x")))

       (tabular
         (fact "password of incorrect length throws an exception"
               (against-background
                 (u/user-exists? user-store ?admin-login) => false)

               (admin/create-admin-user
                 {:admin-login      ?admin-login
                  :admin-first-name ...first-name...
                  :admin-last-name  ...last-name...
                  :admin-password   ?password} user-store) => (throws Exception)
               (provided
                 (u/store-admin! anything anything anything anything anything) => :return :times 0))

        ?admin-login        ?password
        "admin@admin.com"   "short"
        "admin2@admin.com"  string-of-255))


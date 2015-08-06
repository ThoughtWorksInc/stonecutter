(ns stonecutter.test.admin
  (:require [midje.sweet :refer :all]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as u]
            [stonecutter.admin :as admin]))

(def admin-login "admin@stonecutter.com")
(def admin-password "validpassword123")
(def user-store (m/create-memory-store))

(facts "about registering admins"
       (fact "valid login and password stores an admin"
             (admin/create-admin-user {:admin-login admin-login :admin-password admin-password} user-store)
             => :return-value

             (provided
               (u/store-admin! user-store admin-login admin-password) => :return-value))

       (fact "duplicating login is not stored"
             (admin/create-admin-user {:admin-login admin-login :admin-password admin-password} user-store)
             => nil

             (provided
               (u/is-duplicate-user? user-store admin-login) => true
               (u/store-admin! user-store admin-login admin-password) => :return :times 0)))

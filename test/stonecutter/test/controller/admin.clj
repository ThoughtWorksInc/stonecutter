(ns stonecutter.test.controller.admin
  (:require [midje.sweet :refer :all]
            [stonecutter.routes :as routes]
            [stonecutter.controller.admin :as admin]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as user]))

(facts "about show-user-list"
       (fact "reponse body displays the users"
             (let [user-store (m/create-memory-store)
                   _user1 (user/store-user! user-store "user1@email.com" "password1")
                   _user2 (user/store-user! user-store "user2@email.com" "password2")]
               (->> (th/create-request :get (routes/path :show-user-list) nil)
                    (admin/show-user-list user-store)
                    :body)) => (contains #"user1@email.com[\s\S]+user2@email.com")))
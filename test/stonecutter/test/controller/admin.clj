(ns stonecutter.test.controller.admin
  (:require [midje.sweet :refer :all]
            [stonecutter.routes :as routes]
            [stonecutter.controller.admin :as admin]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as user]
            [stonecutter.config :as config]))

(facts "about show-user-list"
       (fact "response body displays the users"
             (let [user-store (m/create-memory-store)
                   _admin (user/store-admin! user-store "admin@email.com" "password")
                   _user1 (user/store-user! user-store "user1@email.com" "password1")
                   _user2 (user/store-user! user-store "user2@email.com" "password2")
                   page (->> (th/create-request :get (routes/path :show-user-list) nil)
                             (admin/show-user-list user-store)
                             :body)]

               page => (contains #"user1@email.com[\s\S]+user2@email.com")
               page =not=> (contains #"admin@email.com")))

       (fact "post to set-user-trustworthiness redirects to show-user-list"
             (let [user-store (m/create-memory-store)
                   request (th/create-request :post (routes/path :set-user-trustworthiness) {:login "user1@email.com"})
                   response (admin/set-user-trustworthiness user-store request)]
               response) => (th/check-redirects-to (routes/path :show-user-list)))

       (fact "post with trust toggle attr changes user's role to trusted"
             (let [user-store (m/create-memory-store)
                   user-email "user1@email.com"
                   _user1 (user/store-user! user-store user-email "password1")
                   request (th/create-request :post (routes/path :set-user-trustworthiness) {:trust-toggle "on" :login user-email})]
               (admin/set-user-trustworthiness user-store request)
               (-> (user/retrieve-user user-store user-email)
                   :role) => (:trusted config/roles)))

       (fact "post without trust toggle attr changes user's role to untrusted"
             (let [user-store (m/create-memory-store)
                   user-email "user2@email.com"
                   _user2 (user/store-user! user-store user-email "password2")
                   request (th/create-request :post (routes/path :set-user-trustworthiness) {:login user-email})]
               (admin/set-user-trustworthiness user-store request)
               (-> (user/retrieve-user user-store user-email)
                   :role) => (:untrusted config/roles))))
(ns stonecutter.test.controller.admin
  (:require [midje.sweet :refer :all]
            [stonecutter.routes :as routes]
            [stonecutter.controller.admin :as admin]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as user]
            [stonecutter.db.client :as client]
            [stonecutter.config :as config]))

(facts "about apps list"
       (fact "response body displays the apps"
             (let [client-store (m/create-memory-store)
                   _admin (user/store-admin! client-store "first name" "last name" "admin@email.com" "password")
                   _client1 (th/store-client! client-store "name-1" "client-id-1" "client-secret-1" "client-url-1")
                   _client2 (th/store-client! client-store "name-2" "client-id-2" "client-secret-2" "client-url-2")
                   page (->> (th/create-request :get (routes/path :show-apps-list) nil)
                             (admin/show-apps-list client-store)
                             :body)]

               page => (contains #"name-1")
               page => (contains #"name-2")))

       (fact "post to create-client with client name and client url creates a client"
             (let [client-store (m/create-memory-store)
                   name "client-name"
                   url "client-url"
                   request (th/create-request :post (routes/path :create-client) {:name name :url url})]
               (admin/create-client client-store request)
               (client/retrieve-clients client-store) => (just (just {:client-id anything :client-secret anything :name name :url url}))))

       (fact "post to create-client redirects to show-apps-list"
             (let [client-store (m/create-memory-store)
                   name "client-name"
                   url "client-url"
                   request (th/create-request :post (routes/path :create-client) {:name name :url url})
                   response (admin/create-client client-store request)]
               response) => (th/check-redirects-to (routes/path :show-apps-list))))

(facts "about show-user-list"
       (fact "response body displays the users"
             (let [user-store (m/create-memory-store)
                   _admin (user/store-admin! user-store "first name" "last name" "admin@email.com" "password")
                   _user1 (th/store-user! user-store "Frank" "Lasty" "user1@email.com" "password1")
                   _user2 (th/store-user! user-store "Frank" "Lasty" "user2@email.com" "password2")
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
                   _user1 (th/store-user! user-store "Frank" "Lasty" user-email "password1")
                   request (th/create-request :post (routes/path :set-user-trustworthiness) {:trust-toggle "on" :login user-email})]
               (admin/set-user-trustworthiness user-store request)
               (-> (user/retrieve-user user-store user-email)
                   :role) => (:trusted config/roles)))

       (fact "post without trust toggle attr changes user's role to untrusted"
             (let [user-store (m/create-memory-store)
                   user-email "user2@email.com"
                   _user2 (th/store-user! user-store user-email "password2")
                   request (th/create-request :post (routes/path :set-user-trustworthiness) {:login user-email})]
               (admin/set-user-trustworthiness user-store request)
               (-> (user/retrieve-user user-store user-email)
                   :role) => (:untrusted config/roles))))

(facts "post will respond with flash message"
       (fact "trusting user sends user-trusted flash message"
             (let [user-store (m/create-memory-store)
                   user-email "user3#email.com"
                   _user3 (th/store-user! user-store user-email "password3")
                   request (th/create-request :post (routes/path :set-user-trustworthiness) {:login user-email :trust-toggle "on"})
                   response (admin/set-user-trustworthiness user-store request)]

               (:flash response) => (contains {:translation-key       :user-trusted
                                               :updated-account-email user-email})))

       (fact "untrusting user sends user-untrusted flash message"
             (let [user-store (m/create-memory-store)
                   user-email "user3#email.com"
                   _user (th/store-user! user-store user-email "password")
                   request (th/create-request :post (routes/path :set-user-trustworthiness) {:login user-email})
                   response (admin/set-user-trustworthiness user-store request)]

               (:flash response) => (contains {:translation-key       :user-untrusted
                                               :updated-account-email user-email}))))

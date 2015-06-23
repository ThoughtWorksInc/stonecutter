(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.handler :refer [app register-user]]
            [clauth.user :as user-store]
            [net.cgrand.enlive-html :as html]
            [stonecutter.storage :as s]))

(defn p [v] (prn v) v)

(defn create-request [method url params]
  (-> (mock/request method url)
      (assoc :params params)))

(defn create-user [login password]
  {:login login
   :password password
   :name nil
   :url nil})

(background (before :facts (user-store/reset-user-store!)))

(fact "registration url returns a 200 response"
      (-> (mock/request :get "/register") app :status) => 200)

(future-fact "sign-in url returns a 200 response"
      (-> (mock/request :get "/sign-in") app :status) => 200)

(fact "unknown url returns a 404 response"
      (-> (mock/request :get "/unknown-url") app :status) => 404)

(fact "user data is saved"
      (let [user-registration-data (create-user "valid@email.com" "password")]
        (-> (create-request :post "/register" {:email "valid@email.com" :password "password" :confirm-password "password"})
            register-user
            :status) => 200

        (provided
          (s/store-user! "valid@email.com" "password") => user-registration-data)))

(fact "same user cannot be created twice"
      (let [user-registration-data (create-user "valid@email.com" "password")]
        (-> (create-request :post "/register" {:email "valid@email.com" :password "password" :confirm-password "password"})
            register-user
            :body) => (contains "You saved the user")

        (-> (create-request :post "/register" {:email "valid@email.com" :password "password" :confirm-password "password"})
            register-user
            :body) => (contains "User already exists")))

(facts "about validation errors"
       (fact "user isn't saved to the database if email is invalid"
             (-> (create-request :post "/register" {:email "invalid"}) register-user) => anything
             (provided
               (user-store/new-user anything anything) => anything :times 0
               (user-store/store-user anything) => anything :times 0))
       (facts "registration page is rendered with errors"
              (let [html-response ( -> (create-request :post "/register" {:email "invalid"})
                                       register-user
                                       :body
                                       html/html-snippet)]
                (fact "email field should have validation error class"
                      (html/select html-response [:.form-row--validation-error]) =not=> empty?)
                (fact "invalid email value should be preserved"
                      (-> (html/select html-response [:.registration-email-input])
                          first
                          :attrs
                          :value) => "invalid"))))

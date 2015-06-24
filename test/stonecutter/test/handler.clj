(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clauth.user :as user-store]
            [net.cgrand.enlive-html :as html]
            [stonecutter.handler :refer :all]
            [stonecutter.validation :as v]
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

(def user-params {:email "valid@email.com" :password "password"})
(def user-confirm-params {:email "valid@email.com" :password "password" :confirm-password "password"})

(background (before :facts (user-store/reset-user-store!)))

(fact "registration url returns a 200 response"
      (-> (mock/request :get "/register") app :status) => 200)

(fact "sign-in url returns a 200 response"
      (-> (mock/request :get "/sign-in") app :status) => 200)

(fact "unknown url returns a 404 response"
      (-> (mock/request :get "/unknown-url") app :status) => 404)

(fact "user data is saved"
      (let [user-registration-data (create-user "valid@email.com" "password")]
        (-> (create-request :post "/register" user-confirm-params)
            register-user
            :status) => 200

        (provided
          (s/store-user! "valid@email.com" "password") => {:email "valid@email.com"})))

(future-fact "user can sign in with valid credentials"
      (-> (create-request :post "/sign-in" user-params)
          sign-in
          :body) => (:email user-params)

      (provided
        (s/retrieve-user "valid@email.com" "password") => {:email "valid@email.com"}))

(future-fact "user cannot sign in with invalid credentials"
      (-> (create-request :post "/sign-in" {:email "invalid@credentials.com" :password "password"})
          sign-in
          :body) => (contains "Invalid email address or password")

      (provided
        (s/retrieve-user "invalid@credentials.com" "password") => nil))

(fact "email must not be a duplicate"
      (-> (create-request :post "/register" user-confirm-params)
          register-user
          :body) => (contains "User already exists")

        (provided
          (v/validate-registration user-confirm-params s/is-duplicate-user?) => {:email :duplicate}
          (user-store/new-user anything anything) => anything :times 0
          (user-store/store-user anything) => anything :times 0))

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

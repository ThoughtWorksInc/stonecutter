(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.handler :refer [app register-user]]
            [clauth.user :as user-store]
            [net.cgrand.enlive-html :as html]
            ))

(defn p [v] (prn v) v)

(defn create-request [method url params]
  (-> (mock/request method url)
      (assoc :params params)))

(fact "registration url returns a 200 response"
      (-> (mock/request :get "/register") app :status) => 200 
      )

(fact "user data is saved"
      (let [user-registration-data {:login "valid@email.com" 
                                    :password "encrypted-password" 
                                    :name nil
                                    :url nil}]
        (-> (mock/request :post "/register") 
            (assoc :params {:email "valid@email.com" :password "password"}) 
            register-user 
            :status) => 200 

        (provided
          (user-store/new-user "valid@email.com" "password") => user-registration-data
          (user-store/store-user user-registration-data) => anything)))


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
                                      html/html-snippet)
                   _ (prn html-response)
                   ]
               (fact "email field should have validation error class" 
                     (html/select html-response [:.form-row--validation-error]) =not=> empty?)  
               (fact "invalid email value should be preserved"
               (-> (html/select html-response [:.registration-email-input])
                   first
                   :attrs
                   :value) => "invalid"))))

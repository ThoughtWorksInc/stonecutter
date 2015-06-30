(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clauth.user :as user-store]
            [net.cgrand.enlive-html :as html]
            [stonecutter.handler :refer :all]
            [stonecutter.validation :as v]
            [stonecutter.storage :as s]
            [clauth.token :as token]
            [clauth.client :as client]))

(defn create-request [method url params]
  (-> (mock/request method url)
      (assoc :params params)
      (assoc-in [:context :translator] {})))

(defn create-user [login password]
  {:login    login
   :password password
   :name     nil
   :url      nil})

(def user-params {:email "valid@email.com" :password "password"})
(def user-confirm-params {:email "valid@email.com" :password "password" :confirm-password "password"})

(background (before :facts (user-store/reset-user-store!)))

(fact "registration url returns a 200 response"
      (-> (mock/request :get "/register") app :status) => 200)

(defn p [v] (prn v) v)

(fact "sign-in url returns a 200 response"
      (-> (mock/request :get "/sign-in") app :status) => 200)

(fact "sign-out url returns a 302 response"
      (-> (mock/request :get "/sign-out") app :status) => 302)

(fact "unknown url returns a 404 response"
      (-> (mock/request :get "/unknown-url") app :status) => 404)

(fact "user can sign in with valid credentials and is redirected to profile, with user added to session"
      (-> (create-request :post "/sign-in" user-params)
          sign-in) => (contains {:status 302 :headers {"Location" "/profile"} :session {:user {:email "valid@email.com"}}})
      (provided
        (s/authenticate-and-retrieve-user "valid@email.com" "password") => {:email "valid@email.com"}))

(fact "if user has session client id, then create an access token and add to user session"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            sign-in) => (contains {:status  302 :headers {"Location" return-to-url}
                                   :session {:user {:email "valid@email.com"} :access_token ...token...}})
        (provided
          (s/authenticate-and-retrieve-user "valid@email.com" "password") => {:email "valid@email.com"}
          (client/fetch-client "client-id") => ...client...
          (token/create-token ...client... "valid@email.com") => {:token ...token...})))

(defn p [v] (prn v) v)

(fact "if user logged out, access token and user email are removed from session"
      (let [return-to-url "/authorisation?client-id=whatever"] 
        (-> (create-request :post "/sign-in" user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            sign-in
            sign-out) =not=> (contains {:session {:user {:email "valid@email.com"} :access_token ...token...}})  
        (provided
          (s/authenticate-and-retrieve-user "valid@email.com" "password") => {:email "valid@email.com"}
          (client/fetch-client "client-id") => ...client...
          (token/create-token ...client... "valid@email.com") => {:token ...token...}))) 

(fact "if user has client id but no return-to in session, throws an exception"
      (-> (create-request :post "/sign-in" user-params)
          (assoc-in [:session :client-id] "client-id")
          sign-in) => (throws Exception)
      (provided
        (s/authenticate-and-retrieve-user "valid@email.com" "password") => {:email "valid@email.com"}))

(fact "if user has invalid client id, then throws an exception"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            sign-in) => (throws Exception)
        (provided
          (s/authenticate-and-retrieve-user "valid@email.com" "password") => {:email "valid@email.com"}
          (client/fetch-client "client-id") => nil)))

(facts "about sign-in validation errors"
       (fact "user cannot sign in with invalid credentials"
             (-> (create-request :post "/sign-in" {:email "invalid@credentials.com" :password "password"})
                 sign-in) => (contains {:status 400})
             (provided
               (s/authenticate-and-retrieve-user "invalid@credentials.com" "password") => nil))
       (facts "sign-in page is rendered with errors when invalid credentials are used"
              (let [html-response (-> (create-request :post "/sign-in" {:email    "invalid@credentials.com"
                                                                        :password "password"})
                                      sign-in
                                      :body
                                      html/html-snippet)]
                (fact "form should include validation error class"
                      (html/select html-response [:.clj--validation-summary__item]) =not=> empty?)
                (fact "email value should be preserved"
                      (-> (html/select html-response [:.clj--email__input])
                          first
                          :attrs
                          :value) => "invalid@credentials.com"))))

(fact "user data is saved"
      (let [user-registration-data (create-user "valid@email.com" "password")]
        (-> (create-request :post "/register" user-confirm-params)
            register-user
            :status) => 200
        (provided
          (s/store-user! "valid@email.com" "password") => {:email "valid@email.com"})))

(fact "email must not be a duplicate"
      (let [html-response (-> (create-request :post "/register" user-confirm-params)
                              register-user
                              :body
                              html/html-snippet)]
       (-> (html/select html-response [:.form-row--validation-error])
           first
           :attrs
           :class)) => (contains "clj--registration-email")  
      (provided
        (v/validate-registration user-confirm-params s/is-duplicate-user?) => {:email :duplicate}
        (user-store/new-user anything anything) => anything :times 0
        (user-store/store-user anything) => anything :times 0))

(facts "about registration validation errors"
       (fact "user isn't saved to the database if email is invalid"
             (-> (create-request :post "/register" {:email "invalid"}) register-user) => anything
             (provided
               (user-store/new-user anything anything) => anything :times 0
               (user-store/store-user anything) => anything :times 0))
       (facts "registration page is rendered with errors"
              (let [html-response (-> (create-request :post "/register" {:email "invalid"})
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

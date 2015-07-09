(ns stonecutter.test.controller.user
  (:require [midje.sweet :refer :all]
            [stonecutter.controller.user :as c]
            [ring.mock.request :as mock]
            [stonecutter.storage :as s]
            [stonecutter.validation :as v]
            [clauth.client :as client]
            [clauth.token :as token]
            [clauth.user :as user-store]
            [clauth.store :as clauth-store]
            [net.cgrand.enlive-html :as html]))

(def email "valid@email.com")
(def password "password")
(def sign-in-user-params {:email email :password password})
(def register-user-params {:email email :password password :confirm-password password})

(defn create-request [method url params]
  (-> (mock/request method url)
      (assoc :params params)
      (assoc-in [:context :translator] {})))

(defn create-user [login password]
  {:login    login
   :password password
   :name     nil
   :url      nil})

(background (before :facts (user-store/reset-user-store!)))

(fact "user can sign in with valid credentials and is redirected to profile, with user and access_token added to session"
      (-> (create-request :post "/sign-in" sign-in-user-params)
          c/sign-in) => (contains {:status 302 :headers {"Location" "/profile"}
                                   :session {:user ...user...
                                             :access_token ...token...}})
      (provided
        (s/authenticate-and-retrieve-user email password) => ...user...
        (token/create-token nil ...user...) => {:token ...token...}))

(fact "user can register with valid credentials and is redirected to profile-created page, with user added to session"
      (-> (create-request :post "/register" register-user-params)
          c/register-user) => (contains {:status 302 :headers {"Location" "/profile-created"}
                                         :session {:user ...user...
                                                   :access_token ...token...}})
      (provided
        (v/validate-registration register-user-params s/is-duplicate-user?) => {}
        (s/store-user! email password) => ...user...
        (token/create-token nil ...user...) => {:token ...token...}))

(fact "signed-in? returns true only when user and access_token are in the session"
      (tabular
        (c/signed-in? ?request) => ?expected-result
       ?request                                                  ?expected-result
       {:session {:user ...user... :access_token ...token...}}   truthy
       {:session {:user nil        :access_token ...token...}}   falsey
       {:session {:user ...user... :access_token nil}}           falsey
       {:session {:user nil        :access_token nil}}           falsey
       {:session {}}                                             falsey
       {:session nil}                                            falsey
       {}                                                        falsey))

(facts "accessing sign-in form"
       (fact "without user and access_token in session shows the sign-in form"
             (-> (create-request :get "/sign-in" nil)
                 c/show-sign-in-form) => (contains {:status 200}))

       (fact "with user and access_token in session redirects to /")
             (-> (create-request :get "/sign-in" nil)
                 (assoc-in [:session :user] ...user...)
                 (assoc-in [:session :access_token] ...token...)
                 c/show-sign-in-form) => (contains {:status 302 :headers {"Location" "/"}
                                                    :session {:user ...user...
                                                              :access_token ...token...}}))

(fact "if user has session client id, then create an access token and add to user session"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" sign-in-user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            c/sign-in) => (contains {:status  302 :headers {"Location" return-to-url}
                                    :session {:access_token ...token... :user ...user...}})
        (provided
          (s/authenticate-and-retrieve-user email password) => ...user...
          (client/fetch-client "client-id") => ...client...
          (token/create-token ...client... ...user...) => {:token ...token...})))

(fact "if user logged out, access token and user email are removed from session"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" sign-in-user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            c/sign-in
            c/sign-out
            :session) => empty?
        (provided
          (s/authenticate-and-retrieve-user email password) => ...user...
          (client/fetch-client "client-id") => ...client...
          (token/create-token ...client... ...user...) => {:token ...token...})))

(fact "if user has client id but no return-to in session, throws an exception"
      (-> (create-request :post "/sign-in" sign-in-user-params)
          (assoc-in [:session :client-id] "client-id")
          c/sign-in) => (throws Exception)
      (provided
        (s/authenticate-and-retrieve-user email password) => ...user...))

(fact "if user has invalid client id, then throws an exception"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" sign-in-user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            c/sign-in) => (throws Exception)
        (provided
          (s/authenticate-and-retrieve-user email password) => ...user...
          (client/fetch-client "client-id") => nil)))

(facts "about sign-in validation errors"
       (fact "user cannot sign in with blank password"
             (-> (create-request :post "/sign-in" {:email "email@credentials.com" :password ""})
                 c/sign-in) => (contains {:status 200}))
       (fact "user cannot sign in with invalid credentials"
             (-> (create-request :post "/sign-in" {:email "invalid@credentials.com" :password "password"})
                 c/sign-in) => (contains {:status 200})
             (provided
               (s/authenticate-and-retrieve-user "invalid@credentials.com" "password") => nil))
       (facts "sign-in page is rendered with errors when invalid credentials are used"
              (let [html-response (-> (create-request :post "/sign-in" {:email    "invalid@credentials.com"
                                                                        :password "password"})
                                      c/sign-in
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
        (-> (create-request :post "/register" register-user-params)
            c/register-user
            :status) => 302
        (provided
          (s/store-user! "valid@email.com" "password") => ...user...)))

(fact "account can be deleted and user is signed-out"
      (-> (create-request :get "/delete-account" nil)
          (assoc-in [:session :user :login] "account_to_be@deleted.com")
          (assoc-in [:session :access_token] ...token...)
          c/delete-account) => (contains {:status 302 :headers {"Location" "/sign-out"}})
      (provided
        (s/delete-user! "account_to_be@deleted.com") => anything))

(fact "email must not be a duplicate"
      (let [html-response (-> (create-request :post "/register" register-user-params)
                              c/register-user
                              :body
                              html/html-snippet)]
        (-> (html/select html-response [:.form-row--validation-error])
            first
            :attrs
            :class)) => (contains "clj--registration-email")
      (provided
        (v/validate-registration register-user-params s/is-duplicate-user?) => {:email :duplicate}
        (user-store/new-user anything anything) => anything :times 0
        (user-store/store-user anything) => anything :times 0))

(facts "about registration validation errors"
       (fact "user isn't saved to the database if email is invalid"
             (-> (create-request :post "/register" {:email "invalid"}) c/register-user) => anything
             (provided
               (user-store/new-user anything anything) => anything :times 0
               (user-store/store-user anything) => anything :times 0))
       (facts "registration page is rendered with errors"
              (let [html-response (-> (create-request :post "/register" {:email "invalid"})
                                      c/register-user
                                      :body
                                      html/html-snippet)]
                (fact "email field should have validation error class"
                      (html/select html-response [:.form-row--validation-error]) =not=> empty?)
                (fact "invalid email value should be preserved"
                      (-> (html/select html-response [:.registration-email-input])
                          first
                          :attrs
                          :value) => "invalid"))))

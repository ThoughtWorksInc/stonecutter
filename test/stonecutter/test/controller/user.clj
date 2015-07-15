(ns stonecutter.test.controller.user
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clauth.token :as cl-token]
            [clauth.client :as cl-client]
            [clauth.user :as cl-user]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as routes]
            [stonecutter.client :as c]
            [stonecutter.controller.user :as u]
            [stonecutter.db.user :as user]
            [stonecutter.view.profile :as profile]
            [stonecutter.validation :as v]))

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

(background (before :facts (cl-user/reset-user-store!)))

(fact "user can sign in with valid credentials and is redirected to profile, with user and access_token added to session"
      (-> (create-request :post "/sign-in" sign-in-user-params)
          u/sign-in) => (contains {:status 302 :headers {"Location" "/profile"}
                                   :session {:user ...user...
                                             :access_token ...token...}})
      (provided
        (user/authenticate-and-retrieve-user email password) => ...user...
        (cl-token/create-token nil ...user...) => {:token ...token...}))

(facts "about registration"
       (fact "user can register with valid credentials and is redirected to profile-created page, with user added to session"
             (-> (create-request :post "/register" register-user-params)
                 u/register-user) => (contains {:status 302 :headers {"Location" "/profile-created"}
                                                :session {:user ...user...
                                                          :access_token ...token...}})
             (provided
               (v/validate-registration register-user-params user/is-duplicate-user?) => {}
               (user/store-user! email password) => ...user...
               (cl-token/create-token nil ...user...) => {:token ...token...}))

       (fact "session is not lost when redirecting from registration"
            (-> (create-request :post (routes/path :register-user) register-user-params)
                (assoc :session {:some "data"})
                u/register-user) => (contains {:status 302
                                               :headers {"Location" "/profile-created"}
                                               :session {:some "data"
                                                         :user ...user...
                                                         :access_token ...token...}})
             (provided
               (v/validate-registration register-user-params user/is-duplicate-user?) => {}
               (user/store-user! email password) => ...user...
               (cl-token/create-token nil ...user...) => {:token ...token...})))

(fact "signed-in? returns true only when user and access_token are in the session"
      (tabular
        (u/signed-in? ?request) => ?expected-result
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
                 u/show-sign-in-form) => (contains {:status 200}))

       (fact "with user and access_token in session redirects to /")
             (-> (create-request :get "/sign-in" nil)
                 (assoc-in [:session :user] ...user...)
                 (assoc-in [:session :access_token] ...token...)
                 u/show-sign-in-form) => (contains {:status 302 :headers {"Location" "/"}
                                                    :session {:user ...user...
                                                              :access_token ...token...}}))

(fact "if user has session client id, then create an access token and add to user session"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" sign-in-user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            u/sign-in) => (contains {:status  302 :headers {"Location" return-to-url}
                                    :session {:access_token ...token... :user ...user...}})
        (provided
          (user/authenticate-and-retrieve-user email password) => ...user...
          (cl-client/fetch-client "client-id") => ...client...
          (cl-token/create-token ...client... ...user...) => {:token ...token...})))

(fact "if user logged out, access token and user email are removed from session"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" sign-in-user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            u/sign-in
            u/sign-out
            :session) => empty?
        (provided
          (user/authenticate-and-retrieve-user email password) => ...user...
          (cl-client/fetch-client "client-id") => ...client...
          (cl-token/create-token ...client... ...user...) => {:token ...token...})))

(fact "if user has client id but no return-to in session, throws an exception"
      (-> (create-request :post "/sign-in" sign-in-user-params)
          (assoc-in [:session :client-id] "client-id")
          u/sign-in) => (throws Exception)
      (provided
        (user/authenticate-and-retrieve-user email password) => ...user...))

(fact "if user has invalid client id, then throws an exception"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" sign-in-user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            u/sign-in) => (throws Exception)
        (provided
          (user/authenticate-and-retrieve-user email password) => ...user...
          (cl-client/fetch-client "client-id") => nil)))

(facts "about sign-in validation errors"
       (fact "user cannot sign in with blank password"
             (-> (create-request :post "/sign-in" {:email "email@credentials.com" :password ""})
                 u/sign-in) => (contains {:status 200}))
       (fact "user cannot sign in with invalid credentials"
             (-> (create-request :post "/sign-in" {:email "invalid@credentials.com" :password "password"})
                 u/sign-in) => (contains {:status 200})
             (provided
               (user/authenticate-and-retrieve-user "invalid@credentials.com" "password") => nil))
       (facts "sign-in page is rendered with errors when invalid credentials are used"
              (let [html-response (-> (create-request :post "/sign-in" {:email    "invalid@credentials.com"
                                                                        :password "password"})
                                      u/sign-in
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
            u/register-user
            :status) => 302
        (provided
          (user/store-user! "valid@email.com" "password") => ...user...)))

(fact "account can be deleted, user is redirected to profile-deleted and session is cleared"
      (-> (create-request :post "/delete-account" nil)
          (assoc-in [:session :user :login] "account_to_be@deleted.com")
          (assoc-in [:session :access_token] ...token...)
          u/delete-account) => (contains {:status 302 :headers {"Location" "/profile-deleted"} :session nil})
      (provided
        (user/delete-user! "account_to_be@deleted.com") => anything))

(fact "user can access profile-deleted page when not signed in"
      (-> (create-request :get "/profile-deleted" nil)
          u/show-profile-deleted) => (contains {:status 200}))

(fact "email must not be a duplicate"
      (let [html-response (-> (create-request :post "/register" register-user-params)
                              u/register-user
                              :body
                              html/html-snippet)]
        (-> (html/select html-response [:.form-row--validation-error])
            first
            :attrs
            :class)) => (contains "clj--registration-email")
      (provided
        (v/validate-registration register-user-params user/is-duplicate-user?) => {:email :duplicate}
        (cl-user/new-user anything anything) => anything :times 0
        (cl-user/store-user anything) => anything :times 0))

(facts "about registration validation errors"
       (fact "user isn't saved to the database if email is invalid"
             (-> (create-request :post "/register" {:email "invalid"}) u/register-user) => anything
             (provided
               (cl-user/new-user anything anything) => anything :times 0
               (cl-user/store-user anything) => anything :times 0))
       (facts "registration page is rendered with errors"
              (let [html-response (-> (create-request :post "/register" {:email "invalid"})
                                      u/register-user
                                      :body
                                      html/html-snippet)]
                (fact "email field should have validation error class"
                      (html/select html-response [:.form-row--validation-error]) =not=> empty?)
                (fact "invalid email value should be preserved"
                      (-> (html/select html-response [:.registration-email-input])
                          first
                          :attrs
                          :value) => "invalid"))))

(facts "about profile created"
       (fact "view defaults with link to view profile"
             (let [html-response (-> (create-request :get (routes/path :show-profile-created) nil)
                                     u/show-profile-created
                                     :body
                                     html/html-snippet)]
             (-> (html/select html-response [:.clj--profile-created-next__button]) first :attrs :href)
               => (contains (routes/path :show-profile))))

       (fact "coming from an app, view will link to show authorisation form"
             (let [html-response (-> (create-request :get (routes/path :show-profile-created) nil)
                                     (assoc :session {:client-id "123" :return-to "/somewhere"})
                                     u/show-profile-created
                                     :body
                                     html/html-snippet)]
             (-> (html/select html-response [:.clj--profile-created-next__button]) first :attrs :href)
               => (contains "/somewhere"))))

(facts "about show-profile"
       (fact "user's authorised clients passed to html-response"
             (-> (create-request :get (routes/path :show-profile) nil)
                 (assoc :session {:user {:login ...email...}})
                 u/show-profile
                 :body) => (contains #"CLIENT 1[\s\S]+CLIENT 2")
             (provided
               (user/retrieve-user ...email...) => {:login ...email...
                                                    :authorised-clients [...client-id-1... ...client-id-2...]}
               (c/retrieve-client ...client-id-1...) => {:name "CLIENT 1"}
               (c/retrieve-client ...client-id-2...) => {:name "CLIENT 2"})))

(ns stonecutter.test.controller.user
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clauth.token :as cl-token]
            [clauth.user :as cl-user]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as routes]
            [stonecutter.controller.user :as u]
            [stonecutter.db.client :as c]
            [stonecutter.db.user :as user]
            [stonecutter.view.profile :as profile]
            [stonecutter.validation :as v]))


(defn check-redirects-to [path]
  (checker [response] (and
                        (= (:status response) 302)
                        (= (get-in response [:headers "Location"]) path))))

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

(fact "user can sign in with valid credentials and is redirected to profile, with user-login and access_token added to session"
      (-> (create-request :post "/sign-in" sign-in-user-params)
          u/sign-in) => (every-checker
                          (check-redirects-to "/profile")
                          (contains {:session {:user-login ...user-login...
                                               :access_token ...token...}}))
      (provided
        (user/authenticate-and-retrieve-user email password) => {:login ...user-login...}
        (cl-token/create-token nil {:login ...user-login...}) => {:token ...token...}))

(facts "about registration"
       (fact "user can register with valid credentials and is redirected to profile-created page, with user-login and access_token added to session"
             (-> (create-request :post "/register" register-user-params)
                 u/register-user) => (every-checker
                                       (check-redirects-to "/profile-created")
                                       (contains {:session {:user-login ...user-login...
                                                            :access_token ...token...}}))
             (provided
               (v/validate-registration register-user-params user/is-duplicate-user?) => {}
               (user/store-user! email password) => {:login ...user-login...}
               (cl-token/create-token nil {:login ...user-login...}) => {:token ...token...}))

       (fact "session is not lost when redirecting from registration"
            (-> (create-request :post (routes/path :register-user) register-user-params)
                (assoc :session {:some "data"})
                u/register-user) => (every-checker
                                      (check-redirects-to "/profile-created")
                                      (contains {:session {:some "data"
                                                           :user-login ...user-login...
                                                           :access_token ...token...}}))
             (provided
               (v/validate-registration register-user-params user/is-duplicate-user?) => {}
               (user/store-user! email password) => {:login ...user-login...}
               (cl-token/create-token nil {:login ...user-login...}) => {:token ...token...})))

(fact "signed-in? returns true only when user-login and access_token are in the session"
      (tabular
        (u/signed-in? ?request) => ?expected-result
       ?request                                                              ?expected-result
       {:session {:user-login ...user-login... :access_token ...token...}}   truthy
       {:session {:user-login nil              :access_token ...token...}}   falsey
       {:session {:user-login ...user-login... :access_token nil}}           falsey
       {:session {:user-login nil              :access_token nil}}           falsey
       {:session {}}                                                         falsey
       {:session nil}                                                        falsey
       {}                                                                    falsey))

(facts "accessing sign-in form"
       (fact "without user-login and access_token in session shows the sign-in form"
             (-> (create-request :get "/sign-in" nil)
                 u/show-sign-in-form) => (contains {:status 200}))

       (fact "with user-login and access_token in session redirects to /")
             (-> (create-request :get "/sign-in" nil)
                 (assoc-in [:session :user-login] ...user-login...)
                 (assoc-in [:session :access_token] ...token...)
                 u/show-sign-in-form) => (every-checker
                                           (check-redirects-to "/")
                                           (contains {:session {:user-login ...user-login...
                                                                :access_token ...token...}})))

(fact "if user has session client id, then create an access token and add to user session"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" sign-in-user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            u/sign-in) => (every-checker
                            (check-redirects-to return-to-url)
                            (contains {:session {:access_token ...token...
                                                 :user-login ...user-login...}}))
        (provided
          (user/authenticate-and-retrieve-user email password) => {:login ...user-login...}
          (c/retrieve-client "client-id") => ...client...
          (cl-token/create-token ...client... {:login ...user-login...}) => {:token ...token...})))

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
          (c/retrieve-client "client-id") => ...client...
          (cl-token/create-token ...client... ...user...) => {:token ...token...})))

(fact "if user has client id but no return-to in session, should remove client-id from the session and redirect to profile"
      (def user {:login ...user-login...})
      (-> (create-request :post "/sign-in" sign-in-user-params)
          (assoc-in [:session :client-id] "client-id")
          u/sign-in) => (every-checker
                          (check-redirects-to (routes/path :show-profile))
                          (contains {:session {:user-login ...user-login...
                                               :access_token ...token...}}))
      (provided
        (user/authenticate-and-retrieve-user email password) => user
        (u/generate-login-access-token user) => ...token...))

(fact "if user has invalid client id, then throws an exception"
      (let [return-to-url "/authorisation?client-id=whatever"]
        (-> (create-request :post "/sign-in" sign-in-user-params)
            (assoc-in [:session :client-id] "client-id")
            (assoc-in [:session :return-to] return-to-url)
            u/sign-in) => (throws Exception)
        (provided
          (user/authenticate-and-retrieve-user email password) => ...user...
          (c/retrieve-client "client-id") => nil)))

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

(fact "account can be deleted, user is redirected to profile-deleted and session is cleared"
      (-> (create-request :post "/delete-account" nil)
          (assoc-in [:session :user-login] "account_to_be@deleted.com")
          (assoc-in [:session :access_token] ...token...)
          u/delete-account) => (every-checker
                                 (check-redirects-to "/profile-deleted")
                                 (contains {:session nil}))
      (provided
        (user/delete-user! "account_to_be@deleted.com") => anything))

(fact "user can access profile-deleted page when not signed in"
      (-> (create-request :get "/profile-deleted" nil)
          u/show-profile-deleted) => (contains {:status 200}))

(fact "user data is saved"
      (let [user-registration-data (create-user "valid@email.com" "password")]
        (-> (create-request :post "/register" register-user-params)
            u/register-user) => (check-redirects-to "/profile-created")
        (provided
          (user/store-user! "valid@email.com" "password") => ...user...)))

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

(facts "about changing password"
       (fact "the user's password is updated if current password is correct and new password is confirmed"
             (-> (create-request :post "/change-password" {:current-password "currentPassword"
                                                           :new-password "newPassword"
                                                           :confirm-new-password "newPassword"})
                 (assoc-in [:session :user-login] "user_who_is@changing_password.com")
                 u/change-password) => (check-redirects-to "/profile")
             (provided
               (user/authenticate-and-retrieve-user "user_who_is@changing_password.com"
                                                    "currentPassword") => ...user...
               (user/change-password! "user_who_is@changing_password.com" "newPassword") => ...updated-user...))

       (fact "user is returned to change-password page and user's password is not changed if there are validation errors"
             (-> (create-request :post "/change-password" ...invalid-params...)
                 (assoc-in [:session :user-login] "user_who_is@changing_password.com")
                 u/change-password
                 :status) => 200
             (provided
               (v/validate-change-password ...invalid-params...) => {:some-validation-key "some-value"}
               (user/change-password! anything anything) => anything :times 0))

       (fact "user cannot change password if current-password is invalid"
             (-> (create-request :post "/change-password" {:current-password "wrong-password"})
                 (assoc-in [:session :user-login] "user_who_is@changing_password.com")
                 u/change-password
                 :status) => 200
             (provided
               (v/validate-change-password anything) => {}
               (user/authenticate-and-retrieve-user "user_who_is@changing_password.com" "wrong-password") => nil
               (user/change-password! anything anything) => anything :times 0))

       (future-facts "change-password page is rendered with errors"
              (fact "when validation fails"
                    (-> (create-request :post "/change-password" ...invalid-params...)
                        (assoc-in [:session :user-login] "user_who_is@changing_password.com")
                        u/change-password
                        :body
                        html/html-snippet
                        (html/select [:.clj--validation-summary__item])) =not=> empty?
                    (provided
                      (v/validate-change-password ...invalid-params...) => {:some-validation-key "some-value"}))

              (fact "when authorisation fails"
                    (-> (create-request :post "/change-password" ...params-with-wrong-current-password...)
                        u/change-password
                        :body
                        html/html-snippet
                        (html/select [:.clj--validation-summary__item])) =not=> empty?
                    (provided
                      (v/validate-change-password ...params-with-wrong-current-password...) => {}
                      (user/authenticate-and-retrieve-user anything anything) => nil))))


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
                 (assoc :session {:user-login ...email...})
                 u/show-profile
                 :body) => (contains #"CLIENT 1[\s\S]+CLIENT 2")
             (provided
               (user/retrieve-user ...email...) => {:login ...email...
                                                    :authorised-clients [...client-id-1... ...client-id-2...]}
               (c/retrieve-client ...client-id-1...) => {:name "CLIENT 1"}
               (c/retrieve-client ...client-id-2...) => {:name "CLIENT 2"})))

(facts "about unsharing profile cards"
       (facts "about get requests to /unshare-profile-card"
              (fact "client_id from query params is used in the form"
                    (-> (create-request :get (routes/path :show-unshare-profile-card) {:client_id "client-id"})
                        (assoc-in [:session :user-login] ...email...)
                        u/show-unshare-profile-card
                        :body
                        html/html-snippet
                        (html/select [:.clj--client-id__input])
                        first
                        :attrs
                        :value) => "client-id"
                    (provided
                      (user/is-authorised-client-for-user? ...email... "client-id") => true
                      (c/retrieve-client "client-id") => {:client-id "client-id" :name "CLIENT_NAME"}))

              (fact "client name is correctly shown on the page"
                    (let [element-has-correct-client-name-fn (fn [element] (= (html/text element) "CLIENT_NAME"))]
                      (-> (create-request :get (routes/path :show-unshare-profile-card) {:client_id "client-id"})
                          (assoc-in [:session :user-login] ...email...)
                          u/show-unshare-profile-card
                          :body
                          html/html-snippet
                          (html/select [:.clj--app-name])) => (has some element-has-correct-client-name-fn)
                      (provided
                        (user/is-authorised-client-for-user? ...email... "client-id") => true
                        (c/retrieve-client "client-id") => {:client-id "client-id" :name "CLIENT_NAME"})))

              (fact "missing client_id query param responds with 404"
                    (-> (create-request :get (routes/path :show-unshare-profile-card) nil)
                        u/show-unshare-profile-card) => {:status 404})

              (fact "user is redirected to /profile if client_id is not in user's list of authorised clients"
                    (-> (create-request :get (routes/path :show-unshare-profile-card) {:client_id ...client-id...})
                        (assoc-in [:session :user-login] ...email...)
                        u/show-unshare-profile-card) => (check-redirects-to "/profile")
                    (provided
                      (user/is-authorised-client-for-user? ...email... ...client-id...) => false)))

       (facts "about post requests to /unshare-profile-card"
              (fact "posting to /unshare-profile-card with client-id in the form params should remove client-id from the user's authorised clients and then redirect the user to the profile page"
                    (-> (create-request :post "/unshare-profile-card" {:client_id "client-id"})
                        (assoc-in [:session :user-login] "user@email.com")
                        u/unshare-profile-card) => (check-redirects-to "/profile")
                    (provided
                      (user/remove-authorised-client-for-user! "user@email.com" "client-id") => anything))))

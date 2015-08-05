(ns stonecutter.test.controller.user
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clauth.token :as cl-token]
            [clauth.user :as cl-user]
            [net.cgrand.enlive-html :as html]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.controller.user :as u]
            [stonecutter.db.client :as c]
            [stonecutter.db.user :as user]
            [stonecutter.db.storage :as storage]
            [stonecutter.view.profile :as profile]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.validation :as v]))

(def check-body-not-blank
  (checker [response] (not (empty? (:body response)))))

(defn check-redirects-to [path]
  (checker [response] (and
                        (= (:status response) 302)
                        (= (get-in response [:headers "Location"]) path))))

(defn check-signed-in [request user]
  (let [is-signed-in? #(and (= (:login user) (get-in % [:session :user-login]))
                            (contains? (:session %) :access_token))]
    (checker [response]
             (let [session-not-changed (not (contains? response :session))]
               (or (and (is-signed-in? request)
                        session-not-changed)
                   (is-signed-in? response))))))

(def email "valid@email.com")
(def password "password")
(def confirmation-id "1234-ABCD")
(def sign-in-user-params {:email email :password password})
(def register-user-params {:email email :password password :confirm-password password})

(defn create-request
  ([method url params]
   (-> (mock/request method url)
       (assoc :params params)
       (assoc-in [:context :translator] {})))
  ([method url params session]
   (->
     (create-request method url params)
     (assoc :session session))))

(defn create-request-with-query-string [method url params]
  (-> (mock/request method url params)
      (assoc :params params)
      (assoc-in [:context :translator] {})))

(defn create-user [login password]
  {:login    login
   :password password
   :name     nil
   :url      nil})

(def most-recent-email (atom nil))

(defn test-email-sender! [email subject body]
  (reset! most-recent-email {:email   email
                             :subject subject
                             :body    body}))

(defn test-email-renderer [email-data]
  {:subject "confirmation"
   :body    email-data})

(background (before :facts (do (storage/setup-in-memory-stores!)
                               (cl-user/reset-user-store! @storage/user-store)
                               (email/initialise! test-email-sender!
                                                  {:confirmation test-email-renderer}))
                    :after (do (storage/reset-in-memory-stores!)
                               (email/reset-email-configuration!)
                               (reset! most-recent-email nil))))

(facts "about registration"
       (fact "user can register with valid credentials and is redirected to profile-created page, with user-login and access_token added to session"
             (let [response (->> (create-request :post (routes/path :register-user) register-user-params {:some "session-data"})
                                 (u/register-user @storage/user-store))
                   registered-user (user/retrieve-user @storage/user-store email)]
               response => (check-redirects-to (routes/path :show-profile-created))
               response => (contains {:session (contains {:user-login   (:login registered-user)
                                                          :access_token (complement nil?)})})))

       (fact "session is not lost when redirecting from registration"
             (let [response (->> (create-request :post (routes/path :register-user) register-user-params {:some "session-data"})
                                 (u/register-user @storage/user-store))]
               response => (check-redirects-to (routes/path :show-profile-created))
               response => (contains {:session (contains {:some "session-data"})})))

       (fact "user data is saved"
             (let [user-registration-data (create-user "valid@email.com" "password")]
               (->> (create-request :post (routes/path :register-user) register-user-params)
                    (u/register-user @storage/user-store)) => anything
               (provided
                 (user/store-user! @storage/user-store "valid@email.com" "password") => ...user...)))

       (fact "user is send a confirmation email with the correct content"
             (against-background
               (uuid/uuid) => confirmation-id)
             (let [response (->> (create-request :post (routes/path :register-user) register-user-params)
                                 (u/register-user @storage/user-store))
                   registered-user (user/retrieve-user @storage/user-store email)]
               (:email @most-recent-email) => email
               (:body @most-recent-email) => (contains {:confirmation-id confirmation-id})))

       (fact "when user email is send, flash message is assoc-ed in redirect"
             (against-background
               (uuid/uuid) => confirmation-id)
             (let [response (->> (create-request :post (routes/path :register-user) register-user-params)
                                 (u/register-user @storage/user-store))]
               (:flash response) => :confirm-email-sent)))

(facts "about registration validation errors"
       (fact "email must not be a duplicate"
             (let [html-response (->> (create-request :post "/register" register-user-params)
                                      (u/register-user @storage/user-store)
                                      :body
                                      html/html-snippet)]
               (-> (html/select html-response [:.form-row--validation-error])
                   first
                   :attrs
                   :class)) => (contains "clj--registration-email")
             (provided
               (v/validate-registration register-user-params anything) => {:email :duplicate}
               (cl-user/new-user anything anything) => anything :times 0
               (cl-user/store-user @storage/user-store anything) => anything :times 0))

       (fact "user isn't saved to the database if email is invalid"
             (->> (create-request :post "/register" {:email "invalid"}) (u/register-user @storage/user-store)) => anything
             (provided
               (cl-user/new-user anything anything) => anything :times 0
               (cl-user/store-user @storage/user-store anything) => anything :times 0))

       (facts "registration page is rendered with errors"
              (let [html-response (->> (create-request :post "/register" {:email "invalid"})
                                       (u/register-user @storage/user-store)
                                       :body
                                       html/html-snippet)]
                (fact "email field should have validation error class"
                      (html/select html-response [:.form-row--validation-error]) =not=> empty?)
                (fact "invalid email value should be preserved"
                      (-> (html/select html-response [:.registration-email-input])
                          first
                          :attrs
                          :value) => "invalid"))))

(fact "user can sign in with valid credentials and is redirected to profile, with user-login and access_token added to session"
      (->> (create-request :post "/sign-in" sign-in-user-params)
           (u/sign-in @storage/user-store)) => (contains {:status  302 :headers {"Location" (routes/path :show-profile)}
                                                          :session {:user-login   ...user-login...
                                                                    :access_token ...token...}})
      (provided
        (user/authenticate-and-retrieve-user @storage/user-store email password) => {:login ...user-login...}
        (cl-token/create-token @storage/token-store nil {:login ...user-login...}) => {:token ...token...}))

(fact "signed-in? returns true only when user-login and access_token are in the session"
      (tabular
        (u/signed-in? ?request) => ?expected-result
        ?request ?expected-result
        {:session {:user-login ...user-login... :access_token ...token...}} truthy
        {:session {:user-login nil :access_token ...token...}} falsey
        {:session {:user-login ...user-login... :access_token nil}} falsey
        {:session {:user-login nil :access_token nil}} falsey
        {:session {}} falsey
        {:session nil} falsey
        {} falsey))

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
                                     (contains {:session {:user-login   ...user-login...
                                                          :access_token ...token...}})))

(fact "when user signs in, if the session contains return-to, then redirect to that address"
      (->> (create-request :post "/sign-in" sign-in-user-params {:return-to ...return-to-url...})
           (u/sign-in @storage/user-store)) => (contains {:status  302 :headers {"Location" ...return-to-url...}
                                                          :session {:access_token ...token... :user-login ...user-login...}})
      (provided
        (user/authenticate-and-retrieve-user @storage/user-store email password) => {:login ...user-login...}
        (cl-token/create-token @storage/token-store nil {:login ...user-login...}) => {:token ...token...}))

(facts "about sign-in validation errors"
       (fact "user cannot sign in with blank password"
             (->> (create-request :post "/sign-in" {:email "email@credentials.com" :password ""})
                  (u/sign-in @storage/user-store)) => (contains {:status 200}))

       (fact "user cannot sign in with invalid credentials"
             (->> (create-request :post "/sign-in" {:email "invalid@credentials.com" :password "password"})
                  (u/sign-in @storage/user-store)) => (contains {:status 200})
             (provided
               (user/authenticate-and-retrieve-user @storage/user-store "invalid@credentials.com" "password") => nil))

       (facts "sign-in page is rendered with errors when invalid credentials are used"
              (let [html-response (->> (create-request :post "/sign-in" {:email    "invalid@credentials.com"
                                                                         :password "password"})
                                       (u/sign-in @storage/user-store)
                                       :body
                                       html/html-snippet)]
                (fact "form should include validation error class"
                      (html/select html-response [:.clj--validation-summary__item]) =not=> empty?)
                (fact "email value should be preserved"
                      (-> (html/select html-response [:.clj--email__input])
                          first
                          :attrs
                          :value) => "invalid@credentials.com"))))

(fact "when user signs out, access token and user login are removed from session"
      (let [request-with-session {:session {:access_token   ...access-token...
                                            :user-login     ...user-login...
                                            :something-else ...something-else...}}]
        (-> request-with-session
            u/sign-out
            :session)) => {:something-else ...something-else...})

(fact "account can be deleted, user is redirected to profile-deleted and session is cleared"
      (->> (create-request :post "/delete-account" nil {:user-login   "account_to_be@deleted.com"
                                                        :access_token ...token...})
           (u/delete-account @storage/user-store)) => (every-checker
                                                        (check-redirects-to "/profile-deleted")
                                                        (contains {:session nil}))
      (provided
        (user/delete-user! @storage/user-store "account_to_be@deleted.com") => anything))

(fact "user can access profile-deleted page when not signed in"
      (-> (create-request :get "/profile-deleted" nil)
          u/show-profile-deleted) => (contains {:status 200}))

(facts "about changing password"
       (fact "the user's password is updated if current password is correct and new password is confirmed"
             (-> (let [request (create-request :post "/change-password" {:current-password     "currentPassword"
                                                                         :new-password         "newPassword"
                                                                         :confirm-new-password "newPassword"}
                                               {:user-login "user_who_is@changing_password.com"})]
                   (u/change-password @storage/user-store request) => (every-checker (check-redirects-to "/profile")
                                                                                     (contains {:flash :password-changed}))
                   (provided
                     (user/authenticate-and-retrieve-user @storage/user-store "user_who_is@changing_password.com"
                                                          "currentPassword") => ...user...
                     (user/change-password! @storage/user-store "user_who_is@changing_password.com" "newPassword") => ...updated-user...))))

       (fact "user is returned to change-password page and user's password is not changed if there are validation errors"
             (->> (create-request :post "/change-password" ...invalid-params... {:user-login "user_who_is@changing_password.com"})
                  (u/change-password @storage/user-store)) => (every-checker (contains {:status 200})
                                                                             check-body-not-blank)
             (provided
               (v/validate-change-password ...invalid-params...) => {:some-validation-key "some-value"}
               (user/change-password! @storage/user-store anything anything) => anything :times 0))

       (fact "user cannot change password if current-password is invalid"
             (->> (create-request :post "/change-password" {:current-password "wrong-password"} {:user-login "user_who_is@changing_password.com"})
                  (u/change-password @storage/user-store)) => (every-checker (contains {:status 200})
                                                                             check-body-not-blank)
             (provided
               (v/validate-change-password anything) => {}
               (user/authenticate-and-retrieve-user @storage/user-store "user_who_is@changing_password.com" "wrong-password") => nil
               (user/change-password! @storage/user-store anything anything) => anything :times 0))

       (facts "about rendering change-password page with errors"
              (fact "there are no validation messages by default"
                    (-> (create-request :get "/change-password" {})
                        u/show-change-password-form
                        :body
                        html/html-snippet
                        (html/select [:.clj--validation-summary__item])) => empty?)

              (fact "when validation fails"
                    (-> (u/change-password @storage/user-store (create-request :post "/change-password" ...invalid-params... {:user-login "user_who_is@changing_password.com"}))
                        :body
                        html/html-snippet
                        (html/select [:.clj--validation-summary__item])) =not=> empty?
                    (provided
                      (v/validate-change-password ...invalid-params...) => {:new-password :too-short}))

              (fact "when authorisation fails"
                    (-> (u/change-password @storage/user-store (create-request :post "/change-password" ...params-with-wrong-current-password...))
                        :body
                        html/html-snippet
                        (html/select [:.clj--validation-summary__item])) =not=> empty?
                    (provided
                      (v/validate-change-password ...params-with-wrong-current-password...) => {}
                      (user/authenticate-and-retrieve-user anything anything anything) => nil))))


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
                                     (assoc :session {:return-to "/somewhere"})
                                     u/show-profile-created
                                     :body
                                     html/html-snippet)]
               (-> (html/select html-response [:.clj--profile-created-next__button]) first :attrs :href)
               => (contains "/somewhere")))

       (fact "coming from an app, return-to is removed from the session"
             (let [session (-> (create-request :get (routes/path :show-profile-created) nil)
                               (assoc :session {:user-login   ...email...
                                                :access_token ...token...
                                                :return-to    ...url...})
                               u/show-profile-created
                               :session)]
               session =not=> (contains {:return-to anything})
               session => (contains {:user-login anything})
               session => (contains {:access_token anything}))))

(defn with-signed-in-user [ring-map user]
  (let [access-token (cl-token/create-token nil user)]
    (-> ring-map
        (assoc-in [:session :access_token] (:token access-token))
        (assoc-in [:session :user-login] (:login user)))))

(facts "about show-profile"
       (fact "user's authorised clients passed to html-response"
             (->> (create-request :get (routes/path :show-profile) nil {:user-login ...email...})
                  (u/show-profile @storage/user-store)
                  :body) => (contains #"CLIENT 1[\s\S]+CLIENT 2")
             (provided
               (user/retrieve-user @storage/user-store ...email...) => {:login              ...email...
                                                                        :authorised-clients [...client-id-1... ...client-id-2...]}
               (c/retrieve-client anything ...client-id-1...) => {:name "CLIENT 1"}
               (c/retrieve-client anything ...client-id-2...) => {:name "CLIENT 2"}))

       (tabular
         (fact "user confirmation status is displayed appropriately"
               (against-background
                 (user/retrieve-user @storage/user-store ...email...) => {:login      ...email...
                                                                          :confirmed? ?confirmed})
               (let [enlive-snippet
                     (->> (create-request :get (routes/path :show-profile) nil {:user-login ...email...})
                          (u/show-profile @storage/user-store)
                          :body
                          html/html-snippet)]

                 (html/select enlive-snippet [?should-show]) => (one-of anything)
                 (html/select enlive-snippet [?should-hide]) => empty?))

         ?confirmed ?should-show ?should-hide
         true :.clj--email-confirmed-message :.clj--email-not-confirmed-message
         false :.clj--email-not-confirmed-message :.clj--email-confirmed-message))

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
                      (user/is-authorised-client-for-user? @storage/user-store ...email... "client-id") => true
                      (c/retrieve-client anything "client-id") => {:client-id "client-id" :name "CLIENT_NAME"}))

              (fact "client name is correctly shown on the page"
                    (let [element-has-correct-client-name-fn (fn [element] (= (html/text element) "CLIENT_NAME"))]
                      (-> (create-request :get (routes/path :show-unshare-profile-card) {:client_id "client-id"})
                          (assoc-in [:session :user-login] ...email...)
                          u/show-unshare-profile-card
                          :body
                          html/html-snippet
                          (html/select [:.clj--client-name])) => (has some element-has-correct-client-name-fn)
                      (provided
                        (user/is-authorised-client-for-user? @storage/user-store ...email... "client-id") => true
                        (c/retrieve-client anything "client-id") => {:client-id "client-id" :name "CLIENT_NAME"})))

              (fact "missing client_id query param responds with 404"
                    (-> (create-request :get (routes/path :show-unshare-profile-card) nil)
                        u/show-unshare-profile-card) => {:status 404})

              (fact "user is redirected to /profile if client_id is not in user's list of authorised clients"
                    (-> (create-request :get (routes/path :show-unshare-profile-card) {:client_id ...client-id...})
                        (assoc-in [:session :user-login] ...email...)
                        u/show-unshare-profile-card) => (check-redirects-to "/profile")
                    (provided
                      (user/is-authorised-client-for-user? @storage/user-store ...email... ...client-id...) => false)))

       (facts "about post requests to /unshare-profile-card"
              (fact "posting to /unshare-profile-card with client-id in the form params should remove client-id from the user's authorised clients and then redirect the user to the profile page"
                    (-> (create-request :post "/unshare-profile-card" {:client_id "client-id"})
                        (assoc-in [:session :user-login] "user@email.com")
                        u/unshare-profile-card) => (check-redirects-to "/profile")
                    (provided
                      (user/remove-authorised-client-for-user! @storage/user-store "user@email.com" "client-id") => anything))))

(ns stonecutter.test.controller.user
  (:require [midje.sweet :refer :all]
            [clauth.token :as cl-token]
            [clauth.user :as cl-user]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.config :as config]
            [stonecutter.controller.user :as u]
            [stonecutter.db.client :as c]
            [stonecutter.db.user :as user]
            [stonecutter.db.invitations :as inv]
            [stonecutter.db.mongo :as m]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.validation :as v]
            [stonecutter.test.email :as test-email]
            [stonecutter.db.confirmation :as confirmation]
            [stonecutter.db.invitations :as i]
            [stonecutter.test.util.time :as test-time]
            [clojure.java.io :as io]))

(def check-body-not-blank
  (checker [response] (not (empty? (:body response)))))

(def default-first-name "Frank")
(def default-last-name "Lasty")
(def default-email "valid@email.com")
(def default-password "password")
(def confirmation-id "1234-ABCD")
(def sign-in-user-params
  {:sign-in-email    default-email
   :sign-in-password default-password
   :action           "sign-in"})

(defn register-user-params [first-name last-name email password]
  {:registration-first-name first-name
   :registration-last-name  last-name
   :registration-email      email
   :registration-password   password
   :action                  "register"})

(def default-register-user-params (register-user-params default-first-name default-last-name
                                                        default-email default-password))

(def test-clock (test-time/new-stub-clock 0))

(defn test-email-renderer [email-data]
  {:subject "confirmation"
   :body    email-data})

(background
  (email/get-confirmation-renderer) => test-email-renderer)

(facts "about index"
       (fact "when user is not logged in renders the index page"
             (u/index (th/create-request :get (routes/path :index) {})) => (th/check-renders-page [:.func--index-page]))
       (fact "when user is logged in redirects to show profile page"
             (u/index {:session {:user-login ...user-login... :access_token ...access-token...}})
             => (th/check-redirects-to (routes/path :show-profile))))

(facts "about accept invite page"
       (fact "when invite-id is in the request and the database, the accept request page is rendered"
             (let [invite-db (m/create-memory-store)
                   invite-id (inv/generate-invite-id! invite-db "user@email.somewhere" test-clock 7 uuid/uuid)
                   response (u/accept-invite invite-db (th/create-request :get (routes/path :accept-invite :invite-id invite-id) {:invite-id invite-id}))
                   html-response (html/html-snippet (:body response))]
               response => (th/check-renders-page [:.func--accept-invite-page])
               (-> (html/select html-response [:.clj--registration-email__input])
                   first
                   :attrs
                   :value) => "user@email.somewhere"))

       (fact "when invite-id is in the request but not the database, the index page is rendered"
             (let [invite-db (m/create-memory-store)
                   invite-id "weiurht84567yoerghb"]
               (u/accept-invite invite-db (th/create-request :get (routes/path :accept-invite :invite-id invite-id) {:invite-id invite-id}))
               => (th/check-renders-page [:.func--index-page])))

       (fact "when registering using an invite, user data is saved and set to trusted"
             (let [invite-id "84927492GFJSIUD"
                   user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)]
               (->> (th/create-request :post (routes/path :register-using-invitation :invite-id invite-id) default-register-user-params)
                    (u/register-using-invitation user-store token-store confirmation-store test-email-sender ...invitation-store...))) => anything
             (provided
               (user/store-user! anything "Frank" "Lasty" default-email "password") => anything :times 1
               (user/update-user-role! anything default-email "trusted") => anything
               (i/remove-invite! ...invitation-store... anything) => nil))

       (fact "when registering using an invite, the invite data is removed from the database"
             (let [invitation-store (m/create-memory-store)
                   user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)
                   invite-id (inv/generate-invite-id! invitation-store default-email test-clock 7 uuid/uuid)
                   request (th/create-request :post (routes/path :register-using-invitation :invite-id invite-id) (assoc default-register-user-params :invite-id invite-id))]
               (u/register-using-invitation user-store token-store confirmation-store test-email-sender invitation-store request)
               (i/fetch-by-id invitation-store invite-id)
               (:role (user/retrieve-user user-store default-email)) => "trusted"))

       (fact "when there are errors in registering with an invite, the invite data is not removed from the database"
             (let [invitation-store (m/create-memory-store)
                   invite-id (inv/generate-invite-id! invitation-store "user@email.somewhere" test-clock 7 uuid/uuid)
                   user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)]
               (->> (th/create-request :post (routes/path :register-using-invitation :invite-id invite-id) {:invite-id invite-id :registration-email "invalid"})
                    (u/register-using-invitation user-store token-store confirmation-store test-email-sender invitation-store))
               (i/fetch-by-id invitation-store invite-id)) =not=> nil))

(fact "posting to sign-in-or-register route with no valid action parameter"
      (u/sign-in-or-register ...user-store... ...token-store... ...confirmation-store... ...email-sender...
                             {:action ...invalid...}) => nil)

(facts "about registration"
       (fact "user can register with valid credentials and is redirected to profile-created page, with user-login and access_token added to session"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)
                   response (->> (th/create-request :post (routes/path :sign-in-or-register) default-register-user-params {:some "session-data"})
                                 (u/sign-in-or-register user-store token-store confirmation-store test-email-sender))
                   registered-user (user/retrieve-user user-store default-email)]
               response => (th/check-redirects-to (routes/path :show-profile-created))
               response => (contains {:session (contains {:user-login   (:login registered-user)
                                                          :access_token (complement nil?)})})))

       (fact "session is not lost when redirecting from registration"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)
                   response (->> (th/create-request :post (routes/path :sign-in-or-register) default-register-user-params {:some "session-data"})
                                 (u/sign-in-or-register user-store token-store confirmation-store test-email-sender))]
               response => (th/check-redirects-to (routes/path :show-profile-created))
               response => (contains {:session (contains {:some "session-data"})})))

       (fact "user data is saved"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)]
               (->> (th/create-request :post (routes/path :sign-in-or-register) default-register-user-params)
                    (u/sign-in-or-register user-store token-store confirmation-store test-email-sender))) => anything
             (provided
               (user/store-user! anything "Frank" "Lasty" "valid@email.com" "password") => ...user...))

       (fact "user is sent a confirmation email with the correct content"
             (against-background
               (uuid/uuid) => confirmation-id)
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)
                   response (->> (th/create-request :post (routes/path :sign-in-or-register) default-register-user-params)
                                 (u/sign-in-or-register user-store token-store confirmation-store test-email-sender))
                   registered-user (user/retrieve-user user-store default-email)]
               (:email (test-email/last-sent-email test-email-sender)) => default-email
               (:body (test-email/last-sent-email test-email-sender)) => (contains {:confirmation-id confirmation-id})))

       (fact "when user email is sent, flash message is assoc-ed in response"
             (against-background
               (uuid/uuid) => confirmation-id)
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)
                   response (->> (th/create-request :post (routes/path :sign-in-or-register) default-register-user-params)
                                 (u/sign-in-or-register user-store token-store confirmation-store test-email-sender))]
               (:flash response) => {:flash-type    :confirm-email-sent
                                     :email-address default-email})))


(facts "about registration validation errors"
       (fact "email must not be a duplicate"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)
                   original-user (th/store-user! user-store default-email default-password)
                   html-response (->> (th/create-request :post (routes/path :sign-in-or-register) (register-user-params
                                                                                                    default-first-name default-last-name
                                                                                                    default-email default-password))
                                      (u/sign-in-or-register user-store token-store confirmation-store test-email-sender)
                                      :body
                                      html/html-snippet)]
               (-> (html/select html-response [:.form-row--invalid])
                   first
                   :attrs
                   :class)) => (contains "clj--registration-email"))

       (fact "user isn't saved to the database if email is invalid"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   test-email-sender (test-email/create-test-email-sender)]
               (->> (th/create-request :post (routes/path :sign-in-or-register) {:registration-email "invalid" :action "register"})
                    (u/sign-in-or-register user-store token-store confirmation-store test-email-sender))) => anything
             (provided
               (cl-user/new-user anything anything) => anything :times 0
               (cl-user/store-user anything anything) => anything :times 0))

       (facts "index page is rendered with errors"
              (let [user-store (m/create-memory-store)
                    token-store (m/create-memory-store)
                    confirmation-store (m/create-memory-store)
                    test-email-sender (test-email/create-test-email-sender)
                    html-response (->> (th/create-request :post (routes/path :sign-in-or-register) {:registration-email "invalid"
                                                                                                    :action             "register"})
                                       (u/sign-in-or-register user-store token-store confirmation-store test-email-sender)
                                       :body
                                       html/html-snippet)]
                (fact "email field should have validation error class"
                      (html/select html-response [:.form-row--invalid]) =not=> empty?)
                (fact "invalid email value should be preserved"
                      (-> (html/select html-response [:.clj--registration-email__input])
                          first
                          :attrs
                          :value) => "invalid"))))

(fact "user can sign in with valid credentials and is redirected to profile, with user-login and access_token added to session"
      (->> (th/create-request :post (routes/path :sign-in-or-register) sign-in-user-params)
           (u/sign-in-or-register ...user-store... ...token-store... ...confirmation-store... ...email-sender...))
      => (contains {:status  302 :headers {"Location" (routes/path :show-profile)}
                    :session (contains {:user-login   ...user-login...
                                        :access_token ...token...})})
      (provided
        (user/authenticate-and-retrieve-user ...user-store... default-email default-password) => {:login ...user-login...}
        (cl-token/create-token ...token-store... nil {:login ...user-login...}) => {:token ...token...}))

(fact "when user signs in, if the session contains return-to, then redirect to that address"
      (->> (th/create-request :post (routes/path :sign-in-or-register) sign-in-user-params {:return-to ...return-to-url...})
           (u/sign-in-or-register ...user-store... ...token-store... ...confirmation-store... ...email-sender...))
      => (contains {:status  302 :headers {"Location" ...return-to-url...}
                    :session (contains {:access_token ...token... :user-login ...user-login...})})
      (provided
        (user/authenticate-and-retrieve-user ...user-store... default-email default-password) => {:login ...user-login...}
        (cl-token/create-token ...token-store... nil {:login ...user-login...}) => {:token ...token...}))

(facts "about sign-in validation errors"
       (let [user-store (m/create-memory-store)
             token-store (m/create-memory-store)]
         (fact "user cannot sign in with blank password"
               (->> (th/create-request :post (routes/path :sign-in-or-register) {:sign-in-email    "email@credentials.com"
                                                                                 :sign-in-password ""
                                                                                 :action           "sign-in"})
                    (u/sign-in-or-register user-store token-store ...confirmation-store... ...email-sender...))
               => (contains {:status 200})))

       (fact "user cannot sign in with invalid credentials"
             (->> (th/create-request :post (routes/path :sign-in-or-register) {:sign-in-email    "invalid@credentials.com"
                                                                               :sign-in-password "password"
                                                                               :action           "sign-in"})
                  (u/sign-in-or-register ...user-store... ...token-store... ...confirmation-store... ...email-sender...))
             => (contains {:status 200})
             (provided
               (user/authenticate-and-retrieve-user ...user-store... "invalid@credentials.com" "password") => nil))

       (facts "index page is rendered with errors when invalid credentials are used"
              (let [user-store (m/create-memory-store)
                    token-store (m/create-memory-store)
                    html-response (->> (th/create-request :post (routes/path :sign-in-or-register) {:sign-in-email    "not-a-registered-user@credentials.com"
                                                                                                    :sign-in-password "password"
                                                                                                    :action           "sign-in"})
                                       (u/sign-in-or-register user-store token-store ...confirmation-store... ...email-sender...)
                                       :body
                                       html/html-snippet)]
                (fact "form should include validation error class"
                      (html/select html-response [:.clj--sign-in-validation-summary__item]) =not=> empty?)
                (fact "email value should be preserved"
                      (-> (html/select html-response [:.clj--sign-in-email__input])
                          first
                          :attrs
                          :value) => "not-a-registered-user@credentials.com"))))

(fact "when user signs out, access token and user login are removed from session"
      (let [request-with-session {:session {:access_token   ...access-token...
                                            :user-login     ...user-login...
                                            :something-else ...something-else...}}]
        (-> request-with-session
            u/sign-out
            :session)) => {:something-else ...something-else...})

(facts "about deleting accounts"
       (let [user-store (m/create-memory-store)
             confirmation-store (m/create-memory-store)
             _user (th/store-user! user-store default-email default-password)
             _confirmation (confirmation/store! confirmation-store default-email confirmation-id)
             request (th/create-request :post "/delete-account" nil {:user-login   default-email
                                                                     :access_token ...token...})]

         (fact "the user is redirected to the profile-deleted page, and session is cleared"
               (u/delete-account user-store confirmation-store request) => (every-checker
                                                                             (th/check-redirects-to "/profile-deleted")
                                                                             (contains {:session nil})))

         (fact "the user account and any confirmations are deleted"
               (user/retrieve-user user-store default-email) => nil?
               (confirmation/retrieve-by-user-email confirmation-store default-email) => nil?)))

(fact "user can access profile-deleted page when not signed in"
      (-> (th/create-request :get "/profile-deleted" nil)
          u/show-profile-deleted) => (contains {:status 200}))

(facts "about changing password"
       (fact "the user's password is updated if current password is correct and new password is valid"
             (let [request (th/create-request :post "/change-password" {:current-password "currentPassword"
                                                                        :new-password     "newPassword"}
                                              {:user-login "user_who_is@changing_password.com"})
                   test-email-sender (test-email/create-test-email-sender)]
               (u/change-password ...user-store... test-email-sender request) => (every-checker (th/check-redirects-to "/profile")
                                                                                                (contains {:flash :password-changed}))
               (provided
                 (user/authenticate-and-retrieve-user ...user-store... "user_who_is@changing_password.com" "currentPassword") => ...user...
                 (user/change-password! ...user-store... "user_who_is@changing_password.com" "newPassword") => ...updated-user...)))

       (fact "user is returned to change-password page and user's password is not changed if there are validation errors"
             (let [test-email-sender (test-email/create-test-email-sender)]
               (->> (th/create-request :post "/change-password" ...invalid-params... {:user-login "user_who_is@changing_password.com"})
                    (u/change-password ...user-store... test-email-sender))) => (every-checker (contains {:status 200})
                                                                                               check-body-not-blank)
             (provided
               (v/validate-change-password ...invalid-params... anything) => {:some-validation-key "some-value"}
               (user/change-password! ...user-store... anything anything) => anything :times 0))

       (fact "user cannot change password if current-password is invalid"
             (let [user-store (m/create-memory-store)
                   email "user_who_is@changing_password.com"
                   test-email-sender (test-email/create-test-email-sender)]
               (th/store-user! user-store email "new-password")
               (let [original-encrypted-password (:password (user/retrieve-user user-store email))]
                 (->> (th/create-request :post "/change-password" {:current-password "wrong-password"} {:user-login email})
                      (u/change-password user-store test-email-sender)) => (every-checker (contains {:status 200})
                                                                                          check-body-not-blank)
                 (fact "password has not been changed"
                       original-encrypted-password => (:password (user/retrieve-user user-store email))))))

       (facts "about rendering change-password page with errors"
              (fact "there are no validation messages by default"
                    (-> (th/create-request :get "/change-password" {})
                        u/show-change-password-form
                        :body
                        html/html-snippet
                        (html/select [:.clj--validation-summary])
                        first
                        :attrs
                        :class) =not=> (contains "validation-summary--show"))

              (fact "when validation fails"
                    (let [test-email-sender (test-email/create-test-email-sender)]
                      (-> (u/change-password ...user-store... test-email-sender (th/create-request :post "/change-password" ...invalid-params... {:user-login "user_who_is@changing_password.com"}))
                          :body
                          html/html-snippet
                          (html/select [:.clj--validation-summary__item]))) =not=> empty?
                    (provided
                      (v/validate-change-password ...invalid-params... anything) => {:new-password :too-short}))

              (fact "when authorisation fails"
                    (let [user-store (m/create-memory-store)
                          email "user_who_is@changing_password.com"
                          test-email-sender (test-email/create-test-email-sender)]
                      (th/store-user! user-store email "new-password")
                      (-> (u/change-password user-store test-email-sender (th/create-request :post "/change-password" {:current-password "wrong-password"} {:user-login email}))
                          :body
                          html/html-snippet
                          (html/select [:.clj--validation-summary__item])) =not=> empty?)))

       (facts "about confirmation email"
              (fact "email is sent to user when password is changed"
                    (let [test-email-sender (test-email/create-test-email-sender)
                          request (th/create-request :post "/change-password" {:current-password "currentPassword"
                                                                               :new-password     "newPassword"}
                                                     {:user-login "user_who_is@changing_password.com"})]
                      (u/change-password ...user-store... test-email-sender request) => (every-checker (th/check-redirects-to "/profile")
                                                                                                       (contains {:flash :password-changed}))
                      (:body (test-email/last-sent-email test-email-sender))) => (contains "admin@email.com")
                    (provided
                      (user/authenticate-and-retrieve-user ...user-store... "user_who_is@changing_password.com" "currentPassword") => ...user...
                      (user/change-password! ...user-store... "user_who_is@changing_password.com" "newPassword") => ...updated-user...
                      (config/admin-login anything) => "admin@email.com"
                      (config/app-name anything) => ...app-name...))))

(facts "about changing email"
       (fact "the user's email is updated if new email is valid"
             (let [email "user-who-is@changing-email.com"
                   new-email "new-email@email.com"
                   request (th/create-request :post "/update-user-email" {:email-address new-email}
                                              {:user-login email})
                   test-email-sender (test-email/create-test-email-sender)
                   user-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   user (th/store-user! user-store email "password")]
               (u/update-user-email user-store confirmation-store test-email-sender request) => (every-checker (th/check-redirects-to "/profile")
                                                                                                  (contains {:flash :email-changed}))
               (user/retrieve-user user-store email) => nil
               (user/retrieve-user user-store new-email) =not=> nil
               (:email (test-email/last-sent-email test-email-sender)) => new-email
               (let [new-confirmation-id (:confirmation-id (confirmation/retrieve-by-user-email confirmation-store new-email))]
                 new-confirmation-id =not=> nil
                 (:body (test-email/last-sent-email test-email-sender)) => (contains {:confirmation-id new-confirmation-id}))))

       (fact "The user's email is not updated if new email is already registered"
             (let [email "user_who_is@changing_email.com"
                   new-email "new-email@email.com"
                   test-email-sender (test-email/create-test-email-sender)
                   user-store (m/create-memory-store)
                   confirmation-store (m/create-memory-store)
                   current-user (th/store-user! user-store email "password")
                   already-existing-email-user (th/store-user! user-store new-email "password2")
                   request (th/create-request :post "/update-user-email" {:email-address new-email}
                                              {:user-login email})]
               (-> (u/update-user-email user-store confirmation-store test-email-sender request)
                   :body
                   html/html-snippet
                   (html/select [:.clj--new-email__validation])) =not=> empty?
               (dissoc (user/retrieve-user user-store email) :password) => current-user
               (dissoc (user/retrieve-user user-store new-email) :password) => already-existing-email-user))

       (facts "about rendering change-email page with errors"
              (fact "there are no validation messages by default"
                    (-> (th/create-request :get "/change-email" {})
                        u/show-change-email-form
                        :body
                        html/html-snippet
                        (html/select [:.form-row--invalid])) => empty?)

              (fact "there are validation messages if email is invalid"
                    (let [test-email-sender (test-email/create-test-email-sender)
                          confirmation-store (m/create-memory-store)]
                      (-> (u/update-user-email ...user-store... confirmation-store test-email-sender
                                               (th/create-request :post "/change-email" {:email-address "invalid-email-somewhere"}
                                                                  {:user-login "user_who_is@changing_email.com"}))
                          :body
                          html/html-snippet
                          (html/select [:.form-row--invalid]))) =not=> empty?)))

(facts "about updating profile image"
       (let [user-store (m/create-memory-store)
             email "user@email.com"
             user (th/store-user! user-store email "password")
             request (th/create-request :post "/update-profile-image"
                                        {:profile-photo {:content-type "image/png" :tempfile (io/resource "avatar.png")}}
                                        {:user-login email})]
         (u/update-profile-image user-store request) => (th/check-redirects-to "/profile")
         (fact "image in request is saved to file system"
               (io/resource (str "public/images/profile/" (:uid user) ".png")) =not=> nil)
         (fact "image path is saved to db"
               (:profile-picture (user/retrieve-user user-store email)) => (str config/profile-picture-directory (:uid user) ".png"))
         (io/delete-file (str "resources/public/" config/profile-picture-directory (:uid user) ".png"))))

(facts "about profile created"
       (fact "view defaults with link to view profile"
             (let [html-response (-> (th/create-request :get (routes/path :show-profile-created) nil)
                                     u/show-profile-created
                                     :body
                                     html/html-snippet)]
               (-> (html/select html-response [:.clj--profile-created-next__button]) first :attrs :href)
               => (contains (routes/path :show-profile))))

       (fact "coming from an app, view will link to show authorisation form"
             (let [html-response (-> (th/create-request :get (routes/path :show-profile-created) nil)
                                     (assoc :session {:return-to "/somewhere"})
                                     u/show-profile-created
                                     :body
                                     html/html-snippet)]
               (-> (html/select html-response [:.clj--profile-created-next__button]) first :attrs :href)
               => (contains "/somewhere")))

       (fact "coming from an app, return-to is removed from the session"
             (let [session (-> (th/create-request :get (routes/path :show-profile-created) nil)
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
             (->> (th/create-request :get (routes/path :show-profile) nil {:user-login ...email...})
                  (u/show-profile ...client-store... ...user-store...)
                  :body) => (contains #"CLIENT 1[\s\S]+CLIENT 2")
             (provided
               (user/retrieve-user ...user-store... ...email...) => {:login              ...email...
                                                                     :authorised-clients [...client-id-1... ...client-id-2...]}
               (c/retrieve-client ...client-store... ...client-id-1...) => {:name "CLIENT 1"}
               (c/retrieve-client ...client-store... ...client-id-2...) => {:name "CLIENT 2"}))

       (tabular
         (fact "unconfirmed email message is displayed only when user :confirmed? is false"
               (against-background
                 (user/retrieve-user ...user-store... ...email...) => {:login      ...email...
                                                                       :confirmed? ?confirmed})
               (let [enlive-snippet
                     (->> (th/create-request :get (routes/path :show-profile) nil {:user-login ...email...})
                          (u/show-profile (m/create-memory-store) ...user-store...)
                          :body
                          html/html-snippet)]
                 (html/select enlive-snippet [:.clj--unconfirmed-email-message-container]) => ?check))

         ?confirmed ?check
         false (one-of anything)
         true empty?
         nil empty?)


       (tabular
         (fact "admin status is displayed appropriately"
               (against-background
                 (user/retrieve-user ...user-store... ...email...) => {:login ...email...
                                                                       :role  ?role})
               (let [enlive-snippet
                     (->> (th/create-request :get (routes/path :show-profile) nil {:user-login ...email...})
                          (#(assoc-in % [:session :role] ?role))
                          (u/show-profile (m/create-memory-store) ...user-store...)
                          :body
                          html/html-snippet)]

                 (html/select enlive-snippet [:.clj--admin__span]) => ?result))

         ?role ?result
         (:admin config/roles) (one-of anything)
         "nobody" empty?
         nil empty?))

(facts "about resending confirmation emails"
       (facts "when the user's email address has not yet been confirmed"
              (let [test-email-sender (test-email/create-test-email-sender)
                    user-store (m/create-memory-store)
                    confirmation-store (m/create-memory-store)
                    user (th/store-user! user-store default-email default-password)
                    _confirmation (confirmation/store! confirmation-store (:login user) confirmation-id)
                    request (th/create-request :post (routes/path :resend-confirmation-email)
                                               {}
                                               {:user-login default-email})
                    response (u/resend-confirmation-email user-store confirmation-store test-email-sender request)]

                (fact "redirects to profile page with flash message indicating email has been sent"
                      response => (th/check-redirects-to "/profile")
                      response => (contains {:flash :confirmation-email-sent}))

                (fact "sends another confirmation email"
                      (:email (test-email/last-sent-email test-email-sender)) => default-email
                      (:body (test-email/last-sent-email test-email-sender)) => (contains {:confirmation-id confirmation-id}))))

       (facts "when the user's email has already been confirmed"
              (let [test-email-sender (test-email/create-test-email-sender)
                    user-store (m/create-memory-store)
                    confirmation-store (m/create-memory-store)
                    _user-with-confirmed-email (->> (th/store-user! user-store default-email default-password)
                                                    (user/confirm-email! user-store))
                    request (th/create-request :post (routes/path :resend-confirmation-email)
                                               {}
                                               {:user-login default-email})
                    response (u/resend-confirmation-email user-store confirmation-store test-email-sender request)]

                (fact "redirects to profile page with flash message indicating address is already confirmed"
                      response => (th/check-redirects-to "/profile")
                      response => (contains {:flash :email-already-confirmed}))

                (fact "does not send another confirmation email"
                      (test-email/last-sent-email test-email-sender) => nil?))))

(facts "about unsharing profile cards"
       (facts "about get requests to /unshare-profile-card"
              (fact "client_id from query params is used in the form"
                    (let [request (th/create-request :get (routes/path :show-unshare-profile-card)
                                                     {:client_id "client-id"}
                                                     {:user-login ...email...})]
                      (-> (u/show-unshare-profile-card ...client-store... ...user-store... request)
                          :body
                          html/html-snippet
                          (html/select [:.clj--client-id__input])
                          first
                          :attrs
                          :value)) => "client-id"
                    (provided
                      (user/is-authorised-client-for-user? ...user-store... ...email... "client-id") => true
                      (c/retrieve-client ...client-store... "client-id") => {:client-id "client-id" :name "CLIENT_NAME"}))

              (fact "client name is correctly shown on the page"
                    (let [element-has-correct-client-name-fn (fn [element] (= (html/text element) "CLIENT_NAME"))
                          request (th/create-request :get (routes/path :show-unshare-profile-card)
                                                     {:client_id "client-id"}
                                                     {:user-login ...email...})]
                      (-> (u/show-unshare-profile-card ...client-store... ...user-store... request)
                          :body
                          html/html-snippet
                          (html/select [:.clj--client-name])) => (has some element-has-correct-client-name-fn)
                      (provided
                        (user/is-authorised-client-for-user? ...user-store... ...email... "client-id") => true
                        (c/retrieve-client ...client-store... "client-id") => {:client-id "client-id" :name "CLIENT_NAME"})))

              (fact "missing client_id query param responds with nil (404)"
                    (->> (th/create-request :get (routes/path :show-unshare-profile-card) nil)
                         (u/show-unshare-profile-card (m/create-memory-store) ...user-store...)) => nil)

              (fact "user is redirected to /profile if client_id is not in user's list of authorised clients"
                    (->> (th/create-request :get (routes/path :show-unshare-profile-card)
                                            {:client_id ...client-id...}
                                            {:user-login ...email...})
                         (u/show-unshare-profile-card ...client-store... ...user-store...)) => (th/check-redirects-to "/profile")
                    (provided
                      (user/is-authorised-client-for-user? ...user-store... ...email... ...client-id...) => false)))

       (facts "about post requests to /unshare-profile-card"
              (fact "posting to /unshare-profile-card with client-id in the form params should remove client-id from the user's authorised clients and then redirect the user to the profile page"
                    (->> (th/create-request :post "/unshare-profile-card" {:client_id "client-id"} {:user-login "user@email.com"})
                         (u/unshare-profile-card ...user-store...)) => (th/check-redirects-to "/profile")
                    (provided
                      (user/remove-authorised-client-for-user! ...user-store... "user@email.com" "client-id") => anything))))

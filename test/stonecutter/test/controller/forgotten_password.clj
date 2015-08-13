(ns stonecutter.test.controller.forgotten-password
  (:require [midje.sweet :refer :all]
            [clauth.store :as cl-store]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.test.view.test-helpers :as vth]
            [stonecutter.controller.forgotten-password :as fp]
            [stonecutter.routes :as routes]
            [stonecutter.email :as email]
            [stonecutter.test.email :as test-email]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as user]
            [stonecutter.db.forgotten-password :as fpdb]
            [stonecutter.routes :as r]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [stonecutter.util.time :as time]
            [stonecutter.test.util.time :as test-time])
  (:import (org.mindrot.jbcrypt BCrypt)))

(def email-address "email@address.com")
(def forgotten-password-id "SOME-UUID")

(fact "about validation checks on posting email address"
      (let [email-sender (test-email/create-test-email-sender)
            response (->> (th/create-request :post "/forgotten-password" {:email "invalid email"})
                          (fp/forgotten-password-form-post email-sender nil nil nil))]
        (vth/response->enlive-m response) => (vth/element-exists? [:.form-row--validation-error])
        (test-email/last-sent-email email-sender) => nil))

(defn test-renderer [email-data]
  {:subject "TEST EMAIL"
   :body    email-data})

(def test-clock (test-time/new-stub-clock 0))

(background (email/get-forgotten-password-renderer) => test-renderer)

(fact "about valid email address"
      (let [user-store (m/create-memory-store)
            _ (user/store-user! user-store email-address "password")]

        (fact "if user exists then forgotten-password-id is created and stored, e-mail is sent, and user is redirected to confirmation page"
              (let [email-sender (test-email/create-test-email-sender)
                    forgotten-password-store (m/create-memory-store)
                    test-request (-> (th/create-request :post "/forgotten-password" {:email email-address})
                                     (th/add-config-request-context {:app-name "My App" :base-url "https://myapp.com"}))]
                (fp/forgotten-password-form-post email-sender user-store forgotten-password-store test-clock test-request)
                => (th/check-redirects-to (routes/path :show-forgotten-password-confirmation))

                (let [last-email (test-email/last-sent-email email-sender)
                      forgotten-password-id (-> last-email :body :forgotten-password-id)
                      forgotten-password-entry (cl-store/fetch forgotten-password-store forgotten-password-id)]
                  last-email => {:email   email-address
                                 :subject "TEST EMAIL"
                                 :body    {:forgotten-password-id forgotten-password-id :app-name "My App" :base-url "https://myapp.com"}}
                  (:login forgotten-password-entry) => email-address)))

        (fact "if forgotten-password record already exists for user, then new id is not written to store and current one is reused"
              (let [email-sender (test-email/create-test-email-sender)
                    forgotten-password-store (m/create-memory-store)
                    existing-id "existing-forgotten-password-id"
                    test-request (-> (th/create-request :post "/forgotten-password" {:email email-address})
                                     (th/add-config-request-context {:app-name "My App" :base-url "https://myapp.com"}))]
                (fpdb/store-id-for-user! forgotten-password-store test-clock existing-id email-address 24)
                (fp/forgotten-password-form-post email-sender user-store forgotten-password-store test-clock test-request)
                => (th/check-redirects-to (routes/path :show-forgotten-password-confirmation))
                (let [last-email (test-email/last-sent-email email-sender)
                      forgotten-password-entries (cl-store/entries forgotten-password-store)]
                  (-> last-email :body :forgotten-password-id) => existing-id
                  (first forgotten-password-entries) => (contains {:forgotten-password-id existing-id :login email-address}))))

        (fact "if forgotten-password record already exists for user, but has expired, then record is removed and replaced with new id"
              (let [email-sender (test-email/create-test-email-sender)
                    forgotten-password-store (m/create-memory-store)
                    clock (test-time/new-stub-clock 0)
                    test-request (-> (th/create-request :post "/forgotten-password" {:email email-address})
                                     (th/add-config-request-context {:app-name "My App" :base-url "https://myapp.com"}))]
                (fpdb/store-id-for-user! forgotten-password-store test-clock "old-id" email-address 24) => anything

                ; Move clock forward by 24 hours
                (test-time/update-time clock (partial + time/day))
                (fp/forgotten-password-form-post email-sender user-store forgotten-password-store clock test-request)
                => (th/check-redirects-to (routes/path :show-forgotten-password-confirmation))

                (let [last-email (test-email/last-sent-email email-sender)]
                  (-> last-email :body :forgotten-password-id) =not=> "old-id"
                  (count (cl-store/entries forgotten-password-store)) => 1
                  (-> (cl-store/entries forgotten-password-store) first :forgotten-password-id) =not=> "old-id")))

        (fact "users email is lower-cased"
              (let [email-sender (test-email/create-test-email-sender)
                    forgotten-password-store (m/create-memory-store)
                    test-request (-> (th/create-request :post "/forgotten-password" {:email (string/upper-case email-address)}))]
                (fp/forgotten-password-form-post email-sender user-store forgotten-password-store test-clock test-request)
                (-> (test-email/last-sent-email email-sender) :email) => email-address
                (-> (cl-store/entries forgotten-password-store) first :login) => email-address))
        ))

(facts "about reset password form"
       (fact "if the forgotten-password-id in the URL corresponds to a non-expired forgotten-password record, the reset password form is displayed"
             (let [user-store (m/create-memory-store)
                   forgotten-password-store (m/create-memory-store)
                   _ (user/store-user! user-store email-address "password")
                   _ (fpdb/store-id-for-user! forgotten-password-store test-clock forgotten-password-id email-address 24)
                   test-request (th/create-request :get (routes/path :show-reset-password-form
                                                                     :forgotten-password-id forgotten-password-id)
                                                   {:forgotten-password-id forgotten-password-id})
                   response (fp/show-reset-password-form forgotten-password-store user-store test-clock test-request)]
               (:status response) => 200))

       (fact "if there is no forgotten password record with an id matching that in the URL, then redirect to resend e-mail"
             (let [user-store (m/create-memory-store)
                   forgotten-password-store (m/create-memory-store)
                   test-request (th/create-request :get (routes/path :show-reset-password-form
                                                                     :forgotten-password-id forgotten-password-id)
                                                   {:forgotten-password-id forgotten-password-id})]
               (fp/show-reset-password-form forgotten-password-store user-store test-clock test-request) => (th/check-redirects-to (r/path :show-forgotten-password-form))))

       (fact "if a non-expired forgotten-password record can be found, but there is no corresponding user, then redirect to resend e-mail"
             (let [user-store (m/create-memory-store)
                   forgotten-password-store (m/create-memory-store)
                   _ (fpdb/store-id-for-user! forgotten-password-store test-clock forgotten-password-id email-address 24)
                   test-request (th/create-request :get (routes/path :show-reset-password-form
                                                                     :forgotten-password-id forgotten-password-id)
                                                   {:forgotten-password-id forgotten-password-id})]
               (fp/show-reset-password-form forgotten-password-store user-store test-clock test-request) => (th/check-redirects-to (r/path :show-forgotten-password-form))))
       )

(defn create-reset-password-post
  ([forgotten-password-id new-password confirm-password]
   (th/create-request :post "reset-password-endpoint"
                      {:forgotten-password-id forgotten-password-id
                       :new-password          new-password
                       :confirm-new-password  confirm-password}))
  ([forgotten-password-id new-password confirm-password session]
   (-> (create-reset-password-post forgotten-password-id new-password confirm-password)
       (assoc :session session))))

(facts "about resetting a password"
       (let [user-store (m/create-memory-store)
             forgotten-password-store (m/create-memory-store)
             token-store (m/create-memory-store)]
         (user/store-user! user-store email-address "password")
         (fpdb/store-id-for-user! forgotten-password-store test-clock forgotten-password-id email-address 24)

         (fact "if there are validation errors, then the reset password form is returned with the validation errors"
               (let [test-request (create-reset-password-post forgotten-password-id "" "")
                     response (fp/reset-password-form-post forgotten-password-store user-store token-store test-clock test-request)]
                 (:status response) => 200
                 (vth/response->enlive-m response) => (vth/element-exists? [:.form-row--validation-error])))

         (fact "if the id doesn't exist and params are invalid then redirect to resend e-mail"
               (let [response (->> (create-reset-password-post "unknown-forgotten-password-id" "" "")
                                   (fp/reset-password-form-post forgotten-password-store user-store token-store test-clock))]
                 response => (th/check-redirects-to (r/path :show-forgotten-password-form))
                 (:flash response) => :expired-password-reset))

         (fact "if the id doesn't exist and params are valid then redirect to resend e-mail"
               (->> (create-reset-password-post "unknown-forgotten-password-id" "new-password" "new-password")
                    (fp/reset-password-form-post forgotten-password-store user-store token-store test-clock))
               => (th/check-redirects-to (r/path :show-forgotten-password-form)))

         (fact "if id exists and params are valid, but related user no longer exists, then redirect to resend e-mail"
               (let [forgotten-password-store (m/create-memory-store)]
                 (fpdb/store-id-for-user! forgotten-password-store test-clock "id-without-user" "nonexistant@user.com" 24)
                 (->> (create-reset-password-post "id-without-user" "new-password" "new-password")
                      (fp/reset-password-form-post forgotten-password-store user-store token-store test-clock))
                 => (th/check-redirects-to (r/path :show-forgotten-password-form))))

         (fact "if id exists and params are valid, but id has expired then redirect to resend e-mail"
               (let [forgotten-password-store (m/create-memory-store)
                     clock (test-time/new-stub-clock 0)]
                 (fpdb/store-id-for-user! forgotten-password-store clock forgotten-password-id email-address 24)
                 ;; move forward in time
                 (test-time/update-time clock (partial + (* 2 time/day)))
                 (->> (create-reset-password-post forgotten-password-id "new-password" "new-password")
                      (fp/reset-password-form-post forgotten-password-store user-store token-store clock))
                 => (th/check-redirects-to (r/path :show-forgotten-password-form))))

         (fact "if the password and confirm password is valid"
               (let [valid-request (create-reset-password-post forgotten-password-id "new-password" "new-password" {:other-value "other-value"})
                     response (fp/reset-password-form-post forgotten-password-store user-store token-store test-clock valid-request)]
                 (fact "the new password is successfully saved"
                       (let [new-encrypted-password (:password (user/retrieve-user user-store email-address))]
                         (BCrypt/checkpw "password" new-encrypted-password) => false
                         (BCrypt/checkpw "new-password" new-encrypted-password) => true))
                 (fact "the user is logged in and existing session values are not preserved"
                       (-> response :session :user-login) => email-address
                       (-> response :session :access_token) =not=> nil?
                       (-> response :session :access_token) => (-> (cl-store/entries token-store) first :token)
                       (-> (cl-store/entries token-store) first :subject) => (user/retrieve-user user-store email-address)
                       (-> response :session count) => 2)
                 (fact "the response has a flash message indicating that the user's password has been changed"
                       (-> response :flash) => :password-changed)
                 (fact "the user is redirected to the profile page"
                       response => (th/check-redirects-to (r/path :show-profile)))
                 (fact "the forgotten password id record is removed"
                       (cl-store/entries forgotten-password-store) => empty?)))))

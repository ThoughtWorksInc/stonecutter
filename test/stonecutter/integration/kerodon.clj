(ns stonecutter.integration.kerodon
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter.integration.kerodon-helpers :as kh]
            [stonecutter.handler :as h]
            [stonecutter.db.storage :as s]
            [stonecutter.logging :as l]
            [stonecutter.view.register :as register-view]))

(l/init-logger!)

(defn print-enlive [state]
  (prn (-> state :enlive))
  state)

(defn print-request [state]
  (prn (-> state :request))
  state)

(defn print-state [state]
  (prn state)
  state)

(def registration-email-input :.func--email__input)
(def registration-password-input :.func--password__input)
(def registration-confirm-input :.func--confirm-password__input)
(def registration-submit :.func--create-profile__button)
(def registration-page-body :.func--register-page)
(def registration-email-validation-element :.clj--registration-email__validation)

(def profile-created-page-body :.func--profile-created-page)
(def profile-page-body :.func--profile-page)
(def profile-deleted-page-body :.func--profile-deleted-page)

(def sign-in-email-input :.func--email__input)
(def sign-in-password-input :.func--password__input)
(def sign-in-submit :.func--sign-in__button)
(def sign-in-page-body :.func--sign-in-page)

(def sign-out-link :.func--sign-out__link)

(def delete-account-link :.func--delete-account__link)
(def delete-account-button :.func--delete-account__button)
(def delete-account-page-body :.func--delete-account-page)

(def error-404-page-body :.func--error-404-page)
(def error-500-page-body :.func--error-500-page)

(defn register [state email]
  (-> state
      (k/visit "/register")
      (k/fill-in registration-email-input email)
      (k/fill-in registration-password-input "valid-password")
      (k/fill-in registration-confirm-input "valid-password")
      (k/press registration-submit)))

(defn sign-in [state email]
  (-> state
      (k/visit "/sign-in")
      (k/fill-in sign-in-email-input email)
      (k/fill-in sign-in-password-input "valid-password")
      (k/press sign-in-submit)))

(s/setup-in-memory-stores!)

(facts "Home url redirects to sign-in page if user is not signed in"
       (-> (k/session h/app)
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)
           (kh/selector-exists [sign-in-page-body])))

(facts "User is returned to same page when email is invalid"
       (-> (k/session h/app)
           (k/visit "/register")
           (k/fill-in "Email address" "invalid-email")
           (k/press registration-submit)
           (kh/page-uri-is "/register")
           (kh/response-status-is 200)
           (kh/selector-exists [registration-page-body])
           (kh/selector-includes-content [registration-email-validation-element] "Enter a valid email address")))

(facts "User is returned to same page when existing email is used"
       (-> (k/session h/app)
           (register "existing@user.com")
           (k/visit "/register")
           (k/fill-in registration-email-input "existing@user.com")
           (k/fill-in registration-password-input "password")
           (k/fill-in registration-confirm-input "password")
           (k/press registration-submit)
           (kh/page-uri-is "/register")
           (kh/response-status-is 200)
           (kh/selector-exists [registration-page-body])))

(facts "Register page redirects to profile-created page when registered
       and
       user is correctly in the session so that email address is displayed on profile card"
       (-> (k/session h/app)
           (register "email@server.com")
           (k/follow-redirect)
           (kh/page-uri-is "/profile-created")
           (kh/response-status-is 200)
           (kh/selector-exists [profile-created-page-body])

           (k/visit "/profile")
           (kh/page-uri-is "/profile")
           (kh/response-status-is 200)
           (kh/selector-exists [profile-page-body])
           (kh/selector-includes-content [:body] "email@server.com")))

(facts "User is redirected to sign-in page when accessing profile page not signed in"
       (-> (k/session h/app)
           (k/visit "/profile")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)
           (kh/selector-exists [sign-in-page-body])))

(facts "User can sign in"
       (-> (k/session h/app)
           (sign-in "email@server.com")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")
           (kh/response-status-is 200)
           (kh/selector-exists [profile-page-body])
           (kh/selector-includes-content [:body] "email@server.com")))

(facts "User can sign out"
       (-> (k/session h/app)
           (sign-in "email@server.com")
           (k/visit "/profile")
           (k/follow sign-out-link)
           (k/follow-redirect)
           (kh/page-uri-is "/")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")))

(facts "Home url redirects to profile page if user is signed in"
       (-> (k/session h/app)
           (sign-in "email@server.com")
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")))

(facts "Home url redirects to profile page if user is registered"
       (-> (k/session h/app)
           (register "email2@server.com")
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")))

(facts "User can delete account"
      (-> (k/session h/app)
          (register "account_to_be@deleted.com")
          (k/visit "/profile")
          (k/follow delete-account-link)
          (kh/page-uri-is "/delete-account")
          (kh/response-status-is 200)
          (kh/selector-exists [delete-account-page-body])
          (k/press delete-account-button)
          (k/follow-redirect)
          (kh/page-uri-is "/profile-deleted")
          (kh/response-status-is 200)
          (kh/selector-exists [profile-deleted-page-body])))

(facts "Not found page is shown for unknown url"
       (-> (k/session h/app)
           (k/visit "/wrong-url")
           (kh/response-status-is 404)
           (kh/selector-exists [error-404-page-body])))

(fact "Error page is shown if an exception is thrown"
      (against-background
        (register-view/registration-form anything) =throws=> (Exception.))
      (-> (k/session (h/create-app :dev-mode? false))
          (k/visit "/register")
           (kh/response-status-is 500)
           (kh/selector-exists [error-500-page-body]))
      (fact "if dev mode is enabled then error middleware isn't invoked (exception not caught)"
            (-> (k/session (h/create-app :dev-mode? true))
                (k/visit "/register")) => (throws Exception)))

(future-fact "Replaying the same post will generate a 403 from the csrf handling"
      (-> (k/session h/app)
          (register "csrf@email.com")
          (sign-in "csrf@email.com")
          (kh/replay-last-request)
          (kh/response-status-is 403)))

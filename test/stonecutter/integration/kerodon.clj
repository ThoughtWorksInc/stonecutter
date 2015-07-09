(ns stonecutter.integration.kerodon
  (:require [midje.sweet :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [kerodon.core :as k]
            [net.cgrand.enlive-html :as html]
            [stonecutter.integration.kerodon-helpers :as kh]
            [stonecutter.handler :as h]
            [stonecutter.storage :as s]
            [stonecutter.logging :as l]
            [stonecutter.view.register :refer [registration-form]]
            [stonecutter.middleware :as m]))

(l/init-logger!)

(defn selector-includes-content [state selector content]
  (fact {:midje/name "Check if element contains string"}
        (-> state :enlive (html/select selector) first html/text) => (contains content))
  state)

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
      (k/fill-in :.func--email__input email)
      (k/fill-in :.func--password__input "valid-password")
      (k/press :.func--sign-in__button)))

(s/setup-in-memory-stores!)

(facts "Home url redirects to sign-in page if user is not signed in"
       (-> (k/session h/app)
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-title-is "Sign in")
           (kh/page-uri-is "/sign-in")))

(facts "User is returned to same page when email is invalid"
       (-> (k/session h/app)
           (k/visit "/register")
           (k/fill-in "Email address" "invalid-email")
           (k/press :.func--create-profile__button)
           (kh/page-uri-is "/register")
           (kh/selector-has-content [:.clj--registration-email__validation] "Enter a valid email address")))

(facts "User is returned to same page when existing email is used"
       (-> (k/session h/app)
           (register "existing@user.com")
           (k/visit "/register")
           (k/fill-in registration-email-input "existing@user.com")
           (k/fill-in registration-password-input "password")
           (k/fill-in registration-confirm-input "password")
           (k/press registration-submit)
           (kh/page-uri-is "/register")
           (kh/response-status-is 200)))

(facts "Register page redirects to profile-created page when registered
       and
       user is correctly in the session so that email address is displayed on profile card"
       (-> (k/session h/app)
           (register "email@server.com")
           (k/follow-redirect)
           (kh/page-uri-is "/profile-created")

           (k/visit "/profile")
           (kh/page-uri-is "/profile")
           (selector-includes-content [:body] "email@server.com")))

(facts "User is redirected to sign-in page when accessing profile page not signed in"
       (-> (k/session h/app)
           (k/visit "/profile")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")))

(facts "User can sign in"
       (-> (k/session h/app)
           (sign-in "email@server.com")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")
           (selector-includes-content [:body] "email@server.com")))

(facts "User can sign out"
       (-> (k/session h/app)
           (sign-in "email@server.com")
           (k/visit "/profile")
           (k/follow :.func--sign-out__link)
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
          (k/visit "/delete-account")
          (kh/page-uri-is "/delete-account")
          (kh/response-status-is 200)
          (k/press :.func--delete-account__button)
          (k/follow-redirect)
          (kh/page-uri-is "/sign-out")))

(facts "Not found page is shown for unknown url"
       (-> (k/session h/app)
           (k/visit "/wrong-url")
           (kh/page-title-is "Error-404")))

(fact "Error page is shown if an exception is thrown"
      (against-background
        (registration-form anything) =throws=> (Exception.))
      (-> (k/session (h/create-app :dev-mode? false))
          (k/visit "/register")
          (kh/page-title-is "Error-500"))
      (fact "if dev mode is enabled then error middleware isn't invoked"
            (-> (k/session (h/create-app :dev-mode? true))
                (k/visit "/register")) => (throws Exception)))

(future-fact "Replaying the same post will generate a 403 from the csrf handling"
      (-> (k/session h/app)
          (register "csrf@email.com")
          (sign-in "csrf@email.com")
          (kh/replay-last-request)
          (kh/response-status-is 403)))

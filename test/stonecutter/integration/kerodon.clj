(ns stonecutter.integration.kerodon
  (:require [midje.sweet :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [kerodon.core :as k]
            [net.cgrand.enlive-html :as html]
            [stonecutter.integration.kerodon-helpers :as kh]
            [stonecutter.handler :as h]
            [stonecutter.storage :as s]
            [stonecutter.logging :as l]
            [stonecutter.view.register :refer [registration-form]]))

(l/init-logger!)


(defn selector-includes-content [state selector content]
  (fact {:midje/name "Check if element contains string"}
        (-> state :enlive (html/select selector) first html/text) => (contains content))
  state)

(defn print-enlive [state]
  (prn (-> state :enlive))
  state)

(s/setup-in-memory-stores!)

(facts "Home url redirects to sign-in page if user is not signed in"
       (-> (k/session h/app)
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-title-is "Sign in")
           (kh/page-uri-is "/login")))

(facts "User is returned to same page when email is invalid"
       (-> (k/session h/app)
           (k/visit "/register")
           (k/fill-in "Email address" "invalid-email")
           (k/press :.func--create-profile__button)
           (kh/page-uri-is "/register")
           (kh/selector-has-content [:.clj--registration-email__validation] "Enter a valid email address")))

(facts "User is taken to success page when user is successfully created"
       (-> (k/session h/app)
           (k/visit "/register")
           (k/fill-in :.func--email__input "email@server.com")
           (k/fill-in :.func--password__input "valid-password")
           (k/fill-in :.func--confirm-password__input "valid-password")
           (k/press :.func--create-profile__button)
           (kh/page-uri-is "/register")
           (kh/selector-has-content [:body] "You saved the user")))

(facts "User is redirected to sign-in page when accessing profile page not signed in"
       (-> (k/session h/app)
           (k/visit "/profile")
           (k/follow-redirect)
           (kh/page-uri-is "/login")))

(defn sign-in [state]
  ( -> state
       (k/visit "/login")
       (k/fill-in :.func--email__input "email@server.com")
       (k/fill-in :.func--password__input "valid-password")
       (k/press :.func--sign-in__button)))

(facts "User can sign in"
       (-> (k/session h/app)
           sign-in
           (k/follow-redirect)
           (kh/page-uri-is "/profile") 
           (selector-includes-content [:body] "email@server.com"))) 

(facts "User can sign out"
       (-> (k/session h/app)
           sign-in
           (k/visit "/profile")
           (k/follow :.func--sign-out__link)
           (k/follow-redirect)
           (kh/page-uri-is "/")
           (k/follow-redirect)
           (kh/page-uri-is "/login")))

(facts "Home url redirects to profile page if user is signed in"
       (-> (k/session h/app)
           sign-in
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")))

(facts "Not found page is shown for unknown url"
       (-> (k/session h/app)
           (k/visit "/wrong-url")
           (kh/page-title-is "Error-404")))

(fact "Error page is shown if an exception is thrown"
      (against-background
         (registration-form anything) =throws=> (Exception.))
       (-> (k/session (h/wrap-error-handling h/app))
           (k/visit "/register")
           (kh/page-title-is "Error-500")))

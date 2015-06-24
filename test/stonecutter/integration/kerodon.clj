(ns stonecutter.integration.kerodon
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter.handler :refer [app]]
            [stonecutter.storage :as s]
            [net.cgrand.enlive-html :as html]))

(defn page-title-is [state title]
  (fact {:midje/name "Checking page title:"}
        (-> state :enlive (html/select [:title]) first html/text) => title)
  state)

(defn page-uri-is [state uri]
  (fact {:midje/name "Checking page uri:"}
        (-> state :request :uri) => uri)
  state)

(defn selector-has-content [state selector content]
  (fact {:midje/name "Check content of element"}
        (-> state :enlive (html/select selector) first html/text) => content)
  state)

(defn print-enlive [state]
  (prn (-> state :enlive))
  state)

(s/start-in-memory-datastore!)

(facts "Home url redirects to registration page"
       (-> (k/session app)
           (k/visit "/")
           (k/follow-redirect)
           (page-title-is "Register")
           (page-uri-is "/register")))

(facts "User is returned to same page when email is invalid"
       (-> (k/session app)
           (k/visit "/register")
           (k/fill-in "Email address" "invalid-email")
           (k/press "Create account")
           (page-uri-is "/register")
           (selector-has-content [:.clj--registration-email__validation] "Enter a valid email address")))

(facts "User is taken to success page when user is successfully created"
       (-> (k/session app)
           (k/visit "/register")
           (k/fill-in :.func--email__input "email@server.com")
           (k/fill-in :.func--password__input "valid-password")
           (k/fill-in :.func--confirm-password__input "valid-password")
           (k/press "Create account")
           (page-uri-is "/register")
           (selector-has-content [:body] "You saved the user")))

(future-facts "User can sign in"
       (-> (k/session app)
           (k/visit "/sign-in")
           (k/fill-in :.func--email__input "email@server.com")
           (k/fill-in :.func--password__input "valid-password")
           (k/press "Sign in")
           (page-uri-is "/profile")
           (selector-has-content [:body] "You are signed in as email@server.com")))

(facts "Not found page is shown for unknown url"
       (-> (k/session app)
           (k/visit "/wrong-url")
           (page-title-is "Error-404")))

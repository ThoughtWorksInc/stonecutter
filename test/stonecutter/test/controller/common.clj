(ns stonecutter.test.controller.common
  (:require [midje.sweet :refer :all]
            [stonecutter.db.mongo :as m]
            [stonecutter.controller.common :as common]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.routes :as r]
            [clauth.store :as cl-store]))

(facts "about signing in user"
       (let [token-store (m/create-memory-store)
             existing-session {:other "value"}
             user {:login "user@user.com"}
             response (common/sign-in-user token-store user "/path" existing-session)]
         response => (th/check-redirects-to "/path")
         (-> response :session :user-login) => "user@user.com"
         (-> response :session :access_token) =not=> nil
         (-> response :session :access_token) => (-> (cl-store/entries token-store) first :token)
         (-> response :session :other) => "value")

       (fact "if path is not supplied then defaults to home"
             (let [token-store (m/create-memory-store)
                   user {:login "user@user.com"}]
                (common/sign-in-user token-store user) => (th/check-redirects-to (r/path :home)))))

(fact "signed-in? returns true only when user-login and access_token are in the session"
      (tabular
        (common/signed-in? ?request) => ?expected-result
        ?request ?expected-result
        {:session {:user-login ...user-login... :access_token ...token...}} truthy
        {:session {:user-login nil :access_token ...token...}} falsey
        {:session {:user-login ...user-login... :access_token nil}} falsey
        {:session {:user-login nil :access_token nil}} falsey
        {:session {}} falsey
        {:session nil} falsey
        {} falsey))

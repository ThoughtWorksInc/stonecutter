(ns stonecutter.test.controller.oauth
  (:require [midje.sweet :refer :all]
            [stonecutter.controller.oauth :as oauth]
            [ring.mock.request :as r]
            [clauth.client :as client]
            [clauth.token :as token]
            [clauth.user :as user]
            [clauth.auth-code :as auth-code]
            [cheshire.core :as json]
            [stonecutter.controller.user :as u]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.storage :as storage])
  (:import (org.apache.commons.codec.binary Base64)))

(background
  (before :facts (storage/setup-in-memory-stores!)
          :after (storage/reset-in-memory-stores!)))

(facts "about authorisation end-point"
       (fact "request with no parameters returns 403"
             (let [request (r/request :get "/authorisation")
                   response (oauth/authorise request)]
               (:status response) => 403))

       (fact "request with client-id, response_type and redirect_uri returns redirect to login page if there is no user session"
             (let [client-details (client/register-client "ClientTestApp" "localhost:3001") ;FORMAT => {:client-secret "XLFCQRKJXSV3T6YQJL5ZJJVGFUJNT6LD", :client-id "RXBX6ZXAER5KPDSZ3ZCZJDOBS27FLDE7", :name "ClientTestApp", :url "localhost:3001"}
                   request (-> (r/request :get "/authorisation" {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback"})
                               (assoc :params {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback"})
                               (r/header "accept" "text/html"))
                   response (oauth/authorise request)]
               (:status response) => 302
               (-> response (get-in [:headers "Location"])) => "/sign-in"
               (-> response (get-in [:session :return-to])) => (format "/authorisation?client_id=%s&response_type=code&redirect_uri=callback" (:client-id client-details))
               (-> response (get-in [:session :client-id])) => (:client-id client-details)))

       (fact "valid request goes to authorisation page with auth_code and email when there is an existing user session"
             (let [user-email "email@user.com"
                   user (storage/store-user! user-email "password")
                   client-details (client/register-client "MYAPP" "myapp.com") ; NB this saves into the client store
                   access-token (token/create-token client-details user) ; NB this saves into the token store
                   request (-> (r/request :get "/authorisation")
                               (assoc :params {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback"})
                               (r/header "accept" "text/html")
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user :email] user-email)
                               (assoc-in [:context :translator] {}))
                   response (oauth/authorise request)]
               (:status response) => 200
               (get-in response [:session :access_token]) => (:token access-token)
               (get response :body) => (contains "Share Profile Card")
               (get-in response [:session :user :email]) => user-email))

       (fact "posting to authorisation endpoint redirects to callback with auth code"
             (let [user-email "email@user.com"
                   user (storage/store-user! user-email "password")
                   client-details (client/register-client "MYAPP" "myapp.com")
                   access-token (token/create-token client-details user)
                   csrf-token "CSRF-TOKEN"
                   request (-> (r/request :post "/authorisation")
                               (assoc :params {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback" :csrf-token csrf-token})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :csrf-token] csrf-token)
                               (assoc :content-type "application/x-www-form-urlencoded") ;To mock a form post
                               (assoc-in [:session :user :email] user-email))
                   response (oauth/authorise-client request)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => (contains "callback?code=")))

       (fact "user-email and access_token in session stay in session if user is logged in"
             (let [user-email "email@user.com"
                   user (storage/store-user! user-email "password")
                   client-details (client/register-client "MYAPP" "myapp.com")
                   access-token (token/create-token client-details user)
                   request (-> (r/request :post "/authorisation")
                               (assoc :params {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback"})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user] user)
                               ;; stale csrf token can cause session to be lost
                               (assoc-in [:session :csrf-token] "staleCSRFtoken"))
                   response (oauth/authorise request)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => (contains "callback?code=")
               (get-in response [:session :access_token]) => (:token access-token)
               (get-in response [:session :user]) => user)))

(defn encode-client-info [client]
  (format "Basic %s"
          (.encodeAsString
            (Base64.)
            (.getBytes
              (format "%s:%s"
                      (:client-id client)
                      (:client-secret
                        client))))))

(defn create-auth-header [request client]
  (assoc-in request [:headers "authorization"]
            (encode-client-info client)))

(facts "about token endpoint"
       (let [user-email "email@user.com"
             user (storage/store-user! user-email "password")
             client-details (client/register-client "MYAPP" "myapp.com")
             auth-code (auth-code/create-auth-code client-details user "callback")
             request (-> (r/request :get "/token")
                         (assoc :params {:grant_type   "authorization_code"
                                         :redirect_uri "callback"
                                         :code         (:code auth-code)})
                         (create-auth-header client-details))
             response (oauth/validate-token request)
             response-body (-> response :body (json/parse-string keyword))]

         (fact "request with grant type authorization_code and correct credentials returns access token"
               (:status response) => 200
               (:access_token response-body) => (just #"[A-Z0-9]{32}")
               (:token_type response-body) => "bearer")

         (fact "user-email in session is returned in body after validating token"
               (:user-email response-body) => user-email)

         (fact "user id in session is returned in body after validating token"
               (:user-id response-body) => (:uid user)
               (:uid user) =not=> nil?)

         (fact "user email stays in the session after validating token"
               (get-in response [:session :user :email]) => user-email)))
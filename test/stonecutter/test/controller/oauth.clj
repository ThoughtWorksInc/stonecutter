(ns stonecutter.test.controller.oauth
  (:require [midje.sweet :refer :all]
            [stonecutter.controller.oauth :as oauth]
            [ring.mock.request :as r]
            [clauth.client :as client]
            [clauth.token :as token]
            [clauth.user :as user]
            [clauth.auth-code :as auth-code]
            [cheshire.core :as json]
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
             (let [client-details (client/register-client "MyAPP" "myapp.com")
                   request (-> (r/request :get "/authorisation" {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback"})
                               (r/header "accept" "text/html"))
                   response (oauth/authorise request)]
               (:status response) => 302
               (-> response (get-in [:headers "Location"])) => "/login"
               (-> response (get-in [:session :return-to])) => (format "/authorisation?client_id=%s&response_type=code&redirect_uri=callback" (:client-id client-details))))

       (fact "valid request redirects to callback with auth_code when there is an existing user session"
             (let [user (user/register-user "user" "password")
                   client-details (client/register-client "MYAPP" "myapp.com") ; NB this saves into the client store
                   access-token (token/create-token client-details user) ; NB this saves into the token store
                   request (-> (r/request :get "/authorisation")
                               (assoc :params {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback" :access-token (:token access-token)})
                               (r/header "accept" "text/html")
                               (assoc-in [:session :access_token] (:token access-token)))
                   response (oauth/authorise request)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => (contains "callback?code=")
               (get-in response [:session :access_token]) => (:token access-token))))

(defn create-auth-header [request client]
  (assoc-in request [:headers "authorization"]
            (format "Basic %s"
                    (.encodeAsString
                      (Base64.)
                      (.getBytes
                        (format "%s:%s"
                                (:client-id client)
                                (:client-secret
                                  client)))))))
(facts "about token endpoint"
       (fact "request with grant type authorization_code and correct credentials returns access token"
             (let [user (user/register-user "user2" "password")
                   client-details (client/register-client "MYAPP" "myapp.com")
                   auth-code (auth-code/create-auth-code client-details user "callback")
                   request (-> (r/request :get "/token")
                               (assoc :params {:grant_type   "authorization_code"
                                               :redirect_uri "callback"
                                               :code         (:code auth-code)})
                               (create-auth-header client-details))
                   response (oauth/token request)
                   response-body (-> response :body (json/parse-string keyword))]
               (:status response) => 200
               (:access_token response-body) =not=> nil
               (:token_type response-body) => "bearer")))
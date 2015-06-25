(ns stonecutter.test.controller.oauth
  (:require [midje.sweet :refer :all]
            [stonecutter.controller.oauth :as oauth]
            [ring.mock.request :as r]
            [clauth.client :as client]
            [clauth.token :as token]
            [clauth.user :as user]
            ))

(facts "about authorisation end-point TODO RENAME ME"
       (fact "request with no parameters returns 403"
             (let [request (r/request :get "/authorisation")
                   response (oauth/authorise request)]
               (:status response) => 403))

       (fact "request with client-id, response_type and redirect_uri returns redirect to login page if there is no user session"
             (let [client-details  (client/register-client "MyAPP" "myapp.com")
                   request (-> (r/request :get "/authorisation" {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback"})
                               (r/header "accept" "text/html"))
                   response (oauth/authorise request)]
               (:status response) => 302
               (-> response (get-in [:headers "Location"])) => "/login"
               (-> response (get-in [:session :return-to])) => (format "/authorisation?client_id=%s&response_type=code&redirect_uri=callback" (:client-id client-details))))

       (fact "valid request does something TODO RENAME when there is an existing user session"
             (let [user (user/register-user "user" "password")
                   client-details (client/register-client "MYAPP" "myapp.com") ; NB this saves into the client store
                   access-token (token/create-token client-details user) ; NB this saves into the token store
                   request (-> (r/request :get "/authorisation")
                               (assoc :params {:client_id (:client-id client-details) :response_type "code" :redirect_uri "callback" :access-token (:token access-token)})
                               (r/header "accept" "text/html")
                               (assoc-in [:session :access_token] (:token access-token))
                               (dissoc :content-type)
                               (r/header "content-type" nil))
                   response (oauth/authorise request)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => (contains "callback?code=")
               (get-in response [:session :access_token]) => (:token access-token)
               )
             )
       )

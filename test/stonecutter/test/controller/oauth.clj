(ns stonecutter.test.controller.oauth
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as r]
            [clauth.client :as cl-client]
            [clauth.token :as cl-token]
            [clauth.auth-code :as cl-auth-code]
            [cheshire.core :as json]
            [stonecutter.routes :as routes]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.client :as client]
            [stonecutter.db.user :as user])
  (:import (org.apache.commons.codec.binary Base64)))

(defn create-request [method url params]
  (-> (r/request method url params)
      (assoc :params params)
      (assoc-in [:context :translator] {})))

(background
  (before :facts (storage/setup-in-memory-stores!)
          :after (storage/reset-in-memory-stores!)))

(def user-email "email@user.com")

(facts "about authorisation end-point"
       (fact "request with client-id, response_type and redirect_uri returns redirect to login page if there is no user session"
             (let [client-details (cl-client/register-client "ClientTestApp" "localhost:3001") ;FORMAT => {:client-secret "XLFCQRKJXSV3T6YQJL5ZJJVGFUJNT6LD", :client-id "RXBX6ZXAER5KPDSZ3ZCZJDOBS27FLDE7", :name "ClientTestApp", :url "localhost:3001"}
                   request (-> (create-request :get (routes/path :authorise)
                                               {:client_id (:client-id client-details)
                                                :response_type "code"
                                                :redirect_uri "callback"})
                               (r/header "accept" "text/html"))
                   response (oauth/authorise request)]
               (:status response) => 302
               (-> response (get-in [:headers "Location"])) => "/sign-in"
               (-> response (get-in [:session :return-to])) => (format "/authorisation?client_id=%s&response_type=code&redirect_uri=callback" (:client-id client-details))
               (-> response (get-in [:session :client-id])) => (:client-id client-details)))

       (fact "valid request goes to authorisation page with auth_code and email when there is an existing user session"
             (let [user (user/store-user! user-email "password")
                   client-details (cl-client/register-client "MYAPP" "myapp.com") ; NB this saves into the client store
                   access-token (cl-token/create-token client-details user) ; NB this saves into the token store
                   request (-> (create-request :get (routes/path :authorise)
                                               {:client_id (:client-id client-details)
                                                :response_type "code"
                                                :redirect_uri "callback"})
                               (r/header "accept" "text/html")
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user-login] (:login user))
                               (assoc-in [:context :translator] {})
                               ;the csrf-token key in session will stop clauth from refreshing the csrf-token in request
                               (assoc-in [:session :csrf-token] "staleCSRFtoken"))
                   response (oauth/authorise request)]
               (:status response) => 200
               (get-in response [:session :access_token]) => (:token access-token)
               (get response :body) => (contains "Share Profile Card")
               (get-in response [:session :user-login]) => user-email
               (get-in response [:session :csrf-token]) =not=> "staleCSRFtoken"))

       (fact "posting to authorisation endpoint redirects to callback with auth code and adds the client to the user's authorised clients"
             (let [user (user/store-user! user-email "password")
                   client-details (cl-client/register-client "MYAPP" "myapp.com")
                   access-token (cl-token/create-token client-details user)
                   csrf-token "CSRF-TOKEN"
                   request (-> (create-request :post (routes/path :authorise)
                                               {:client_id (:client-id client-details) :response_type "code"
                                                :redirect_uri "callback" :csrf-token csrf-token})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :csrf-token] csrf-token)
                               (assoc :content-type "application/x-www-form-urlencoded") ;To mock a form post
                               (assoc-in [:session :user-login] user-email))
                   response (oauth/authorise-client request)]
               (oauth/authorise-client request) => (contains {:status 302 :headers (contains {"Location" (contains "callback?code=" )} )})
               (provided
                 (user/add-authorised-client-for-user! user-email anything) => ...user...)))

       (fact "valid request redirects to callback with auth code when there is an existing user session and the user has previously authorised the app"
             (let [user (user/store-user! user-email "password")
                   client-details (cl-client/register-client "MYAPP" "myapp.com")
                   updated-user (user/add-authorised-client-for-user! user-email (:client-id client-details))
                   access-token (cl-token/create-token client-details user)
                   request (-> (create-request :get (routes/path :authorise)
                                               {:client_id (:client-id client-details) :response_type "code"
                                                :redirect_uri "callback"})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user-login] (:login user)))
                   response (oauth/authorise request)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => (contains "callback?code=")))

       (fact "user-login and access_token in session stay in session if user is logged in"
             (let [user (user/store-user! user-email "password")
                   client-details (cl-client/register-client "MYAPP" "myapp.com")
                   access-token (cl-token/create-token client-details user)
                   request (-> (create-request :post (routes/path :authorise) nil)
                               (assoc :params {:client_id (:client-id client-details)
                                               :response_type "code" :redirect_uri "callback"})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user-login] (:login user))
                               ;; stale csrf token can cause session to be lost
                               (assoc-in [:session :csrf-token] "staleCSRFtoken"))
                   response (oauth/authorise request)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => (contains "callback?code=")
               (get-in response [:session :access_token]) => (:token access-token)
               (get-in response [:session :user-login]) => user-email)))

(facts "about show-authorise-form"
       (fact "authorise form is rendered with client name"
             (let [element-has-correct-client-name-fn (fn [element] (= (html/text element) "CLIENT_NAME"))]
               (-> (create-request :get (routes/path :show-authorise-form) {:client_id "CLIENT_ID"})
                   oauth/show-authorise-form
                   :body
                   html/html-snippet
                   (html/select [:.clj--app-name])) => (has some element-has-correct-client-name-fn)
               (provided (client/retrieve-client "CLIENT_ID") => {:client-id "CLIENT_ID" :name "CLIENT_NAME"})))

       (fact "redirects to error 404 page if client_id doesn't match a registered client"
               (-> (create-request :get (routes/path :show-authorise-form) {:client_id "CLIENT_ID"})
                   oauth/show-authorise-form) => (contains {:status 404})
               (provided (client/retrieve-client "CLIENT_ID") => nil)))

(facts "about show-authorise-failure"
       (fact "when authorisation failure is rendered will add error=access_denied in the querystring of the callback uri"
             (let [request (-> (create-request :get "/authorise-failure" nil)
                               (assoc-in [:context :translator] {})
                               (assoc-in [:session :redirect-uri] "http://where.do.we.go.now"))
                   response (oauth/show-authorise-failure request)]
               (:status response)) => 200
             (provided
               (oauth/add-error-to-uri "http://where.do.we.go.now") => anything))

       (fact "authorisation failure page is rendered with client name"
             (let [element-has-correct-client-name-fn (fn [element] (= (html/text element) "CLIENT_NAME"))]
               (-> (create-request :get (routes/path :show-authorise-failure) nil)
                   (assoc-in [:session :client-id] "CLIENT_ID")
                   oauth/show-authorise-failure
                   :body
                   html/html-snippet
                   (html/select [:.clj--app-name])) => (has some element-has-correct-client-name-fn)
               (provided (client/retrieve-client "CLIENT_ID") => {:client-id "CLIENT_ID" :name "CLIENT_NAME"}))))

(fact "add-error-to-uri adds oauth error message to callback uri"
      (oauth/add-error-to-uri "https://client.com/callback") => "https://client.com/callback?error=access_denied")

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
       (let [user (user/store-user! user-email "password")
             client-details (cl-client/register-client "MYAPP" "myapp.com")
             auth-code (cl-auth-code/create-auth-code client-details user "callback")
             request (-> (create-request :get (routes/path :validate-token) {:grant_type   "authorization_code"
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
               (get-in response [:session :user-login]) => user-email)))

(facts "about auto-approver"
       (fact "returns true if user has authorised the client"
             (-> (create-request :get (routes/path :authorise) {:client_id ...client-id...})
                 (assoc-in [:session :user-login] ...email...)
                 oauth/auto-approver) => true
             (provided
               (user/is-authorised-client-for-user? ...email... ...client-id...) => true))

       (fact "returns false if user has not authorised the client"
             (-> (create-request :get (routes/path :authorise) {:client_id ...client-id...})
                 (assoc-in [:session :user-login] ...email...)
                 oauth/auto-approver) => false
             (provided
               (user/is-authorised-client-for-user? ...email... ...client-id...) => false)))

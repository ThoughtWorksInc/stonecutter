(ns stonecutter.test.controller.oauth
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as r]
            [clauth.client :as cl-client]
            [clauth.token :as cl-token]
            [clauth.auth-code :as cl-auth-code]
            [cheshire.core :as json]
            [hiccup.util :as hiccup]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.util.time :as t]
            [stonecutter.routes :as routes]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.db.client :as client]
            [stonecutter.db.user :as user]
            [stonecutter.db.mongo :as m]
            [stonecutter.config :as config]
            [monger.core :as monger])
  (:import (org.apache.commons.codec.binary Base64)))

(def user-email "email@user.com")
(def client-url "https://client-app.com")

(facts "about authorisation end-point"
       (fact "request with client-id, response_type and redirect_uri returns redirect to index page if there is no user session"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   auth-code-store (m/create-memory-store)
                   client-store (m/create-memory-store)
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")
                   client-details (cl-client/register-client client-store "ClientTestApp" client-url) ;FORMAT => {:client-secret "XLFCQRKJXSV3T6YQJL5ZJJVGFUJNT6LD", :client-id "RXBX6ZXAER5KPDSZ3ZCZJDOBS27FLDE7", :name "ClientTestApp", :url "localhost:3001"}
                   request (-> (th/create-request-with-query-string :get (routes/path :authorise)
                                                                    {:client_id     (:client-id client-details)
                                                                     :response_type "code"
                                                                     :redirect_uri  client-url})
                               (r/header "accept" "text/html"))
                   response (oauth/authorise auth-code-store client-store user-store token-store profile-picture-store request)]
               (:status response) => 302
               (-> response (get-in [:headers "Location"])) => "/"
               (-> response (get-in [:session :return-to])) => (format "/authorisation?client_id=%s&response_type=code&redirect_uri=%s" (:client-id client-details) (hiccup/url-encode client-url))))

       (fact "valid request goes to authorisation page with auth_code and email when there is an existing user session"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   auth-code-store (m/create-memory-store)
                   client-store (m/create-memory-store)
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")
                   user (th/store-user! user-store user-email "password")
                   client-details (cl-client/register-client client-store "MYAPP" client-url) ; NB this saves into the client store
                   access-token (cl-token/create-token token-store client-details user) ; NB this saves into the token store
                   request (-> (th/create-request-with-query-string :get (routes/path :authorise)
                                                                    {:client_id     (:client-id client-details)
                                                                     :response_type "code"
                                                                     :redirect_uri  client-url})
                               (r/header "accept" "text/html")
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user-login] (:login user))
                               (assoc-in [:context :translator] {})
                               ;the csrf-token key in session will stop clauth from refreshing the csrf-token in request
                               (assoc-in [:session :csrf-token] "staleCSRFtoken"))
                   response (oauth/authorise auth-code-store client-store user-store token-store profile-picture-store request)]
               (:status response) => 200
               (get-in response [:session :access_token]) => (:token access-token)
               (get response :body) => (contains "Share Profile Card")
               (get-in response [:session :user-login]) => user-email
               (get-in response [:session :csrf-token]) =not=> "staleCSRFtoken"))

       (fact "posting to authorisation endpoint redirects to callback with auth code and adds the client to the user's authorised clients"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   auth-code-store (m/create-memory-store)
                   client-store (m/create-memory-store)
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")
                   user (th/store-user! user-store user-email "password")
                   client-details (cl-client/register-client client-store "MYAPP" "myapp.com")
                   access-token (cl-token/create-token token-store client-details user)
                   csrf-token "CSRF-TOKEN"
                   request (-> (th/create-request-with-query-string :post (routes/path :authorise)
                                                                    {:client_id    (:client-id client-details) :response_type "code"
                                                                     :redirect_uri "callback" :csrf-token csrf-token})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :csrf-token] csrf-token)
                               (assoc :content-type "application/x-www-form-urlencoded") ;To mock a form post
                               (assoc-in [:session :user-login] user-email))]
               (oauth/authorise-client auth-code-store client-store user-store token-store profile-picture-store request) => (contains {:status 302 :headers (contains {"Location" (contains "callback?code=")})})
               (provided
                (user/add-authorised-client-for-user! user-store user-email anything) => ...user...)))

       (fact "valid request redirects to callback with auth code when there is an existing user session and the user has previously authorised the app"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   auth-code-store (m/create-memory-store)
                   client-store (m/create-memory-store)
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")
                   user (th/store-user! user-store user-email "password")
                   client-details (cl-client/register-client client-store "MYAPP" "https://myapp.com")
                   updated-user (user/add-authorised-client-for-user! user-store user-email (:client-id client-details))
                   access-token (cl-token/create-token token-store client-details user)
                   request (-> (th/create-request-with-query-string :get (routes/path :authorise)
                                                                    {:client_id    (:client-id client-details) :response_type "code"
                                                                     :redirect_uri "https://myapp.com/callback"})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user-login] (:login user)))
                   response (oauth/authorise auth-code-store client-store user-store token-store profile-picture-store request)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => (contains "callback?code=")))

       (fact "request to authorisation endpoint with an incorrect redirect url does not redirect to the invalid url"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   auth-code-store (m/create-memory-store)
                   client-store (m/create-memory-store)
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")
                   user (th/store-user! user-store user-email "password")
                   client-details (cl-client/register-client client-store "MYAPP" "https://myapp.com")
                   updated-user (user/add-authorised-client-for-user! user-store user-email (:client-id client-details))
                   access-token (cl-token/create-token token-store client-details user)
                   request (-> (th/create-request-with-query-string :get (routes/path :authorise)
                                                                    {:client_id    (:client-id client-details) :response_type "code"
                                                                     :redirect_uri "https://invalidcallback.com"})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user-login] (:login user)))
                   response (oauth/authorise auth-code-store client-store user-store token-store profile-picture-store request)]
               (:status response) =not=> 302))

       (fact "return-to session key is refreshed when accessing authorisation endpoint without being signed in"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   auth-code-store (m/create-memory-store)
                   client-store (m/create-memory-store)
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")
                   client-details (cl-client/register-client client-store "MYAPP" "https://myapp.com")
                   client-id (:client-id client-details)
                   redirect-uri "https://myapp.com/callback"
                   new-return-to-uri (->> {:uri          ...new-uri...
                                           :query-string ...query-string...
                                           :params       {:client_id     client-id
                                                          :response_type "code"
                                                          :redirect_uri  redirect-uri}
                                           :session      {:return-to ...old-return-to-uri...}}
                                          (oauth/authorise auth-code-store client-store user-store token-store profile-picture-store)
                                          :session
                                          :return-to)]
               new-return-to-uri => (contains ...new-uri...)
               new-return-to-uri => (contains ...query-string...)))

       (fact "user-login and access_token in session stay in session if user is logged in"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   auth-code-store (m/create-memory-store)
                   client-store (m/create-memory-store)
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")
                   user (th/store-user! user-store user-email "password")
                   client-details (cl-client/register-client client-store "MYAPP" client-url)
                   access-token (cl-token/create-token token-store client-details user)
                   request (-> (th/create-request-with-query-string :post (routes/path :authorise) nil)
                               (assoc :params {:client_id     (:client-id client-details)
                                               :response_type "code" :redirect_uri client-url})
                               (assoc-in [:session :access_token] (:token access-token))
                               (assoc-in [:session :user-login] (:login user))
                               ;; stale csrf token can cause session to be lost
                               (assoc-in [:session :csrf-token] "staleCSRFtoken"))
                   response (oauth/authorise auth-code-store client-store user-store token-store profile-picture-store request)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => (contains (str client-url "?code="))
               (get-in response [:session :access_token]) => (:token access-token)
               (get-in response [:session :user-login]) => user-email)))

(facts "about show-authorise-form"
       (fact "authorise form is rendered with client name"
             (let [client-store (m/create-memory-store)
                   user-store (m/create-memory-store)
                   user (th/store-user! user-store user-email "password")
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")
                   element-has-correct-client-name-fn (fn [element] (= (html/text element) "CLIENT_NAME"))
                   request (-> (th/create-request-with-query-string :get (routes/path :show-authorise-form) {:client_id "CLIENT_ID"})
                               (assoc-in [:session :user-login] (:login user)))]
               (-> (oauth/show-authorise-form client-store user-store profile-picture-store request)
                   :body
                   html/html-snippet
                   (html/select [:.clj--client-name])) => (has some element-has-correct-client-name-fn)
               (provided (client/retrieve-client client-store "CLIENT_ID") => {:client-id "CLIENT_ID" :name "CLIENT_NAME"})))

       (fact "returns nil (404) if client_id doesn't match a registered client"
             (let [client-store (m/create-memory-store)
                   user-store (m/create-memory-store)
                   conn (monger/connect "mongodb://localhost:27017/stonecutter-test")
                   profile-picture-store (monger/get-gridfs conn "stonecutter-test")]
               (->> (th/create-request-with-query-string :get (routes/path :show-authorise-form) {:client_id "CLIENT_ID"})
                    (oauth/show-authorise-form client-store user-store profile-picture-store)) => nil
               (provided (client/retrieve-client client-store "CLIENT_ID") => nil))))

(facts "about show-authorise-failure"
       (fact "when authorisation failure is rendered will add error=access_denied in the querystring of the callback uri"
             (let [client-store (m/create-memory-store)
                   request (-> (th/create-request-with-query-string :get "/authorise-failure"
                                                                    {:redirect_uri "http://where.do.we.go.now"})
                               (assoc-in [:context :translator] {}))
                   response (oauth/show-authorise-failure client-store request)]
               (:status response)) => 200
             (provided
               (oauth/add-error-to-uri "http://where.do.we.go.now") => anything))

       (fact "authorisation failure page is rendered with client name"
             (let [client-store (m/create-memory-store)
                   element-has-correct-client-name-fn (fn [element] (= (html/text element) "CLIENT_NAME"))
                   request (th/create-request-with-query-string :get (routes/path :show-authorise-failure)
                                                        {:client_id "CLIENT_ID"})]
               (-> (oauth/show-authorise-failure client-store request)
                   :body
                   html/html-snippet
                   (html/select [:.clj--client-name])) => (has some element-has-correct-client-name-fn)
               (provided (client/retrieve-client client-store "CLIENT_ID") => {:client-id "CLIENT_ID" :name "CLIENT_NAME"}))))

(fact "add-error-to-uri adds oauth error message to callback uri"
      (oauth/add-error-to-uri "https://client.com/callback") => "https://client.com/callback?error=access_denied")

(defn stub-id-token-generator [sub aud token-lifetime-minutes additional-claims]
  (merge {:sub sub :aud aud :token-lifetime-minutes token-lifetime-minutes} additional-claims))

(facts "about token endpoint"
       (let [user-store (m/create-memory-store)
             client-store (m/create-memory-store)
             token-store (m/create-memory-store)
             auth-code-store (m/create-memory-store)
             user (user/store-admin! user-store "first name" "last name" user-email "password")
             client-details (cl-client/register-client client-store "MYAPP" "http://myapp.com")]
         (facts "with empty scope"
                (let [config-m {}
                      auth-code (cl-auth-code/create-auth-code auth-code-store client-details user "http://myapp.com/callback")
                      request (th/create-request :post (routes/path :validate-token)
                                                 {:grant_type   "authorization_code"
                                                  :redirect_uri "http://myapp.com/callback"
                                                  :code         (:code auth-code)
                                                  :client_id (:client-id client-details)
                                                  :client_secret (:client-secret client-details)})
                      response (oauth/validate-token config-m
                                                     auth-code-store client-store user-store token-store
                                                     stub-id-token-generator request)
                      response-body (-> response :body (json/parse-string keyword))]

                  (fact "request with grant type authorization_code and correct credentials returns access token"
                        (:status response) => 200
                        (:access_token response-body) => (just #"[A-Z0-9]{32}")
                        (:token_type response-body) => "bearer")

                  (fact "user-email in the response body matches that of the authenticated user"
                        (get-in response-body [:user-info :email]) => user-email)

                  (fact "user-info includes :sub key matching the authenticated user's uid"
                        (let [sub (get-in response-body [:user-info :sub])]
                          sub => (:uid user)
                          sub =not=> nil?))

                  (fact "response should not include an id_token record"
                        (:id_token response-body) => nil?)

                  (fact "email confirmed status is returned in the body after validating token"
                        (get-in response-body [:user-info :email_verified]) => (:confirmed? user))

                  (fact "roles for admin is returned in the body after validating token"
                        (get-in response-body [:user-info :role]) => (name (:role user))))

                (fact "gives a 400 response when the client secret is invalid"
                      (let [config-m {}
                            auth-code (cl-auth-code/create-auth-code auth-code-store client-details user "http://myapp.com/callback")
                            request (th/create-request :post (routes/path :validate-token)
                                                       {:grant_type   "authorization_code"
                                                        :redirect_uri "http://myapp.com/callback"
                                                        :code         (:code auth-code)
                                                        :client_id (:client-id client-details)
                                                        :client_secret "Invalid client secret"})
                            response (oauth/validate-token config-m
                                                           auth-code-store client-store user-store token-store
                                                           stub-id-token-generator request)
                            response-body (-> response :body (json/parse-string keyword))]
                        
                        (:status response) => 400
                        response-body =not=> (contains {:access_token anything})
                        response-body =not=> (contains {:id_token anything})
                        response-body =not=> (contains {:user-info anything}))))

         (facts "about openid connect"
                (let [config-m {:base-url "http://stonecutter.base.url"
                                :open-id-connect-id-token-lifetime-minutes 10}
                      auth-code (cl-auth-code/create-auth-code auth-code-store client-details user "http://myapp.com/callback" "openid" nil)
                      expiry-seconds (t/to-epoch (:expires auth-code))
                      request (th/create-request :post (routes/path :validate-token)
                                                 {:grant_type   "authorization_code"
                                                  :redirect_uri "http://myapp.com/callback"
                                                  :code         (:code auth-code)
                                                  :client_id (:client-id client-details)
                                                  :client_secret (:client-secret client-details)})
                      response (oauth/validate-token config-m
                                                     auth-code-store client-store user-store token-store
                                                     stub-id-token-generator request)
                      response-body (-> response :body (json/parse-string keyword))]

                  (fact "request with authorization_code with a scope of openid uses id-token-generator to create an id_token which is returned in the response"
                        (:id_token response-body) => {:sub                    (:uid user)
                                                      :aud                    (:client-id client-details)
                                                      :token-lifetime-minutes 10
                                                      :email                  user-email
                                                      :email_verified         false
                                                      :role                   (:admin config/roles)})

                  (fact "response body should not contain user-info record"
                        (:user-info response-body) => nil?))

                (fact "gives a 400 response when the client secret is invalid"
                      (let [config-m {:base-url "http://stonecutter.base.url"
                                      :open-id-connect-id-token-lifetime-minutes 10}
                            auth-code (cl-auth-code/create-auth-code auth-code-store client-details user "http://myapp.com/callback" "openid" nil)
                            expiry-seconds (t/to-epoch (:expires auth-code))
                            request (th/create-request :post (routes/path :validate-token)
                                                       {:grant_type   "authorization_code"
                                                        :redirect_uri "http://myapp.com/callback"
                                                        :code         (:code auth-code)
                                                        :client_id (:client-id client-details)
                                                        :client_secret "Invalid client secret"})
                            response (oauth/validate-token config-m
                                                           auth-code-store client-store user-store token-store
                                                           stub-id-token-generator request)
                            response-body (-> response :body (json/parse-string keyword))]
                        
                        (:status response) => 400
                        response-body =not=> (contains {:access_token anything})
                        response-body =not=> (contains {:id_token anything})
                        response-body =not=> (contains {:user-info anything}))))))

(facts "about auto-approver"
       (fact "returns true if user has authorised the client"
             (->> (th/create-request-with-query-string :get (routes/path :authorise)
                                                       {:client_id ...client-id...} {:user-login ...email...})
                  (oauth/auto-approver ...user-store...)) => true
             (provided
               (user/is-authorised-client-for-user? ...user-store... ...email... ...client-id...) => true))

       (fact "returns false if user has not authorised the client"
             (->> (th/create-request-with-query-string :get (routes/path :authorise)
                                                       {:client_id ...client-id...} {:user-login ...email...})
                  (oauth/auto-approver ...user-store...)) => false
             (provided
               (user/is-authorised-client-for-user? ...user-store... ...email... ...client-id...) => false)))

(facts "about is-redirect-uri-valid?"

       (def correct-url "http://test.com")
       (def incorrect-url "http://danger-zone.com")

       (fact "returns true if redirect uri is the same as url in stored client"
             (oauth/is-redirect-uri-valid? ...client-store... ...client-id... correct-url) => true
             (provided
              (client/retrieve-client ...client-store... ...client-id...) => {:url correct-url}))

       (fact "returns false if redirect uri is NOT the same as url in stored client"
             (oauth/is-redirect-uri-valid? ...client-store... ...client-id... incorrect-url) => false
             (provided
              (client/retrieve-client ...client-store... ...client-id...) => {:url correct-url}))

       (tabular
         (fact "returns true as long as host is the same"
               (oauth/is-redirect-uri-valid? ...client-store... ...client-id... ?client-url) => true
               (provided
                (client/retrieve-client ...client-store... ...client-id...) => {:url correct-url}))

         ?client-url
         "https://test.com"
         "http://test.com"
         "http://test.com"
         "https://test.com/callback"
         "https://test.com/callback?params=1")

       (fact "returns true if redirect uri has the same root as url in stored client"
             (oauth/is-redirect-uri-valid? ...client-store... ...client-id... (str correct-url "/callback")) => true
             (provided
              (client/retrieve-client ...client-store... ...client-id...) => {:url correct-url}))

       (fact "returns nil when client-id or redirect-uri does not exist"
             (let [client-store (m/create-memory-store)]
               (oauth/is-redirect-uri-valid? client-store nil correct-url) => nil
               (oauth/is-redirect-uri-valid? client-store "123" nil) => nil))

       (fact "returns nil when client does not have url"
             (oauth/is-redirect-uri-valid? ...client-store... ...client-id... correct-url) => nil
             (provided
              (client/retrieve-client ...client-store... ...client-id...) => {})))

(fact "jwk-set returns the passed in string with json content type"
      (let [jwk-set-json "{\"json-web-key-set\": \"stuff\"}"
            response (oauth/jwk-set jwk-set-json {})]
        (:status response) => 200
        (:body response) => jwk-set-json
        (get-in response [:headers "Content-Type"]) => "application/json"))

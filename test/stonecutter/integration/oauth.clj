(ns stonecutter.integration.oauth
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter.handler :as h]
            [stonecutter.storage :as s]
            [ring.mock.request :as r]
            [clojure.string :as str]
            [clauth.client :as client]
            [clauth.token :as token]
            [clauth.user :as user]
            [stonecutter.integration.kerodon-helpers :as kh]
            [stonecutter.storage :as storage]))

;; CLIENT => AUTH    /authorisation?client-id=123&response_type=code&redirect_uri=callback-url
;;   USER LOGIN (Auth Server)
;;   ALLOW/DENY (Auth Server)  (Leave for later)
;; AUTH => CLIENT /callback-url?auth-code=blah
;; CLIENT => AUTH /token?client-id=123&client-secret=secret&code=code&redirect_uri=callback-url
;; AUTH => CLIENT /callback-url {:username "joebloggs" :accesstoken "1234123o487"}


; SCENARIO 1
; Given the user is not logged in, then auth server redirects user to log in page
;  after user has logged in, then auth server makes http call to client

; SCENARIO 2
; Given the user is logged in, then auth server makes http call to client wiht auth-code

; USER EXPERIENCE

; CLIENT:   Click login  (redirects to Stonecutter)
; STONECUTTER: Login with username and password  (redirect to Client)
;<- Auth/Client conversation
; CLIENT:   'You are logged in'

(defn browser-sends-authorisation-request-from-client-redirect [state client-id]
  (-> state
      (k/visit "/authorisation" :headers {"accept" "text/html"} :params {:client_id     client-id
                                                                         :response_type "code"
                                                                         :redirect_uri  "myclient.com/callback"})))

(defn get-auth-code [state]
  (when-let [location (-> state
                          :response
                          (get-in [:headers "Location"]))]
    (let [query-string (-> location
                           (str/split #"\?")
                           (peek))
          auth-code (-> {:query-string query-string}
                        (ring.middleware.params/params-request)
                        (get-in [:params "code"]))]
      auth-code)))

(defn client-sends-http-token-request [state client-id client-secret]
  (if-let [auth-code (get-auth-code state)]
    (-> state
        (dissoc :cookie-jar)                                ;; HTTP request so won't have browser cookie
        (k/visit "/api/token"
                 :request-method :post
                 :params {:grant_type    "authorization_code"
                          :redirect_uri  "myclient.com/callback"
                          :code          auth-code
                          :client_id     client-id
                          :client_secret client-secret}))
    (throw (Exception. "Unable to get auth-code"))))

(def email "email@server.com")
(def password "valid-password")

(defn setup []
  (s/reset-mongo-stores! "mongodb://localhost:27017/stonecutter-test")
  (let [client (client/register-client "myclient" "myclient.com")
        client-id (:client-id client)
        client-secret (:client-secret client)
        invalid-client-secret (str/reverse client-secret)
        user (storage/store-user! email password)]
    {:client-id             client-id
     :client-secret         client-secret
     :invalid-client-secret invalid-client-secret}))

(background
  (before :contents (s/reset-mongo-stores! "mongodb://localhost:27017/stonecutter-test")
          :after (s/reset-mongo-stores! "mongodb://localhost:27017/stonecutter-test")))

(defn print-debug [v] (prn "Kerodon:" v) v)

(facts "user can sign in through client"
       (let [{:keys [client-id client-secret]} (setup)]
         (-> (k/session h/app)
             (browser-sends-authorisation-request-from-client-redirect client-id)
             (k/follow-redirect)
             ;; login
             (kh/page-uri-is "/sign-in")
             (k/fill-in :.func--email__input email)
             (k/fill-in :.func--password__input password)
             (k/press :.func--sign-in__button)
             ;; check redirect - should have auth_code
             (k/follow-redirect)
             (kh/page-uri-is "/authorisation")
             (k/press :.func--authorise-share-profile__button)
             (kh/location-contains "callback?code=")
             (client-sends-http-token-request client-id client-secret)
             ; return 200 with new access_token
             (kh/response-has-access-token)
             (kh/response-has-user-email email)
             (kh/response-has-id))))

(facts "no access token will be issued with invalid credentials"
       (facts "user cannot sign in with invalid client secret"
              (let [{:keys [client-id invalid-client-secret]} (setup)]
                (-> (k/session h/app)
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (k/follow-redirect)
                    ;; login
                    (kh/page-uri-is "/sign-in")
                    (k/fill-in :.func--email__input email)
                    (k/fill-in :.func--password__input password)
                    (k/press :.func--sign-in__button)
                    ;; check redirect - should have auth_code
                    (k/follow-redirect)
                    (kh/page-uri-is "/authorisation")
                    (k/press :.func--authorise-share-profile__button)
                    (kh/location-contains "callback?code=")
                    (client-sends-http-token-request client-id invalid-client-secret)
                    :response
                    :status)) => 400)

       (facts "user cannot sign in with invalid password"
              (let [{:keys [client-id]} (setup)]
                (-> (k/session h/app)
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (k/follow-redirect)
                    ;; login
                    (kh/page-uri-is "/sign-in")
                    (k/fill-in :.func--email__input email)
                    (k/fill-in :.func--password__input "invalid-password")
                    (k/press :.func--sign-in__button)
                    :response
                    :body)) => (contains "Invalid email address or password")))

(ns stonecutter.integration.oauth
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter.handler :as h]
            [stonecutter.storage :as s]
            [ring.mock.request :as r]
            [clauth.client :as client]
            [clauth.user :as user]
            [stonecutter.integration.kerodon-helpers :as kh]
            [clojure.string :as str]))

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

(s/setup-in-memory-stores!)

(def client (client/register-client "myclient" "myclient.com"))
(def client-id (:client-id client))
(def client-secret (:client-secret client))
(def invalid-client-secret (str/reverse client-secret))

(def user (user/register-user "email@server.com" "valid-password"))

(defn browser-sends-authorisation-request-from-client-redirect [state client-id]
  (-> state
      (k/visit "/authorisation" :headers {"accept" "text/html"} :params {:client_id     client-id
                                                                         :response_type "code"
                                                                         :redirect_uri  "myclient.com/callback"})))

(defn debug [state]
  (prn "IN DEBUG" state)
  state)

(defn get-auth-code [state]
  (let [query-string (-> state
                         :response
                         (get-in [:headers "Location"])
                         (str/split #"\?")
                         (peek))
        auth-code (-> {:query-string query-string}
                      (ring.middleware.params/params-request)
                      (get-in [:params "code"]))]
    auth-code
    ))

(defn client-sends-http-token-request [state client-id client-secret]
  (let [auth-code (get-auth-code state)]
    (-> state
        (dissoc :cookie-jar)                                ;; HTTP request so won't have browser cookie
        (k/visit "/api/token"
                 :params {:grant_type    "authorization_code"
                          :redirect_uri  "myclient.com/callback"
                          :code          auth-code
                          :client_id     client-id
                          :client_secret client-secret}))))


(facts "user can sign in through client"
       (-> (k/session h/app)
           (browser-sends-authorisation-request-from-client-redirect client-id)
           (k/follow-redirect)
           ;; login
           (kh/page-uri-is "/sign-in")
           (k/fill-in :.func--email__input "email@server.com")
           (k/fill-in :.func--password__input "valid-password")
           (k/press :.func--sign-in__button)
           ;; check redirect - should have auth_code
           (k/follow-redirect)
           (kh/location-contains "callback?code=")
           (client-sends-http-token-request client-id client-secret)
           ;; return 200 with new access_token
           (kh/response-has-access-token)
           (kh/response-has-user-email "email@server.com")))

(facts "no access token will be issued with invalid credentials"
       (facts "user cannot sign in with invalid client secret"
              (-> (k/session h/app)
                  (browser-sends-authorisation-request-from-client-redirect client-id)
                  (k/follow-redirect)
                  ;; login
                  (kh/page-uri-is "/sign-in")
                  (k/fill-in :.func--email__input "email@server.com")
                  (k/fill-in :.func--password__input "valid-password")
                  (k/press :.func--sign-in__button)
                  ;; check redirect - should have auth_code
                  (k/follow-redirect)
                  (kh/location-contains "callback?code=")
                  (client-sends-http-token-request client-id invalid-client-secret)
                  :response
                  :status) => 400)

       (facts "user cannot sign in with invalid password"
              (-> (k/session h/app)
                  (browser-sends-authorisation-request-from-client-redirect client-id)
                  (k/follow-redirect)
                  ;; login
                  (kh/page-uri-is "/sign-in")
                  (k/fill-in :.func--email__input "email@server.com")
                  (k/fill-in :.func--password__input "invalid-password")
                  (k/press :.func--sign-in__button)
                  :response
                  :status) => 400))

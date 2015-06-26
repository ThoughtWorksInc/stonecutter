(ns stonecutter.integration.oauth
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter.handler :as h]
            [stonecutter.storage :as s]
            [ring.mock.request :as r]
            [clauth.client :as client]
            [clauth.user :as user]
            [stonecutter.integration.kerodon-helpers :as kh]))

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

(def user (user/register-user "email@server.com" "valid-password"))

(defn client-sends-authorisation-request [state client-id]
  (-> state
      (k/visit "/authorisation" :headers {"accept" "text/html"} :params {:client_id     client-id
                                                                         :response_type "code"
                                                                         :redirect_uri  "myclient.com/callback"})))

(defn debug [state]
  ;(prn "IN DEBUG" state)
  (prn "++++++++++++++++++++++++++++++++" (-> state :response (get-in [:headers "Location"])))
  state)

(defn client-sends-token-request [state client-id client-secret]
 (let [location-url (-> state :response (get-in [:headers "Location"]))]
   (prn "STATE " state)
   (prn "LOCATION URL" location-url)
   (prn "++++++URI+++" (-> {:uri location-url}
                           (ring.middleware.params/assoc-query-params "UTF-8")))
   )
  )

(facts "user can sign in through client"
       (prn "FAILING TEST")
       (-> (k/session h/app)
           (client-sends-authorisation-request client-id)
           (k/follow-redirect)

           ;; login
           (kh/page-uri-is "/login")
           (k/fill-in :.func--email__input "email@server.com")
           (k/fill-in :.func--password__input "valid-password")
           (k/press :.func--sign-in__button)
           ;; check redirect - should have auth_code
           (k/follow-redirect)
           (kh/location-contains "callback?code=")

           ;(client-sends-token-request client-id client-secret)

           ;; return redirects with new access_token
           ;; end of this test is to check that the server has granted approval
           ))

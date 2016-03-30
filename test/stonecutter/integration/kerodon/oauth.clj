(ns stonecutter.integration.kerodon.oauth
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [clojure.string :as string]
            [clauth.client :as cl-client]
            [stonecutter.db.storage :as s]
            [stonecutter.integration.kerodon.kerodon-checkers :as kc]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.integration.integration-helpers :as ih]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.config :as config]))

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
                                                                         :redirect_uri  "http://myclient.com/callback"})))

(defn get-auth-code [state]
  (when-let [location (-> state
                          :response
                          (get-in [:headers "Location"]))]
    (let [query-string (-> location
                           (string/split #"\?")
                           (peek))
          auth-code (-> {:query-string query-string}
                        (ring.middleware.params/params-request)
                        (get-in [:params "code"]))]
      auth-code)))

(defn client-sends-access-token-request [state client-id client-secret]
  (if-let [auth-code (get-auth-code state)]
    (-> state
        (dissoc :cookie-jar)                                ;; HTTP request so won't have browser cookie
        (k/visit "/api/token"
                 :request-method :post
                 :params {:grant_type    "authorization_code"
                          :redirect_uri  "http://myclient.com/callback"
                          :code          auth-code
                          :client_id     client-id
                          :client_secret client-secret}))
    (do (prn "============ Error: Unable to get auth-code")
        state)))

(def first-name "Frank")
(def last-name "Lasty")
(def email "email@server.com")
(def password "valid-password")

(defn setup [stores]
  (let [client (cl-client/register-client (s/get-client-store stores) "myclient" "http://myclient.com")
        client-id (:client-id client)
        client-secret (:client-secret client)
        invalid-client-secret (string/reverse client-secret)
        user (-> (s/get-user-store stores) (th/store-user! email password))]
    {:client-id             client-id
     :client-secret         client-secret
     :client-name           (:name client)
     :invalid-client-secret invalid-client-secret}))

(defn print-debug [v] (prn "Kerodon:" v) v)

(defn sign-in [state]
  (-> state
      (k/fill-in ks/sign-in-email-input email)
      (k/fill-in ks/sign-in-password-input password)
      (k/press ks/sign-in-submit)))

(facts "user authorising client-apps"
       (facts "user can sign in through client"
              (ih/setup-db)
              (let [stores (s/create-in-memory-stores (ih/get-test-db-connection))
                    {:keys [client-id client-secret]} (setup stores)]
                (-> (k/session (ih/build-app {:stores-m stores}))
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (kc/check-and-follow-redirect "to index")
                    ;; login
                    (kc/page-uri-is "/")
                    sign-in
                    ;; check redirect - should have auth_code
                    (kc/check-and-follow-redirect "to authorisation")
                    (kc/page-uri-is "/authorisation")
                    (kc/selector-includes-content [ks/profile-page-profile-card-email] "email@server.com")
                    (kc/selector-includes-content [ks/profile-page-profile-card-name] "Frank Lasty")
                    (kc/selector-has-attribute-with-content [ks/profile-page-profile-card-image :img] :src config/default-profile-picture)
                    (kc/check-and-press ks/authorise-share-profile-button)
                    (kc/location-contains "callback?code=")
                    (client-sends-access-token-request client-id client-secret)
                    ; return 200 with new access_token
                    (kc/response-has-access-token)
                    (kc/response-has-user-info))))

       (facts "user who has already authorised client does not need to authorise client again"
              (ih/setup-db)
              (let [stores (s/create-in-memory-stores (ih/get-test-db-connection))
                    {:keys [client-id client-secret client-name]} (setup stores)]
                (-> (k/session (ih/build-app {:stores-m stores}))
                    ;; authorise client for the first time
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (k/follow-redirect)
                    sign-in
                    (k/follow-redirect)
                    (k/press ks/authorise-share-profile-button)

                    ;; send authorisation request for the second time
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (kc/location-contains "callback?code=")
                    (client-sends-access-token-request client-id client-secret)
                    ;; return 200 with new access_token
                    (kc/response-has-access-token)
                    (kc/response-has-user-info))))

       (facts "user is redirected to authorisation-failure page when cancelling authorisation"
              (ih/setup-db)
              (let [stores (s/create-in-memory-stores (ih/get-test-db-connection))
                    {:keys [client-id client-secret]} (setup stores)]
                (-> (k/session (ih/build-app {:stores-m stores}))
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (k/follow-redirect)
                    ;; login
                    (kc/page-uri-is "/")
                    sign-in
                    ;; check redirect - should have auth_code
                    (k/follow-redirect)
                    (kc/page-uri-is "/authorisation")
                    (k/follow ks/authorise-cancel-link)
                    (kc/page-uri-is "/authorise-failure")
                    :response
                    :status)) => 200))

(facts "no access token will be issued with invalid credentials"
       (facts "user cannot sign in with invalid client secret"
              (ih/setup-db)
              (let [stores (s/create-in-memory-stores (ih/get-test-db-connection))
                    {:keys [client-id invalid-client-secret]} (setup stores)]
                (-> (k/session (ih/build-app {:stores-m stores}))
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (k/follow-redirect)
                    ;; login
                    (kc/page-uri-is "/")
                    sign-in
                    ;; check redirect - should have auth_code
                    (k/follow-redirect)
                    (kc/page-uri-is "/authorisation")
                    (k/press ks/authorise-share-profile-button)
                    (kc/location-contains "callback?code=")
                    (client-sends-access-token-request client-id invalid-client-secret)
                    :response
                    :status)) => 400)

       (facts "user cannot sign in with invalid password"
              (ih/setup-db)
              (let [stores (s/create-in-memory-stores (ih/get-test-db-connection))
                    {:keys [client-id]} (setup stores)]
                (-> (k/session (ih/build-app {:stores-m stores}))
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (k/follow-redirect)
                    ;; login
                    (kc/page-uri-is "/")
                    (k/fill-in ks/sign-in-email-input email)
                    (k/fill-in ks/sign-in-password-input "invalid-password")
                    (k/press ks/sign-in-submit)
                    :response
                    :body)) => (contains "Invalid email address or password")))

(ih/teardown-db)
(ns stonecutter.integration.kerodon.openid
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [ring.mock.request :as r]
            [clojure.string :as string]
            [cheshire.core :as json]
            [clauth.client :as cl-client]
            [stonecutter.handler :as h]
            [stonecutter.db.storage :as s]
            [stonecutter.db.mongo :as m]
            [stonecutter.integration.kerodon.kerodon-checkers :as kh]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.integration.integration-helpers :as ih]
            [stonecutter.db.user :as user]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.test.email :as test-email]))

(defn browser-sends-authorisation-request-from-client-redirect [state client-id]
  (-> state
      (k/visit "/authorisation" :headers {"accept" "text/html"}
               :params {:client_id     client-id
                        :response_type "code"
                        :scope         "openid"
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
    (throw (Exception. "Unable to get auth-code"))))

(def first-name "Frank")
(def last-name "Lasty")
(def email "email@server.com")
(def password "valid-password")

(defn setup [stores]
  (let [client (cl-client/register-client (s/get-client-store stores)  "myclient" "http://myclient.com")
        client-id (:client-id client)
        client-secret (:client-secret client)
        invalid-client-secret (string/reverse client-secret)
        user (-> (s/get-user-store stores) (th/store-user! email password))]
    {:client-id             client-id
     :client-secret         client-secret
     :client-name           (:name client)
     :invalid-client-secret invalid-client-secret}))

(defn print-debug [v] (prn "Kerodon:" v) v)

(defn print-cookie-jar [v] (prn (:cookie-jar v)) v)

(defn sign-in [state]
  (-> state
      (k/fill-in ks/sign-in-email-input email)
      (k/fill-in ks/sign-in-password-input password)
      (k/press ks/sign-in-submit)))

(def test-email-sender (test-email/create-test-email-sender))
(defn stub-token-generator [& args] "an-id-token")

(facts "user authorising client-apps using openid connect"
       (facts "user can sign in through client"
              (ih/setup-db)
              (let [stores (s/create-in-memory-stores (ih/get-test-db-connection))
                    {:keys [client-id client-secret]} (setup stores)]
                (-> (k/session (ih/build-app {:stores-m stores :token-generator stub-token-generator}))
                    (browser-sends-authorisation-request-from-client-redirect client-id)
                    (k/follow-redirect)
                    ;; login
                    (kh/page-uri-is "/")
                    sign-in
                    ;; check redirect - should have auth_code
                    (k/follow-redirect)
                    (kh/page-uri-is "/authorisation")
                    (k/press ks/authorise-share-profile-button)
                    (kh/location-contains "callback?code=")
                    (client-sends-access-token-request client-id client-secret)
                    ; return 200 with new access_token
                    (kh/response-has-access-token)
                    (kh/response-has-id-token-with-value "an-id-token")))))

(defn check-response-is-jwk-set [state]
  (fact {:midje/name "Checking if response is a jwk-set"}
        (let [response-body (-> state :response :body json/parse-string)]
          (get-in state [:response :status]) => 200
          (kh/response-type-is state "application/json")
          response-body => (contains {"keys" anything})))
  state)

(fact "about the jwk-set endpoint"
      (-> (k/session (ih/build-app {}))
          (k/visit "/api/jwk-set")
          (kh/page-uri-is "/api/jwk-set")
          check-response-is-jwk-set))

(ih/teardown-db)

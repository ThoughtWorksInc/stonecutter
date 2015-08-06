(ns stonecutter.test.controller.email-confirmations
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clauth.token :as cl-token]
            [clauth.user :as cl-user]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.controller.email-confirmations :as ec]
            [stonecutter.controller.user :as u]
            [stonecutter.db.user :as user]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.confirmation :as conf]
            [stonecutter.db.mongo :as m]
            [stonecutter.view.profile :as profile]))

(defn check-redirects-to [path]
  (checker [response] (and
                        (= (:status response) 302)
                        (= (get-in response [:headers "Location"]) path))))

(defn with-signed-in-user [ring-map token-store user]
  (let [access-token (cl-token/create-token token-store nil user)]
    (-> ring-map
        (assoc-in [:session :access_token] (:token access-token))
        (assoc-in [:session :user-login] (:login user)))))

(defn check-signed-in [request user]
  (let [is-signed-in? #(and (= (:login user) (get-in % [:session :user-login]))
                            (contains? (:session %) :access_token))]
    (checker [response]
             (let [session-not-changed (not (contains? response :session))]
               (or (and (is-signed-in? request)
                        session-not-changed)
                   (is-signed-in? response))))))

(def most-recent-email (atom nil))
(def email "dummy@email.com")
(def confirmation-id "RANDOM-ID-12345")
(def password "password123")

(defn test-email-sender! [email subject body]
  (reset! most-recent-email {:email   email
                             :subject subject
                             :body    body}))

(defn test-email-renderer [email-data]
  {:subject "confirmation"
   :body    email-data})

(def confirm-email-path
  (routes/path :confirm-email-with-id
               :confirmation-id confirmation-id))

(def confirm-email-request
  (th/create-request :get confirm-email-path {:confirmation-id confirmation-id}))

(background (before :facts (do (storage/setup-in-memory-stores!)
                               (email/initialise! test-email-sender!
                                                  {:confirmation test-email-renderer}))
                    :after (do (storage/reset-in-memory-stores!)
                               (email/reset-email-configuration!)
                               (reset! most-recent-email nil))))

(facts "about confirm-email-with-id"
       (fact "if the confirmation UUID in the query string matches that of the signed in user's user record confirm the account and redirect to profile view"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   user (user/store-user! user-store email "password")
                   confirmation (conf/store! @storage/confirmation-store email confirmation-id)
                   request (-> confirm-email-request
                               (with-signed-in-user token-store user))]
               (ec/confirm-email-with-id user-store request) => (check-redirects-to (routes/path :show-profile))
               (user/retrieve-user user-store (:login user)) =not=> (contains {:confirmation-id anything})
               (user/retrieve-user user-store (:login user)) => (contains {:confirmed? true})))

       (fact "when confirmation UUID in the query string does not match that of the signed in user's user record, signs the user out and redirects to confirmation endpoint with the original confirmation UUID from the query string"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   signed-in-user (user/store-user! user-store "signed-in@email.com" "password")
                   confirming-user (user/store-user! user-store "confirming@email.com" "password")
                   confirmation (conf/store! @storage/confirmation-store "confirming@email.com" confirmation-id)
                   request (-> confirm-email-request
                               (with-signed-in-user token-store signed-in-user))
                   response (ec/confirm-email-with-id user-store request)]
               response =not=> (check-signed-in request signed-in-user)
               response => (check-redirects-to confirm-email-path)))

       (fact "when user is not signed in, redirects to sign-in form with the confirmation endpoint (including confirmation UUID query string) as the successful sign-in redirect target"
             (let [user-store (m/create-memory-store)
                   confirming-user (user/store-user! user-store email "password")
                   confirmation (conf/store! @storage/confirmation-store email confirmation-id)
                   response (ec/confirm-email-with-id user-store confirm-email-request)]
               response => (check-redirects-to (routes/path :confirmation-sign-in-form
                                                            :confirmation-id confirmation-id))))

       (fact "when email confirmation is complete confirmation-id is revoked"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   user (user/store-user! user-store email "password")
                   confirmation (conf/store! @storage/confirmation-store email confirmation-id)
                   request (-> confirm-email-request
                               (with-signed-in-user token-store user))]
               (ec/confirm-email-with-id user-store request)
               (conf/fetch @storage/confirmation-store confirmation-id) => nil))

       (fact "when the confirmation id does not exist in the db"
             (let [user-store (m/create-memory-store)
                   token-store (m/create-memory-store)
                   user (user/store-user! user-store email "password")
                   request (-> confirm-email-request
                               (with-signed-in-user token-store user))]
               (ec/confirm-email-with-id user-store request) => (check-redirects-to (routes/path :sign-in)))))

(facts "about confirmation sign in"
       (fact "when password matches login of confirmation id, user is logged in")
       (->> (th/create-request :post (routes/path :confirmation-sign-in) {:confirmation-id confirmation-id :password password})
            (ec/confirmation-sign-in ...user-store... ...token-store...)) => (contains {:status  302
                                                                         :headers {"Location" confirm-email-path}
                                                                         :session {:user-login   ...user-login...
                                                                                   :access_token ...token...}})
       (provided
        (user/authenticate-and-retrieve-user ...user-store... email password) => {:login ...user-login...}
        (conf/fetch @storage/confirmation-store confirmation-id) => {:login email :confirmation-id confirmation-id}
        (cl-token/create-token ...token-store... nil {:login ...user-login...}) => {:token ...token...})

       (fact "when credentials are invalid, redirect back to form with invalid error"
             (against-background
              (user/authenticate-and-retrieve-user ...user-store... email "Invalid password") => nil
              (conf/fetch @storage/confirmation-store confirmation-id) => {:login email :confirmation-id confirmation-id})
             (let [response (->> (th/create-request :post (routes/path :confirmation-sign-in)
                                                    {:confirmation-id confirmation-id :password "Invalid password"})
                                 (ec/confirmation-sign-in ...user-store... ...token-store...))]
               response => (contains {:status 200})
               response =not=> (contains {:session {:user-login   anything
                                                    :access_token anything}})
               (-> (html/select (html/html-snippet (:body response)) [:.clj--validation-summary__item]) first :attrs :data-l8n)
               => "content:confirmation-sign-in-form/invalid-credentials-validation-message")))



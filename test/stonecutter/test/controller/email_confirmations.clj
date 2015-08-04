(ns stonecutter.test.controller.email-confirmations
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clauth.token :as cl-token]
            [clauth.user :as cl-user]
            [net.cgrand.enlive-html :as html]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.controller.email-confirmations :as ec]
            [stonecutter.controller.user :as u]
            [stonecutter.db.user :as user]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.confirmation :as conf]
            [stonecutter.view.profile :as profile]))

(defn check-redirects-to [path]
  (checker [response] (and
                        (= (:status response) 302)
                        (= (get-in response [:headers "Location"]) path))))

(defn with-signed-in-user [ring-map user]
  (let [access-token (cl-token/create-token @storage/token-store nil user)]
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

(defn create-request [method url params]
  (-> (mock/request method url)
      (assoc :params params)
      (assoc-in [:context :translator] {})))

(def most-recent-email (atom nil))
(def email "dummy@email.com")
(def confirmation-id "RANDOM-ID-12345")
(def password "password123")

(defn test-email-sender! [email subject body]
  (reset! most-recent-email {:email email
                             :subject subject
                             :body body}))

(defn test-email-renderer [email-data]
  {:subject "confirmation"
   :body email-data})

(def confirm-email-path
  (routes/path :confirm-email-with-id
               :confirmation-id confirmation-id))

(def confirm-email-request
  (create-request :get confirm-email-path {:confirmation-id confirmation-id}))

(background (before :facts (do (storage/setup-in-memory-stores!)
                               (cl-user/reset-user-store! @storage/user-store)
                               (email/initialise! test-email-sender!
                                                  {:confirmation test-email-renderer}))
                    :after (do (storage/reset-in-memory-stores!)
                               (email/reset-email-configuration!)
                               (reset! most-recent-email nil))))

(facts "about confirm-email-with-id"
       (fact "if the confirmation UUID in the query string matches that of the signed in user's user record confirm the account and redirect to profile view"
             (let [user (user/store-user! @storage/user-store email "password")
                   confirmation (conf/store! email confirmation-id)
                   request (-> confirm-email-request
                               (with-signed-in-user user))]
               (ec/confirm-email-with-id request) => (check-redirects-to (routes/path :show-profile))
               (user/retrieve-user @storage/user-store (:login user)) =not=> (contains {:confirmation-id anything})
               (user/retrieve-user @storage/user-store (:login user)) => (contains {:confirmed? true})))

       (fact "when confirmation UUID in the query string does not match that of the signed in user's user record, signs the user out and redirects to confirmation endpoint with the original confirmation UUID from the query string"
             (let [signed-in-user (user/store-user! @storage/user-store "signed-in@email.com" "password")
                   confirming-user (user/store-user! @storage/user-store "confirming@email.com" "password")
                   confirmation (conf/store! "confirming@email.com"  confirmation-id)
                   request (-> confirm-email-request
                               (with-signed-in-user signed-in-user))
                   response (ec/confirm-email-with-id request)]
               response =not=> (check-signed-in request signed-in-user)
               response => (check-redirects-to confirm-email-path)))

       (fact "when user is not signed in, redirects to sign-in form with the confirmation endpoint (including confirmation UUID query string) as the successful sign-in redirect target"
             (let [confirming-user (user/store-user! @storage/user-store email "password")
                   confirmation (conf/store! email confirmation-id)
                   response (ec/confirm-email-with-id confirm-email-request)]
               response => (check-redirects-to (routes/path :confirmation-sign-in-form
                                                            :confirmation-id confirmation-id))))

       (fact "when email confirmation is complete confirmation-id is revoked"
             (let [user (user/store-user! @storage/user-store email "password")
                   confirmation (conf/store! email confirmation-id)
                   request (-> confirm-email-request
                               (with-signed-in-user user))]
               (ec/confirm-email-with-id request)
               (conf/fetch confirmation-id) => nil)))

(facts "about confirmation sign in"
       (fact "when password matches login of confirmation id, user is logged in")
      (-> (create-request :post (routes/path :confirmation-sign-in) {:confirmation-id confirmation-id :password password}) 
          ec/confirmation-sign-in) => (contains {:status 302
                                                :headers {"Location" confirm-email-path}
                                                :session {:user-login ...user-login...
                                                          :access_token ...token...}})
      (provided
        (user/authenticate-and-retrieve-user @storage/user-store email password) => {:login ...user-login...}
        (conf/fetch confirmation-id) => {:login email :confirmation-id confirmation-id}
        (cl-token/create-token @storage/token-store nil {:login ...user-login...}) => {:token ...token...})
      
      (fact "when credentials are invalid, redirect back to form with invalid error"
            (against-background
              (user/authenticate-and-retrieve-user @storage/user-store email "Invalid password") => nil
              (conf/fetch confirmation-id) => {:login email :confirmation-id confirmation-id})
            (let [response (-> (create-request :post (routes/path :confirmation-sign-in)
                                               {:confirmation-id confirmation-id :password "Invalid password"}) 
                               ec/confirmation-sign-in)] 
              response => (contains {:status 200})
              response =not=> (contains {:session {:user-login anything
                                                   :access_token anything}})
              (-> (html/select (html/html-snippet (:body response)) [:.clj--validation-summary__item]) first :attrs :data-l8n) 
              => "content:confirmation-sign-in-form/invalid-credentials-validation-message")))
       


(ns stonecutter.test.controller.email-confirmations
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clauth.token :as cl-token]
            [clauth.user :as cl-user]
            [net.cgrand.enlive-html :as html]
            [stonecutter.toggles :as toggles]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.controller.user :as u]
            [stonecutter.db.user :as user]
            [stonecutter.db.storage :as storage]
            [stonecutter.view.profile :as profile]))

(defn check-redirects-to [path]
  (checker [response] (and
                        (= (:status response) 302)
                        (= (get-in response [:headers "Location"]) path))))

(defn with-signed-in-user [ring-map user]
  (let [access-token (cl-token/create-token nil user)]
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

(defn test-email-sender! [email subject body]
  (reset! most-recent-email {:email email
                             :subject subject
                             :body body}))

(defn test-email-renderer [email-data]
  {:subject "confirmation"
   :body email-data})

(background (before :facts (do (storage/setup-in-memory-stores!)
                               (cl-user/reset-user-store!)
                               (email/initialise! test-email-sender!
                                                  {:confirmation test-email-renderer}))
                    :after (do (storage/reset-in-memory-stores!)
                               (email/reset-email-configuration!)
                               (reset! most-recent-email nil))))

(facts "about confirm-email-with-id"
       (fact "if the confirmation UUID in the query string matches that of the signed in user's user record confirm the account and redirect to profile view"
             (let [user (user/store-user! "dummy@email.com" "password")
                   request (-> (create-request :get (routes/path :confirm-email-with-id
                                                                 :confirmation-id (:confirmation-id user))
                                               {:confirmation-id (:confirmation-id user)})
                               (with-signed-in-user user))]
               (u/confirm-email-with-id request) => (check-redirects-to (routes/path :show-profile))
               (user/retrieve-user (:login user)) =not=> (contains {:confirmation-id anything})
               (user/retrieve-user (:login user)) => (contains {:confirmed? true})))

       (fact "when confirmation UUID in the query string does not match that of the signed in user's user record, signs the user out and redirects to confirmation endpoint with the original confirmation UUID from the query string"
             (let [signed-in-user (user/store-user! "signed-in@email.com" "password")
                   confirming-user (user/store-user! "confirming@email.com" "password")
                   request (-> (create-request :get (routes/path :confirm-email-with-id
                                                                 :confirmation-id (:confirmation-id confirming-user))
                                               {:confirmation-id (:confirmation-id confirming-user)})
                               (with-signed-in-user signed-in-user))
                   response (u/confirm-email-with-id request)]
               response =not=> (check-signed-in request signed-in-user)
               response => (check-redirects-to (routes/path :confirm-email-with-id
                                                            :confirmation-id (:confirmation-id confirming-user)))))

       (fact "when user is not signed in, redirects to sign-in form with the confirmation endpoint (including confirmation UUID query string) as the successful sign-in redirect target"
             (let [confirming-user (user/store-user! "confirming@email.com" "password")
                   request (create-request :get (routes/path :confirm-email-with-id
                                                             :confirmation-id (:confirmation-id confirming-user))
                                           {:confirmation-id (:confirmation-id confirming-user)})
                   response (u/confirm-email-with-id request)]
               response => (check-redirects-to (routes/path :confirmation-sign-in-form
                                                            :confirmation-id (:confirmation-id confirming-user))))))

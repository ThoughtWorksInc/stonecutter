(ns stonecutter.test.controller.forgotten-password
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.controller.forgotten-password :as fp]
            [stonecutter.routes :as routes]
            [stonecutter.email :as email]
            [stonecutter.test.email :as test-email]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as user]
            [clauth.store :as cl-store]))

(def email-address "email@address.com")
(def forgotten-password-id "SOME-UUID")

(fact "about validation checks on posting email address"
      (let [email-sender (test-email/create-test-email-sender)
            response (->> (th/create-request :post "/forgotten-password" {:email "invalid email"})
                          (fp/forgotten-password-form-post email-sender nil nil))]
        (-> response
            :body
            html/html-snippet
            (html/select [:.form-row--validation-error])) =not=> empty?
        (test-email/last-sent-email email-sender) => nil))

(defn test-renderer [email-data]
  {:subject "TEST EMAIL"
   :body email-data})

(background (email/get-forgotten-password-renderer) => test-renderer)

(fact "about valid email address"
      (let [user-store (m/create-memory-store)
            user (user/store-user! user-store "admin@admin.admin" "password")]

        (fact "if user exists then forgotten-password-id is created and stored, e-mail is sent, and user is redirected to confirmation page"
              (let [email-sender (test-email/create-test-email-sender)
                    forgotten-password-store (m/create-memory-store)
                    test-request (-> (th/create-request :post "/forgotten-password" {:email "admin@admin.admin"})
                                     (th/add-config-request-context {:app-name "My App" :base-url "https://myapp.com"}))]
                (fp/forgotten-password-form-post email-sender user-store forgotten-password-store test-request) => (th/check-redirects-to (routes/path :show-forgotten-password-confirmation))
                
                (let [last-email (test-email/last-sent-email email-sender)
                      forgotten-password-id (-> last-email :body :forgotten-password-id)
                      forgotten-password-entry (cl-store/fetch forgotten-password-store forgotten-password-id)]
                  last-email => {:email   "admin@admin.admin"
                                 :subject "TEST EMAIL"
                                 :body    {:forgotten-password-id forgotten-password-id :app-name "My App" :base-url "https://myapp.com"}}
                  (:login forgotten-password-entry) => "admin@admin.admin")))

        (fact "if user doesn't exist then e-mail is not sent and forgotten password id is not store"
              (let [email-sender (test-email/create-test-email-sender)
                    forgotten-password-store (m/create-memory-store)
                    test-request (-> (th/create-request :post "/forgotten-password" {:email "nonexistent@blah.com"})
                                     (th/add-config-request-context {:app-name "My App" :base-url "https://myapp.com"}))]
                (fp/forgotten-password-form-post email-sender user-store forgotten-password-store test-request) => (th/check-redirects-to (routes/path :show-forgotten-password-confirmation))
                (test-email/last-sent-email email-sender) => nil
                (cl-store/entries forgotten-password-store) => empty?))))

(facts "about reset password form"
       (fact "if the forgotten-password-id in the URL corresponds to a non-expired forgotten-password record, the reset password form is displayed"
             (let [user-store (m/create-memory-store)
                   user (user/store-user! user-store email-address "old-password")
                   forgotten-password-store (m/create-memory-store)
                   forgotten-password-entry (cl-store/store! forgotten-password-store :forgotten-password-id
                                                             {:forgotten-password-id forgotten-password-id
                                                              :login email-address})
                   test-request (th/create-request :get (routes/path :show-reset-password-form
                                                                     :forgotten-password-id forgotten-password-id)
                                                   {:forgotten-password-id forgotten-password-id})
                   response (fp/show-reset-password-form forgotten-password-store user-store test-request)]
               (:status response) => 200))

       (fact "if there is no forgotten password record with an id matching that in the URL, a 404 is returned"
             (let [user-store (m/create-memory-store)
                   forgotten-password-store (m/create-memory-store)
                   test-request (th/create-request :get (routes/path :show-reset-password-form
                                                                     :forgotten-password-id forgotten-password-id)
                                                   {:forgotten-password-id forgotten-password-id})]
               (:status (fp/show-reset-password-form forgotten-password-store user-store test-request)) => nil))

       (fact "if a non-expired forgotten-password record can be found, but there is no corresponding user, a 404 is returned"
             (let [user-store (m/create-memory-store)
                   forgotten-password-store (m/create-memory-store)
                   forgotten-password-entry (cl-store/store! forgotten-password-store :forgotten-password-id
                                                             {:forgotten-password-id forgotten-password-id
                                                              :login email-address})
                   test-request (th/create-request :get (routes/path :show-reset-password-form
                                                                     :forgotten-password-id forgotten-password-id)
                                                   {:forgotten-password-id forgotten-password-id})]
               (:status (fp/show-reset-password-form forgotten-password-store user-store test-request)) => nil)))

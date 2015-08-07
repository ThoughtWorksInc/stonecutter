(ns stonecutter.test.controller.forgotten-password
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.controller.forgotten-password :as fp]
            [stonecutter.email :as email]
            [stonecutter.test.email :as test-email]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as user]
            [clauth.store :as cl-store]))

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

(email/initialise! {:forgotten-password test-renderer})

(fact "about valid email address"
      (let [user-store (m/create-memory-store)
            user (user/store-user! user-store "admin@admin.admin" "password")]

        (fact "if user exists then forgotten-password-id is created and stored and e-mail is sent"
              (let [email-sender (test-email/create-test-email-sender)
                    forgotten-password-store (m/create-memory-store)
                    test-request (-> (th/create-request :post "/forgotten-password" {:email "admin@admin.admin"})
                                     (th/add-config-request-context {:app-name "My App" :base-url "https://myapp.com"}))]
                (fp/forgotten-password-form-post email-sender user-store forgotten-password-store test-request) => (contains {:body "email sent"})
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
                (fp/forgotten-password-form-post email-sender user-store forgotten-password-store test-request) => (contains {:body "email sent"})
                (test-email/last-sent-email email-sender) => nil
                (cl-store/entries forgotten-password-store) => empty?))))

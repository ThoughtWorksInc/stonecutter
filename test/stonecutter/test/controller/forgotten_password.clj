(ns stonecutter.test.controller.forgotten-password
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.controller.forgotten-password :as fp]
            [stonecutter.email :as email]
            [stonecutter.test.email :as test-email]
            [stonecutter.db.mongo :as m]
            [stonecutter.db.user :as user]))

(fact "about validation checks on posting email address"
      (let [email-sender (test-email/create-test-email-sender)
            response (->> (th/create-request :post "/forgotten-password" {:email "invalid email"})
                          (fp/forgotten-password-form-post email-sender))]
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
      (fact "if user exists then e-mail is sent"
            (let [user-store (m/create-memory-store)
                  user (user/store-user! user-store "admin@admin.admin" "password")
                  email-sender (test-email/create-test-email-sender)
                  test-request (-> (th/create-request :post "/forgotten-password" {:email "admin@admin.admin"})
                                   (th/add-config-request-context {:app-name "My App" :base-url "https://myapp.com"}))]
              (fp/forgotten-password-form-post email-sender test-request)
              (test-email/last-sent-email email-sender) => {:email "admin@admin.admin"
                                                            :subject "TEST EMAIL"
                                                            :body {:forgotten-password-id "" :app-name "My App" :base-url "https://myapp.com"}}))
      (future-fact "if user doesn't exist then e-mail is not sent"

            )
      )

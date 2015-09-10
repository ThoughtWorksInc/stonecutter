(ns stonecutter.test.view.forgotten-password
  (:require [midje.sweet :refer :all]
            [stonecutter.view.forgotten-password :as fp]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]))

(fact
  (th/test-translations "Forgotten password form" fp/forgotten-password-form))

(fact "fp has correct action and method set"
      (let [form (-> (fp/forgotten-password-form {})
                     (html/select [:form])
                     first)]
        (-> form :attrs :action) => (r/path :send-forgotten-password-email)
        (-> form :attrs :method) => "post"))

(fact "error classes are not present when there are no validation errors"
      (-> (th/create-request)
          fp/forgotten-password-form
          (html/select [:.form-row--invalid])) => empty?)

(fact "about displaying email validation errors"
      (fact "when email is invalid"
            (let [errors {:email :invalid}
                  params {:email "invalid"}
                  page (-> (th/create-request {} errors params) fp/forgotten-password-form)]

              (fact "the class for styling errors is added"
                    (html/select page [[:.clj--forgotten-password-email :.form-row--invalid]]) =not=> empty?)
              
              (fact "email validation element is present"
                    (html/select page [:.clj--forgotten-password-email__validation]) =not=> empty?)
              
              (fact "correct error message is displayed"
                    (html/select page [[:.clj--forgotten-password-email__validation (html/attr= :data-l8n "content:forgot-password/email-address-invalid-validation-message")]]) =not=> empty?)

              (fact "non-existent user validation error"
                    (let [page (-> (th/create-request {} {:email :non-existent} params) fp/forgotten-password-form)]
                      (html/select page [[:.clj--forgotten-password-email__validation (html/attr= :data-l8n "content:forgot-password/email-address-non-existent-validation-message")]]) =not=> empty?))
              
              (fact "invalid value is preserved in input field"
                    (-> (html/select page [:.clj--forgotten-password-email__input]) first :attrs :value) => "invalid")))

      (fact "when email is too long"
            (let [long-email-address (apply str (repeat 255 "x"))
                  errors {:email :too-long}
                  params {:email long-email-address}
                  page (-> (th/create-request {} errors params) fp/forgotten-password-form)]
              (fact "the class for styling errors is added"
                    (html/select page [[:.clj--forgotten-password-email :.form-row--invalid]]) =not=> empty?)
              (fact "email validation element is present"
                    (html/select page [:.clj--forgotten-password-email__validation]) =not=> empty?)
              (fact "correct error message is displayed"
                    (html/select page [[:.clj--forgotten-password-email__validation (html/attr= :data-l8n "content:forgot-password/email-address-too-long-validation-message")]]) =not=> empty?))))

(facts "about flash messages"
       (fact "no flash messages are displayed by default"
             (let [page (-> (th/create-request) fp/forgotten-password-form)]
               (-> page (html/select [:.clj--flash-message-container])) => empty?))

       (fact "expired-reset flash message is displayed on page if it is in the flash of request"
             (let [page (-> (th/create-request) (assoc :flash :expired-password-reset) fp/forgotten-password-form)]
               (-> page (html/select [:.clj--flash-message-container])) =not=> empty?)))


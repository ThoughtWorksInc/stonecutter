(ns stonecutter.test.view.index
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.view.index :refer [index]]
            [stonecutter.helper :as helper]))


(facts "about index page"
       (let [page (-> (th/create-request) index)]
         (fact "index page should return some html"
               (html/select page [:form]) =not=> empty?)

         (fact "work in progress should be removed from page"
               page => th/work-in-progress-removed)

         (fact "registration form posts to correct endpoint"
               page => (th/has-form-action? [:.clj--register__form] (r/path :register-user)))
         (fact "sign in form posts to correct endpoint"
               page => (th/has-form-action? [:.clj--sign-in__form] (r/path :sign-in)))
         (fact "forgotten-password button should link to correct page"
               page => (th/has-attr? [:.clj--forgot-password]
                                     :href (r/path :show-forgotten-password-form)))))

(fact
 (th/test-translations "index page" index))

(facts "sign-in error classes are not present when there are no errors"
       (let [page (-> (th/create-request)
                      index)]
         (fact "no elements have class for styling errors"
               page => (th/element-absent? [:.form-row--validation-error]))
         (fact "email validation element is removed"
               page => (th/element-absent? [:.clj--sign-in-email__validation]))
         (fact "password validation element is removed"
               page => (th/element-absent? [:.clj--sign-in-password__validation]))
         (fact "validation summary element is removed"
               page => (th/element-absent? [:.clj--validation-summary]))))

(future-facts "about displaying errors"
       (facts "when email is invalid"
              (let [errors {:email :invalid}
                    params {:email "invalid"}
                    page (-> (th/create-request {} errors params) sign-in-form)]
                (fact "the class for styling errors is added"
                      page => (th/element-exists? [[:.clj--sign-in-email :.form-row--validation-error]]))
                (fact "email validation element is present"
                      page => (th/element-exists? [:.clj--sign-in-email__validation]))
                (fact "correct error message is displayed"
                      page => (th/has-attr? [:.clj--sign-in-email__validation]
                                            :data-l8n "content:sign-in-form/email-address-invalid-validation-message"))
                (fact "invalid value is preserved in input field"
                      page => (th/has-attr? [:.clj--email__input] :value "invalid"))))

       (facts "when email is too long"
              (let [long-email-address (apply str (repeat 255 "x"))
                    errors {:email :too-long}
                    params {:email long-email-address}
                    page (-> (th/create-request {} errors params) sign-in-form)]
                
                (fact "the class for styling errors is added"
                      page => (th/element-exists? [[:.clj--sign-in-email :.form-row--validation-error]]))
                (fact "email validation element is present"
                      page => (th/element-exists? [:.clj--sign-in-email__validation]))
                
                (fact "correct error message is displayed"
                      page => (th/has-attr? [:.clj--sign-in-email__validation]
                                            :data-l8n "content:sign-in-form/email-address-too-long-validation-message"))))

       (fact "when password is blank"
             (let [errors {:password :blank}
                   params {:password ""}
                   page (-> (th/create-request {} errors params) sign-in-form)]
               (fact "the class for styling errors is added"
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--validation-error]]))
               (fact "password validation element is present"
                     page => (th/element-exists? [:.clj--sign-in-password__validation]))
               
               (fact "correct error message is displayed"
                     page => (th/has-attr? [:.clj--sign-in-password__validation]
                                           :data-l8n "content:sign-in-form/password-blank-validation-message"))))

       (fact "when password is too short"
             (let [errors {:password :too-short}
                   params {:password "short"}
                   page (-> (th/create-request {} errors params) sign-in-form)]
               (fact "the class for styling errors is added"
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--validation-error]]))
               (fact "password validation element is present"
                     page => (th/element-exists? [:.clj--sign-in-password__validation]))
               
               (fact "correct error message is displayed"
                     page => (th/has-attr? [:.clj--sign-in-password__validation]
                                           :data-l8n "content:sign-in-form/password-too-short-validation-message"))))

       (fact "when password is too long"
             (let [long-password (apply str (repeat 255 "x"))
                   errors {:password :too-long}
                   params {:password long-password}
                   page (-> (th/create-request {} errors params) sign-in-form)]
               (fact "the class for styling errors is added"
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--validation-error]]))
               (fact "password validation element is present"
                     page => (th/element-exists? [:.clj--sign-in-password__validation]))
               
               (fact "correct error message is displayed"
                     page => (th/has-attr? [:.clj--sign-in-password__validation]
                                           :data-l8n "content:sign-in-form/password-too-long-validation-message")))))

(future-facts "about confirmation sign in form"
       (let [page (-> (th/create-request) confirmation-sign-in-form)]
         (fact "confirmation sign-in-form should return some html"
               page => (th/element-exists? [:form]))

         (fact "there are no missing translations"
               (th/test-translations "confirmation sign in form" confirmation-sign-in-form))

         (fact "work in progress should be removed from page"
               page => th/work-in-progress-removed)

         (fact "form should post to correct endpoint"
               page => (th/has-form-action? (r/path :confirmation-sign-in)))

         (fact "confirmation id should be set in form"
               (let [confirmation-id "confirmation-123"
                     page (-> (th/create-request {} nil {:confirmation-id confirmation-id}) confirmation-sign-in-form)]
                 page => (th/has-attr? [:.clj--confirmation-id__input] :value confirmation-id)))

         (fact "forgotten-password button should link to correct page"
               page => (th/has-attr? [:.clj--forgot-password]
                                     :href (r/path :show-forgotten-password-form)))))

(future-facts "about displaying errors"
       (facts "when password is invalid"
              (let [errors {:credentials :confirmation-invalid}
                    page (-> (th/create-request {} errors {}) confirmation-sign-in-form)]
                
                (fact "credentials validation element is present"
                      page => (th/element-exists? [:.validation-summary--show]))
                
                (fact "correct error message is displayed"
                      page => (th/has-attr? [:.clj--validation-summary__item]
                                            :data-l8n "content:confirmation-sign-in-form/invalid-credentials-validation-message"))
                
                (fact "invalid value is not preserved in input field"
                      (-> page (html/select [:.func--password__input]) first :attrs :value) => empty?))))

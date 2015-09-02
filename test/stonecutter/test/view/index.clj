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

(facts "about displaying sign-in errors"
       (facts "when email is invalid"
              (let [errors {:sign-in-email :invalid}
                    params {:sign-in-email "invalid"}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      page => (th/element-exists? [[:.clj--sign-in-email :.form-row--validation-error]]))
                (fact "email validation element is present"
                      page => (th/element-exists? [:.clj--sign-in-email__validation]))
                (fact "correct error message is displayed"
                      page => (th/has-attr? [:.clj--sign-in-email__validation]
                                            :data-l8n "content:index/sign-in-email-address-invalid-validation-message"))
                (fact "invalid value is preserved in input field"
                      page => (th/has-attr? [:.clj--email__input] :value "invalid"))))

       (facts "when email is too long"
              (let [long-email-address (apply str (repeat 255 "x"))
                    errors {:sign-in-email :too-long}
                    params {:sign-in-email long-email-address}
                    page (-> (th/create-request {} errors params) index)]
                
                (fact "the class for styling errors is added"
                      page => (th/element-exists? [[:.clj--sign-in-email :.form-row--validation-error]]))
                (fact "email validation element is present"
                      page => (th/element-exists? [:.clj--sign-in-email__validation]))
                
                (fact "correct error message is displayed"
                      page => (th/has-attr? [:.clj--sign-in-email__validation]
                                            :data-l8n "content:index/sign-in-email-address-too-long-validation-message"))))

       (fact "when password is blank"
             (let [errors {:sign-in-password :blank}
                   params {:sign-in-password ""}
                   page (-> (th/create-request {} errors params) index)]
               (fact "the class for styling errors is added"
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--validation-error]]))
               (fact "password validation element is present"
                     page => (th/element-exists? [:.clj--sign-in-password__validation]))
               
               (fact "correct error message is displayed"
                     page => (th/has-attr? [:.clj--sign-in-password__validation]
                                           :data-l8n "content:index/sign-in-password-blank-validation-message"))))

       (fact "when password is too short"
             (let [errors {:sign-in-password :too-short}
                   params {:sign-in-password "short"}
                   page (-> (th/create-request {} errors params) index)]
               (fact "the class for styling errors is added"
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--validation-error]]))
               (fact "password validation element is present"
                     page => (th/element-exists? [:.clj--sign-in-password__validation]))
               
               (fact "correct error message is displayed"
                     page => (th/has-attr? [:.clj--sign-in-password__validation]
                                           :data-l8n "content:index/sign-in-password-too-short-validation-message"))))

       (fact "when password is too long"
             (let [long-password (apply str (repeat 255 "x"))
                   errors {:sign-in-password :too-long}
                   params {:sign-in-password long-password}
                   page (-> (th/create-request {} errors params) index)]
               (fact "the class for styling errors is added"
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--validation-error]]))
               (fact "password validation element is present"
                     page => (th/element-exists? [:.clj--sign-in-password__validation]))

               (fact "correct error message is displayed"
                     page => (th/has-attr? [:.clj--sign-in-password__validation]
                                           :data-l8n "content:index/sign-in-password-too-long-validation-message")))))

(facts "about removing elements when there are no registration errors"
       (let [page (-> (th/create-request)
                      index)]
         (fact "no elements have class for styling errors"
               (html/select page [:.form-row--validation-error]) => empty?)
         (fact "email validation element is removed"
               (html/select page [:.clj--registration-email__validation]) => empty?)
         (fact "password validation element is removed"
               (html/select page [:.clj--registration-password__validation]) => empty?)
         (fact "validation summary element is removed"
               (html/select page [:.clj--validation-summary]) => empty?)))

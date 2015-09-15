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
               page => (th/has-form-action? [:.clj--register__form] (r/path :sign-in-or-register)))
         (fact "sign in form posts to correct endpoint"
               page => (th/has-form-action? [:.clj--sign-in__form] (r/path :sign-in-or-register)))
         (fact "forgotten-password button should link to correct page"
               page => (th/has-attr? [:.clj--forgot-password]
                                     :href (r/path :show-forgotten-password-form)))))

(fact
 (th/test-translations "index page" index))

(facts "sign-in error classes are not present when there are no errors"
       (let [page (-> (th/create-request)
                      index)]
         (fact "no elements have class for styling errors"
               page => (th/element-absent? [:.form-row--invalid]))
         (fact "email validation element is removed"
               page => (th/element-absent? [:.clj--sign-in-email__validation]))
         (fact "password validation element is removed"
               page => (th/element-absent? [:.clj--sign-in-password__validation]))
         (fact "validation summary element is removed"
               page => (th/element-absent? [:.clj--sign-in-validation-summary]))))

(facts "about displaying sign-in errors"
       (facts "when email is invalid"
              (let [errors {:sign-in-email :invalid}
                    params {:sign-in-email "invalid"}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      page => (th/element-exists? [[:.clj--sign-in-email :.form-row--invalid]]))
                (fact "email validation element is present"
                      page => (th/element-exists? [:.clj--sign-in-email__validation]))
                (fact "correct error message is displayed"
                      page => (th/has-attr? [:.clj--sign-in-email__validation]
                                            :data-l8n "content:index/sign-in-email-address-invalid-validation-message"))
                (fact "invalid value is preserved in input field"
                      page => (th/has-attr? [:.clj--sign-in-email__input] :value "invalid"))))

       (facts "when email is too long"
              (let [long-email-address (apply str (repeat 255 "x"))
                    errors {:sign-in-email :too-long}
                    params {:sign-in-email long-email-address}
                    page (-> (th/create-request {} errors params) index)]
                
                (fact "the class for styling errors is added"
                      page => (th/element-exists? [[:.clj--sign-in-email :.form-row--invalid]]))
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
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--invalid]]))
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
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--invalid]]))
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
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--invalid]]))
               (fact "password validation element is present"
                     page => (th/element-exists? [:.clj--sign-in-password__validation]))

               (fact "correct error message is displayed"
                     page => (th/has-attr? [:.clj--sign-in-password__validation]
                                           :data-l8n "content:index/sign-in-password-too-long-validation-message")))))

(facts "about removing elements when there are no registration errors"
       (let [page (-> (th/create-request)
                      index)]
         (fact "no elements have class for styling errors"
               (html/select page [:.form-row--invalid]) => empty?)
         (fact "first name validation element is removed"
               (html/select page [:.clj--registration-first-name__validation]) => empty?)
         (fact "last name validation element is removed"
               (html/select page [:.clj--registration-last-name__validation]) => empty?)
         (fact "email validation element is removed"
               (html/select page [:.clj--registration-email__validation]) => empty?)
         (fact "password validation element is removed"
               (html/select page [:.clj--registration-password__validation]) => empty?)))

(facts "about displaying registration errors"
       (facts "when first name is blank"
              (let [errors {:registration-first-name :blank}
                    params {:registration-first-name ""}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-first-name :.form-row--invalid]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-first-name__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-first-name__validation (html/attr= :data-l8n "content:index/register-first-name-invalid-validation-message")]]) =not=> empty?)))

       (facts "when first name is too long"
              (let [errors {:registration-first-name :too-long}
                    long-string "SOMETHING LONGER THAN 70 CHARACTERS --------------------------------------------------"
                    params {:registration-first-name long-string}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-first-name :.form-row--invalid]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-first-name__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-first-name__validation (html/attr= :data-l8n "content:index/register-first-name-invalid-validation-message")]]) =not=> empty?)
                (fact "invalid value is preserved in input field"
                      (-> page (html/select [:.clj--registration-first-name__input]) first :attrs :value) => long-string)))

       (facts "when last name is blank"
              (let [errors {:registration-last-name :blank}
                    params {:registration-last-name ""}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-last-name :.form-row--invalid]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-last-name__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-last-name__validation (html/attr= :data-l8n "content:index/register-last-name-invalid-validation-message")]]) =not=> empty?)))

       (facts "when last name is too long"
              (let [errors {:registration-last-name :too-long}
                    long-string "SOMETHING LONGER THAN 70 CHARACTERS --------------------------------------------------"
                    params {:registration-last-name long-string}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-last-name :.form-row--invalid]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-last-name__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-last-name__validation (html/attr= :data-l8n "content:index/register-last-name-invalid-validation-message")]]) =not=> empty?)
                (fact "invalid value is preserved in input field"
                      (-> page (html/select [:.clj--registration-last-name__input]) first :attrs :value) => long-string)))

       (facts "when email is invalid"
              (let [errors {:registration-email :invalid}
                    params {:registration-email "invalidEmail"}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-email :.form-row--invalid]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-email__validation (html/attr= :data-l8n "content:index/register-email-address-invalid-validation-message")]]) =not=> empty?)
                (fact "invalid value is preserved in input field"
                      (-> page (html/select [:.clj--registration-email__input]) first :attrs :value) => "invalidEmail")))

       (facts "when email is a duplicate"
              (let [errors {:registration-email :duplicate}
                    params {:registration-email "valid@email.com"}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-email :.form-row--invalid]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-email__validation (html/attr= :data-l8n "content:index/register-email-address-duplicate-validation-message")]]) =not=> empty?)
                (fact "duplicate email is preserved"
                      (-> page (html/select [:.clj--registration-email__input]) first :attrs :value) => "valid@email.com")))

       (facts "when email is too long"
              (let [long-email-address (apply str (repeat 255 "x"))
                    errors {:registration-email :too-long}
                    params {:registration-email long-email-address}
                    page (-> (th/create-request {} errors params) index)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-email :.form-row--invalid]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-email__validation (html/attr= :data-l8n "content:index/register-email-address-too-long-validation-message")]]) =not=> empty?)))

       (fact "when password is blank"
             (let [errors {:registration-password :blank}
                   params {:registration-password ""}
                   page (-> (th/create-request {} errors params) index)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--registration-password :.form-row--invalid]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--registration-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--registration-password__validation (html/attr= :data-l8n "content:index/register-password-blank-validation-message")]]) =not=> empty?)))

       (fact "when password is too short"
             (let [errors {:registration-password :too-short}
                   params {:registration-password "short"}
                   page (-> (th/create-request {} errors params) index)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--registration-password :.form-row--invalid]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--registration-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--registration-password__validation (html/attr= :data-l8n "content:index/register-password-too-short-validation-message")]]) =not=> empty?)))

       (fact "when password is too long"
             (let [long-password (apply str (repeat 255 "x"))
                   errors {:registration-password :too-long}
                   params {:registration-password long-password}
                   page (-> (th/create-request {} errors params) index)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--registration-password :.form-row--invalid]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--registration-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--registration-password__validation (html/attr= :data-l8n "content:index/register-password-too-long-validation-message")]]) =not=> empty?))))

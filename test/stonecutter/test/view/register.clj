(ns stonecutter.test.view.register
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]  
            [stonecutter.view.register :refer [registration-form]]))

(fact "registration-form should return some html"
      (let [page (-> (th/create-request {} nil {})
                     registration-form
                     html/html-snippet)]
        (html/select page [:form]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request {} nil {}) registration-form html/html-snippet)]
        page => th/work-in-progress-removed))

(fact "sign in link should go to correct endpoint"
      (let [page (-> (th/create-request {} nil {}) registration-form html/html-snippet)]
        (-> page (html/select [:.func--sign-in__link]) first :attrs :href) => (r/path :sign-in)))

(fact "form should have correct action"
      (let [page (-> (th/create-request {} nil {}) registration-form html/html-snippet)]
        (-> page (html/select [:form]) first :attrs :action) => "/register"))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator nil {}) registration-form)]
        page => th/no-untranslated-strings))

(facts "about removing elements when there are no errors"
      (let [page (-> (th/create-request {} nil {})
                     registration-form
                     html/html-snippet)]
        (fact "no elements have class for styling errors"
            (html/select page [:.form-row--validation-error]) => empty?)
        (fact "email validation element is removed"
            (html/select page [:.clj--registration-email__validation]) => empty?)
        (fact "password validation element is removed"
            (html/select page [:.clj--registration-password__validation]) => empty?)
        (fact "validation summary element is removed"
            (html/select page [:.clj--validation-summary]) => empty?)))

(facts "about displaying errors"
       (facts "when email is invalid"
              (let [errors {:email :invalid}
                    params {:email "invalid"}
                    page (-> (th/create-request {} errors params) registration-form html/html-snippet)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-email :.form-row--validation-error]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-email__validation (html/attr= :data-l8n "content:registration-form/email-address-invalid-validation-message")]]) =not=> empty?) 
                (fact "invalid value is preserved in input field"
                      (-> page (html/select [:.registration-email-input]) first :attrs :value) => "invalid")))

       (facts "when email is a duplicate"
              (let [errors {:email :duplicate}
                    params {:email "valid@email.com"}
                    page (-> (th/create-request {} errors params) registration-form html/html-snippet)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-email :.form-row--validation-error]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-email__validation (html/attr= :data-l8n "content:registration-form/email-address-duplicate-validation-message")]]) =not=> empty?) 
                (fact "duplicate email is preserved"
                      (-> page (html/select [:.registration-email-input]) first :attrs :value) => "valid@email.com")))

       (facts "when email is too long"
              (let [long-email-address (apply str (repeat 255 "x"))
                    errors {:email :too-long}
                    params {:email long-email-address}
                    page (-> (th/create-request {} errors params) registration-form html/html-snippet)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-email :.form-row--validation-error]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-email__validation (html/attr= :data-l8n "content:registration-form/email-address-too-long-validation-message")]]) =not=> empty?)))

       (fact "when password is blank"
             (let [errors {:password :blank}
                   params {:password ""}
                   page (-> (th/create-request {} errors params) registration-form html/html-snippet)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--registration-password :.form-row--validation-error]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--registration-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--registration-password__validation (html/attr= :data-l8n "content:registration-form/password-blank-validation-message")]]) =not=> empty?)))

       (fact "when password is too short"
             (let [errors {:password :too-short}
                   params {:password "short"}
                   page (-> (th/create-request {} errors params) registration-form html/html-snippet)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--registration-password :.form-row--validation-error]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--registration-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--registration-password__validation (html/attr= :data-l8n "content:registration-form/password-too-short-validation-message")]]) =not=> empty?)))

       (fact "when password is too long"
             (let [long-password (apply str (repeat 255 "x"))
                   errors {:password :too-long}
                   params {:password long-password}
                   page (-> (th/create-request {} errors params) registration-form html/html-snippet)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--registration-password :.form-row--validation-error]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--registration-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--registration-password__validation (html/attr= :data-l8n "content:registration-form/password-too-long-validation-message")]]) =not=> empty?)))

       (fact "when confirm password is invalid"
             (let [errors {:confirm-password :invalid}
                   params {:password "password" :confirm-password "invalid-password"}
                   page (-> (th/create-request {} errors params) registration-form html/html-snippet)]
               (fact "confirm password validation is present as a validation summary item"
                     (html/select page [:.clj--validation-summary__item]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--validation-summary__item (html/attr= :data-l8n "content:registration-form/confirm-password-invalid-validation-message")]]) =not=> empty?))))

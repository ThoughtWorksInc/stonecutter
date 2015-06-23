(ns stonecutter.test.view.register
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.register :refer [registration-form add-anti-forgery]]))

(defn create-context [err params]
  {:translator {}
   :errors err
   :params params})

(def no-untranslated-strings
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(fact "registration-form should return some html"
      (let [page (-> (create-context nil {})
                     registration-form
                     html/html-snippet)]
        (html/select page [:form]) =not=> empty?))

(fact "form should have correct action"
      (let [page (-> (create-context nil {}) registration-form html/html-snippet)]
        (-> page (html/select [:form]) first :attrs :action) => "/register"))

(fact "there are no missing translations"
      (let [page (-> (create-context nil {}) registration-form)]
        page => no-untranslated-strings))

(fact "can inject anti-forgery token"
      (let [page (-> "<html><form></form></html>"
                     html/html-snippet)]
        (-> page
            add-anti-forgery
            (html/select [:form (html/attr= :name "__anti-forgery-token")])) =not=> empty?))

(facts "about removing elements when there are no errors"
      (let [page (-> (create-context nil {})
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
                    page (-> (create-context errors params) registration-form html/html-snippet)]
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
                    page (-> (create-context errors params) registration-form html/html-snippet)]
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
                    page (-> (create-context errors params) registration-form html/html-snippet)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-email :.form-row--validation-error]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-email__validation (html/attr= :data-l8n "content:registration-form/email-address-too-long-validation-message")]]) =not=> empty?)))

       (fact "when password is invalid"
             (let [errors {:password :invalid}
                   params {:password ""}
                   page (-> (create-context errors params) registration-form html/html-snippet)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--registration-password :.form-row--validation-error]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--registration-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--registration-password__validation (html/attr= :data-l8n "content:registration-form/password-invalid-validation-message")]]) =not=> empty?)))

       (fact "when confirm password is invalid"
             (let [errors {:confirm-password :invalid}
                   params {:password "password" :confirm-password "invalid-password"}
                   page (-> (create-context errors params) registration-form html/html-snippet)]
               (fact "confirm password validation is present as a validation summary item"
                     (html/select page [:.clj--validation-summary__item]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--validation-summary__item (html/attr= :data-l8n "content:registration-form/confirm-password-invalid-validation-message")]]) =not=> empty?))))

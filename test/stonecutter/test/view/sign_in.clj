(ns stonecutter.test.view.sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.handler :refer [translations-fn translation-map]]
            [stonecutter.view.sign-in :refer [sign-in-form]]
            [stonecutter.test.view.test-helpers :refer [create-request]]))

(def no-untranslated-strings 
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(fact "sign-in-form should return some html"
      (let [page (-> (create-request {} nil {}) sign-in-form html/html-snippet)]
        (html/select page [:form]) =not=> empty?))

(fact "form should have correct action"
      (let [page (-> (create-request {} nil {}) sign-in-form html/html-snippet)]
        (-> page (html/select [:form]) first :attrs :action) => "/login"))

(fact "there are no missing translations"
      (stonecutter.logging/init-logger!)
      (let [translator (translations-fn translation-map)
            page (-> (create-request translator nil {}) sign-in-form)]
        page => no-untranslated-strings))

(facts "about removing elements when there are no errors"
       (let [page (-> (create-request {} nil {})
                      sign-in-form
                      html/html-snippet)]
         (fact "no elements have class for styling errors"
               (html/select page [:.form-row--validation-error]) => empty?)
         (fact "email validation element is removed"
               (html/select page [:.clj--sign-in-email__validation]) => empty?)
         (fact "password validation element is removed"
               (html/select page [:.clj--sign-in-password__validation]) => empty?)
         (fact "validation summary element is removed"
               (html/select page [:.clj--validation-summary]) => empty?)))

(facts "about displaying errors"
       (facts "when email is invalid"
              (let [errors {:email :invalid}
                    params {:email "invalid"}
                    page (-> (create-request {} errors params) sign-in-form html/html-snippet)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--sign-in-email :.form-row--validation-error]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--sign-in-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--sign-in-email__validation (html/attr= :data-l8n "content:sign-in-form/email-address-invalid-validation-message")]]) =not=> empty?) 
                (fact "invalid value is preserved in input field"
                      (-> page (html/select [:.clj--email__input]) first :attrs :value) => "invalid")))

       (facts "when email is too long"
              (let [long-email-address (apply str (repeat 255 "x"))
                    errors {:email :too-long}
                    params {:email long-email-address}
                    page (-> (create-request {} errors params) sign-in-form html/html-snippet)]
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--sign-in-email :.form-row--validation-error]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--sign-in-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--sign-in-email__validation (html/attr= :data-l8n "content:sign-in-form/email-address-too-long-validation-message")]]) =not=> empty?)))

       (fact "when password is blank"
             (let [errors {:password :blank}
                   params {:password ""}
                   page (-> (create-request {} errors params) sign-in-form html/html-snippet)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--sign-in-password :.form-row--validation-error]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--sign-in-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--sign-in-password__validation (html/attr= :data-l8n "content:sign-in-form/password-blank-validation-message")]]) =not=> empty?)))

       (fact "when password is too short"
             (let [errors {:password :too-short}
                   params {:password "short"}
                   page (-> (create-request {} errors params) sign-in-form html/html-snippet)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--sign-in-password :.form-row--validation-error]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--sign-in-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--sign-in-password__validation (html/attr= :data-l8n "content:sign-in-form/password-too-short-validation-message")]]) =not=> empty?)))

       (fact "when password is too long"
             (let [long-password (apply str (repeat 255 "x"))
                   errors {:password :too-long}
                   params {:password long-password}
                   page (-> (create-request {} errors params) sign-in-form html/html-snippet)]
               (fact "the class for styling errors is added"
                     (html/select page [[:.clj--sign-in-password :.form-row--validation-error]]) =not=> empty?)
               (fact "password validation element is present"
                     (html/select page [:.clj--sign-in-password__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--sign-in-password__validation (html/attr= :data-l8n "content:sign-in-form/password-too-long-validation-message")]]) =not=> empty?))))

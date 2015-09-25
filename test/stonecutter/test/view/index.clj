(ns stonecutter.test.view.index
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.view.index :refer [index]]))

(defn string-of-length [n]
  (apply str (repeat n "x")))

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
                                     :href (r/path :show-forgotten-password-form)))
         
         (fact "page has script link to javascript file"
               (html/select page [[:script (html/attr= :src "js/main.js")]]) =not=> empty?)))

(fact (th/test-translations "index page" index))

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
                      page => (th/has-attr? [:.clj--sign-in-email__input] :value "invalid"))
                (fact "there are no missing translations"
                      (th/test-translations "index page" (constantly page)))))

       (facts "when email is too long"
              (let [long-email-address (string-of-length 255)
                    errors {:sign-in-email :too-long}
                    params {:sign-in-email long-email-address}
                    page (-> (th/create-request {} errors params) index)]
                
                (fact "the class for styling errors is added"
                      page => (th/element-exists? [[:.clj--sign-in-email :.form-row--invalid]]))
                (fact "email validation element is present"
                      page => (th/element-exists? [:.clj--sign-in-email__validation]))
                (fact "correct error message is displayed"
                      page => (th/has-attr? [:.clj--sign-in-email__validation]
                                            :data-l8n "content:index/sign-in-email-address-too-long-validation-message"))
                (fact "there are no missing translations"
                      (th/test-translations "index page" (constantly page)))))

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
                                           :data-l8n "content:index/sign-in-password-blank-validation-message"))
               (fact "there are no missing translations"
                      (th/test-translations "index page" (constantly page)))))

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
                                           :data-l8n "content:index/sign-in-password-too-short-validation-message"))
               (fact "there are no missing translations"
                      (th/test-translations "index page" (constantly page)))))

       (fact "when password is too long"
             (let [long-password (string-of-length 255)
                   errors {:sign-in-password :too-long}
                   params {:sign-in-password long-password}
                   page (-> (th/create-request {} errors params) index)]
               (fact "the class for styling errors is added"
                     page => (th/element-exists? [[:.clj--sign-in-password :.form-row--invalid]]))
               (fact "password validation element is present"
                     page => (th/element-exists? [:.clj--sign-in-password__validation]))
               (fact "correct error message is displayed"
                     page => (th/has-attr? [:.clj--sign-in-password__validation]
                                           :data-l8n "content:index/sign-in-password-too-long-validation-message"))
               (fact "there are no missing translations"
                      (th/test-translations "index page" (constantly page))))))

(facts "about removing elements when there are no registration errors"
       (let [page (-> (th/create-request)
                      index)]
         (fact "validation summary is removed"
               (html/select page [:.clj--registration-validation-summary]) => empty?)
         (fact "no elements have class for styling and unhiding errors"
               (html/select page [:.form-row--invalid]) => empty?)
         (fact "first name validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
               (html/select page [:.clj--registration-first-name__validation]) =not=> empty?)
         (fact "last name validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
               (html/select page [:.clj--registration-last-name__validation]) =not=> empty?)
         (fact "email validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
               (html/select page [:.clj--registration-email__validation]) =not=> empty?)
         (fact "password validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
               (html/select page [:.clj--registration-password__validation]) =not=> empty?)))

(fact "Bugfix: registration validation summary is removed when errors are not registration errors"
      (let [errors {:not-a-registration-error :some-error}
            page (-> (th/create-request {} errors) index)]
        (html/select page [:.clj--registration-validation-summary]) => empty?))

(facts "about displaying registration errors"
       (tabular
         (facts "first name validations"
                (let [errors {:registration-first-name ?error}
                      params {:registration-first-name ?input}
                      page (-> (th/create-request {} errors params) index)
                      error-translation (str "content:index/" ?translation-key)]
                  (fact "validation summary includes the error message"
                        (html/select page [[:.clj--registration-validation-summary__item (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--registration-first-name :.form-row--invalid]]) =not=> empty?)
                  (fact "email validation element is present"
                        (html/select page [:.clj--registration-first-name__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--registration-first-name__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "invalid value is preserved in input field"
                        (-> page (html/select [:.clj--registration-first-name__input]) first :attrs :value) => ?input)
                  (fact "there are no missing translations"
                        (th/test-translations "index page" (constantly page)))))
         ?error    ?input                ?translation-key
         :blank    ""                    "register-first-name-blank-validation-message"
         :too-long (string-of-length 71) "register-first-name-too-long-validation-message")

       (tabular
         (facts "last name validations"
                (let [errors {:registration-last-name ?error}
                      params {:registration-last-name ?input}
                      page (-> (th/create-request {} errors params) index)
                      error-translation (str "content:index/" ?translation-key)]
                  (fact "validation summary includes the error message"
                        (html/select page [[:.clj--registration-validation-summary__item (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--registration-last-name :.form-row--invalid]]) =not=> empty?)
                  (fact "email validation element is present"
                        (html/select page [:.clj--registration-last-name__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--registration-last-name__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "invalid value is preserved in input field"
                        (-> page (html/select [:.clj--registration-last-name__input]) first :attrs :value) => ?input)
                  (fact "there are no missing translations"
                        (th/test-translations "index page" (constantly page)))))
         ?error    ?input                ?translation-key
         :blank    ""                    "register-last-name-blank-validation-message"
         :too-long (string-of-length 71) "register-last-name-too-long-validation-message")

       (tabular
         (facts "email validations"
              (let [errors {:registration-email ?error}
                    params {:registration-email ?input}
                    page (-> (th/create-request {} errors params) index)
                    error-translation (str "content:index/" ?translation-key)]
                (fact "validation summary includes the error message"
                      (html/select page [[:.clj--registration-validation-summary__item (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                (fact "the class for styling errors is added"
                      (html/select page [[:.clj--registration-email :.form-row--invalid]]) =not=> empty?)
                (fact "email validation element is present"
                      (html/select page [:.clj--registration-email__validation]) =not=> empty?)
                (fact "correct error message is displayed"
                      (html/select page [[:.clj--registration-email__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                (fact "invalid value is preserved in input field"
                      (-> page (html/select [:.clj--registration-email__input]) first :attrs :value) => ?input)
                (fact "there are no missing translations"
                      (th/test-translations "index page" (constantly page)))))

         ?error      ?input                 ?translation-key
         :invalid    "invalidEmail"         "register-email-address-invalid-validation-message"
         :duplicate  "valid@email.com"      "register-email-address-duplicate-validation-message"
         :too-long   (string-of-length 255) "register-email-address-too-long-validation-message")

       (tabular
         (facts "password validations"
                (let [errors {:registration-password ?error}
                      params {:registration-password ?input}
                      page (-> (th/create-request {} errors params) index)
                      error-translation (str "content:index/" ?translation-key)]
                  (fact "validation summary includes the error message"
                        (html/select page [[:.clj--registration-validation-summary__item (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--registration-password :.form-row--invalid]]) =not=> empty?)
                  (fact "password validation element is present"
                        (html/select page [:.clj--registration-password__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--registration-password__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "the input is not preserved"
                        (-> page (html/select [:.clj--registration-password__input]) first :attrs :value) => nil)
                  (fact "there are no missing translations"
                        (th/test-translations "index page" (constantly page)))))
         ?error     ?input                 ?translation-key
         :blank     ""                     "register-password-blank-validation-message"
         :too-short "2short"               "register-password-too-short-validation-message"
         :too-long  (string-of-length 255) "register-password-too-long-validation-message"))

(ns stonecutter.test.integration.register-form
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [clojure.string :as string]
            [stonecutter.js.controller.register-form :as rfc]
            [stonecutter.js.app :as app]
            [stonecutter.js.dom.register-form :as rfd]
            [stonecutter.test.unit.register-form :as rfut]
            [stonecutter.test.test-utils :as tu])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [dommy.core :refer [sel1]]
                   [stonecutter.test.macros :refer [load-template]]))


(defonce index-page-template (load-template "public/index.html"))

(defn setup-index-page! []
  (dommy/set-html! (sel1 :html) index-page-template))

(def blank-string "")
(def valid-name "Frank")
(def too-long-name (rfut/string-of-length 71))
(def valid-email "frank@franky.fr")
(def invalid-email "invalid-email")
(def too-long-email rfut/email-of-length-255)
(def valid-password "avalidpassword")
(def too-short-password "short")
(def too-long-password (rfut/string-of-length 255))

(defn element-has-text [selector expected-text]
  (let [selected-element (sel1 selector)
        text (dommy/text selected-element)]
    (is (not (string/blank? text)) "Element has no text")
    (is (= expected-text text)
        (str "Expected element to have <" expected-text "> but actually found <" text ">"))))

(defn element-has-no-text [selector]
  (let [selected-element (sel1 selector)
        text (dommy/text selected-element)]
    (is (string/blank? text) "Element is not blank")))

(defn invalid-password-state! []
  (tu/enter-text (rfd/input-selector :registration-password) blank-string)
  (tu/lose-focus (rfd/input-selector :registration-password)))

(deftest on-input
         (setup-index-page!)
         (app/start)
         (testing "inputing text in first name field will cause field invalid class to disappear"
                  (rfd/add-class! (rfd/form-row-selector :registration-first-name) rfd/field-invalid-class)
                  (tu/enter-text (rfd/input-selector :registration-first-name) valid-name)
                  (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-first-name) rfd/field-invalid-class))
         (testing "inputing text in last name field will cause field invalid class to disappear"
                  (rfd/add-class! (rfd/form-row-selector :registration-last-name) rfd/field-invalid-class)
                  (tu/enter-text (rfd/input-selector :registration-last-name) valid-name)
                  (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-last-name) rfd/field-invalid-class))
         (testing "inputing text in email address field will cause field invalid class to disappear"
                  (rfd/add-class! (rfd/form-row-selector :registration-email) rfd/field-invalid-class)
                  (tu/enter-text (rfd/input-selector :registration-email) valid-email)
                  (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-email) rfd/field-invalid-class))

         (testing "password"
                  (testing "- inputing valid password causes field valid class to appear"
                           (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-password) rfd/field-valid-class)
                           (tu/enter-text (rfd/input-selector :registration-password) valid-password)
                           (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-valid-class))
                  (testing "- from valid password to invalid password causes field valid class to disappear"
                           (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-valid-class)
                           (tu/enter-text (rfd/input-selector :registration-password) too-short-password)
                           (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-password) rfd/field-valid-class))
                  (testing "- from invalid password to valid password causes field valid class to appear and field invalid class to disappear"
                           (rfd/add-class! (rfd/form-row-selector :registration-password) rfd/field-invalid-class)
                           (rfd/remove-class! (rfd/form-row-selector :registration-password) rfd/field-valid-class)
                           (tu/enter-text (rfd/input-selector :registration-password) valid-password)
                           (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class)
                           (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-valid-class))
                  (testing "- from invalid password to invalid password causes field invalid class to remain"
                           (invalid-password-state!)
                           (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class)
                           (tu/enter-text (rfd/input-selector :registration-password) too-short-password)
                           (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class))))


(defn check-first-name-has-blank-validation-errors []
  (tu/test-field-has-class (rfd/form-row-selector :registration-first-name) rfd/field-invalid-class)
  (element-has-text (rfd/validation-selector :registration-first-name)
                         (get-in rfd/translations [:index :register-first-name-blank-validation-message])))

(defn check-last-name-has-blank-validation-errors []
  (tu/test-field-has-class (rfd/form-row-selector :registration-last-name) rfd/field-invalid-class)
  (element-has-text (rfd/validation-selector :registration-last-name)
                         (get-in rfd/translations [:index :register-last-name-blank-validation-message])))

(defn check-email-address-has-invalid-validation-errors []
  (tu/test-field-has-class (rfd/form-row-selector :registration-email) rfd/field-invalid-class)
  (element-has-text (rfd/validation-selector :registration-email)
                         (get-in rfd/translations [:index :register-email-address-invalid-validation-message])))

(defn check-password-has-blank-validation-errors []
  (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class)
  (element-has-text (rfd/validation-selector :registration-password)
                         (get-in rfd/translations [:index :register-password-blank-validation-message])))


(deftest losing-focus
         (setup-index-page!)
         (app/start)

         (testing "first-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (tu/lose-focus (rfd/input-selector :registration-first-name))
                           (check-first-name-has-blank-validation-errors)))

         (testing "- losing focus when too-long adds invalid field class and validation message"
                  (tu/set-value (rfd/input-selector :registration-first-name) too-long-name)
                  (tu/lose-focus (rfd/input-selector :registration-first-name))
                  (tu/test-field-has-class (rfd/form-row-selector :registration-first-name) rfd/field-invalid-class)
                  (element-has-text (rfd/validation-selector :registration-first-name)
                                         (get-in rfd/translations [:index :register-first-name-too-long-validation-message])))

         (testing "- losing focus when valid removes invalid field class and validation message"
                  (tu/test-field-has-class (rfd/form-row-selector :registration-first-name) rfd/field-invalid-class)
                  (element-has-text (rfd/validation-selector :registration-first-name)
                                         (get-in rfd/translations [:index :register-first-name-too-long-validation-message]))

                  (tu/set-value (rfd/input-selector :registration-first-name) valid-name)
                  (tu/lose-focus (rfd/input-selector :registration-first-name))

                  (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-first-name) rfd/field-invalid-class)
                  (element-has-no-text (rfd/validation-selector :registration-first-name)))

         (testing "last-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (tu/lose-focus (rfd/input-selector :registration-last-name))
                           (check-last-name-has-blank-validation-errors))

                  (testing "- losing focus when too-long adds invalid field class and validation message"
                           (tu/set-value (rfd/input-selector :registration-last-name) too-long-name)
                           (tu/lose-focus (rfd/input-selector :registration-last-name))
                           (tu/test-field-has-class (rfd/form-row-selector :registration-last-name) rfd/field-invalid-class)
                           (element-has-text (rfd/validation-selector :registration-last-name)
                                                  (get-in rfd/translations [:index :register-last-name-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (tu/test-field-has-class (rfd/form-row-selector :registration-last-name) rfd/field-invalid-class)
                           (element-has-text (rfd/validation-selector :registration-last-name)
                                                  (get-in rfd/translations [:index :register-last-name-too-long-validation-message]))

                           (tu/set-value (rfd/input-selector :registration-last-name) valid-name)
                           (tu/lose-focus (rfd/input-selector :registration-last-name))

                           (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-last-name) rfd/field-invalid-class)
                           (element-has-no-text (rfd/validation-selector :registration-last-name))))

         (testing "email-address"
                  (testing "- losing focus when invalid adds invalid field class and validation message"
                           (tu/set-value (rfd/input-selector :registration-email) invalid-email)
                           (tu/lose-focus (rfd/input-selector :registration-email))
                           (check-email-address-has-invalid-validation-errors))

                  (testing "- losing focus when too-long adds invalid field class and validation message"
                           (tu/set-value (rfd/input-selector :registration-email) too-long-email)
                           (tu/lose-focus (rfd/input-selector :registration-email))
                           (tu/test-field-has-class (rfd/form-row-selector :registration-email) rfd/field-invalid-class)
                           (element-has-text (rfd/validation-selector :registration-email)
                                                  (get-in rfd/translations [:index :register-email-address-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (tu/test-field-has-class (rfd/form-row-selector :registration-email) rfd/field-invalid-class)
                           (element-has-text (rfd/validation-selector :registration-email)
                                                  (get-in rfd/translations [:index :register-email-address-too-long-validation-message]))

                           (tu/set-value (rfd/input-selector :registration-email) valid-email)
                           (tu/lose-focus (rfd/input-selector :registration-email))

                           (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-email) rfd/field-invalid-class)
                           (element-has-no-text (rfd/validation-selector :registration-email))))



         (testing "password"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (tu/lose-focus (rfd/input-selector :registration-password))
                           (check-password-has-blank-validation-errors))

                  (testing "- losing focus when too short adds invalid field class and validation message"
                           (tu/set-value (rfd/input-selector :registration-password) too-short-password)
                           (tu/lose-focus (rfd/input-selector :registration-password))
                           (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class)
                           (element-has-text (rfd/validation-selector :registration-password)
                                                  (get-in rfd/translations [:index :register-password-too-short-validation-message])))

                  (testing "- losing focus when too long adds invalid field class and validation message"
                           (tu/set-value (rfd/input-selector :registration-password) too-long-password)
                           (tu/lose-focus (rfd/input-selector :registration-password))
                           (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class)
                           (element-has-text (rfd/validation-selector :registration-password)
                                                  (get-in rfd/translations [:index :register-password-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (tu/test-field-has-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class)
                           (element-has-text (rfd/validation-selector :registration-password)
                                                  (get-in rfd/translations [:index :register-password-too-long-validation-message])) ; gg|rs 18Sept - what about (element-has-any-text ...) ?

                           (tu/set-value (rfd/input-selector :registration-password) valid-password)
                           (tu/lose-focus (rfd/input-selector :registration-password))

                           (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class)
                           (element-has-no-text (rfd/validation-selector :registration-password)))))

(deftest submitting-invalid-forms
         (setup-index-page!)
         (app/start)

         (testing "submitting empty form"
                  (tu/press-submit rfd/register-form-element-selector)
                  (check-first-name-has-blank-validation-errors)
                  (check-last-name-has-blank-validation-errors)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (tu/has-focus? (rfd/input-selector :registration-first-name)))

         (testing "submitting form with only valid first-name"
                  (tu/enter-text (rfd/input-selector :registration-first-name) valid-name)
                  (tu/press-submit rfd/register-form-element-selector)
                  (check-last-name-has-blank-validation-errors)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (tu/has-focus? (rfd/input-selector :registration-last-name)))

         (testing "submitting form with only valid first-name and last-name"
                  (tu/enter-text (rfd/input-selector :registration-last-name) valid-name)
                  (tu/press-submit rfd/register-form-element-selector)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (tu/has-focus? (rfd/input-selector :registration-email)))

         (testing "submitting form with only valid first-name, last-name and email-address"
                  (tu/enter-text (rfd/input-selector :registration-email) valid-email)
                  (tu/press-submit rfd/register-form-element-selector)
                  (check-password-has-blank-validation-errors)
                  (tu/has-focus? (rfd/input-selector :registration-password))))

(defn default-state [] (atom rfc/default-state))

(deftest prevent-default-submit
         (setup-index-page!)
         (app/start)

         (testing "prevents default when page has errors"
                  (let [submit-event (tu/create-event :submit)]
                    (rfc/block-invalid-submit (default-state) submit-event)
                    (tu/default-prevented? submit-event true)))

         (testing "doesn't prevent default when inputs are valid"
                  (let [submit-event (tu/create-event :submit)]
                    (tu/enter-text (rfd/input-selector :registration-first-name) valid-name)
                    (tu/enter-text (rfd/input-selector :registration-last-name) valid-name)
                    (tu/enter-text (rfd/input-selector :registration-email) valid-email)
                    (tu/enter-text (rfd/input-selector :registration-password) valid-password)
                    (rfc/block-invalid-submit (default-state) submit-event)
                    (testing "all error classes are removed"
                             (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-first-name) rfd/field-invalid-class)
                             (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-last-name) rfd/field-invalid-class)
                             (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-email) rfd/field-invalid-class)
                             (tu/test-field-doesnt-have-class (rfd/form-row-selector :registration-password) rfd/field-invalid-class))
                    (tu/default-prevented? submit-event false))))
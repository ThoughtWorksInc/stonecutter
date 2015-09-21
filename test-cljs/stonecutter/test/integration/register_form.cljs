(ns stonecutter.test.integration.register-form
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [stonecutter.register-form :as rf]
            [stonecutter.utils :as utils]
            [stonecutter.renderer.register-form :as rfr]
            [stonecutter.test.renderer.register-form :as rfrt]
            [stonecutter.test.unit.register-form :as rfut]
            [stonecutter.test.test-utils :as tu])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]
                   [stonecutter.test.macros :refer [load-template]]))


(defn setup-index-page! []
  (dommy/set-html! (sel1 :html)
                   (load-template "public/index.html")))

(def blank-string "")
(def valid-name "Frank")
(def too-long-name (rfut/string-of-length 71))
(def valid-email "frank@franky.fr")
(def invalid-email "invalid-email")
(def too-long-email rfut/email-of-length-255)
(def valid-password "avalidpassword")
(def too-short-password "short")
(def too-long-password (rfut/string-of-length 255))


(defn invalid-password-state! []
  (tu/enter-text rfr/password-input-element-selector blank-string)
  (tu/lose-focus rfr/password-input-element-selector))


(deftest on-input
         (setup-index-page!)
         (utils/start)
         (testing "inputing text in first name field will cause field invalid class to disappear"
                  (rfr/add-or-remove-class! rfr/first-name-form-row-element-selector rfr/field-invalid-class true)
                  (tu/enter-text rfr/first-name-input-element-selector valid-name)
                  (tu/test-field-doesnt-have-class rfr/first-name-form-row-element-selector rfr/field-invalid-class))
         (testing "inputing text in last name field will cause field invalid class to disappear"
                  (rfr/add-or-remove-class! rfr/last-name-form-row-element-selector rfr/field-invalid-class true)
                  (tu/enter-text rfr/last-name-input-element-selector valid-name)
                  (tu/test-field-doesnt-have-class rfr/last-name-form-row-element-selector rfr/field-invalid-class))
         (testing "inputing text in email address field will cause field invalid class to disappear"
                  (rfr/add-or-remove-class! rfr/email-address-form-row-element-selector rfr/field-invalid-class true)
                  (tu/enter-text rfr/email-address-input-element-selector valid-email)
                  (tu/test-field-doesnt-have-class rfr/email-address-form-row-element-selector rfr/field-invalid-class))

         (testing "password"
                  (testing "- inputing valid password causes field valid class to appear"
                           (tu/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-valid-class)
                           (tu/enter-text rfr/password-input-element-selector valid-password)
                           (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-valid-class))
                  (testing "- from valid password to invalid password causes field valid class to disappear"
                           (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-valid-class)
                           (tu/enter-text rfr/password-input-element-selector too-short-password)
                           (tu/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-valid-class))
                  (testing "- from invalid password to valid password causes field valid class to appear and field invalid class to disappear"
                           (rfr/add-or-remove-class! rfr/password-form-row-element-selector rfr/field-invalid-class true)
                           (rfr/add-or-remove-class! rfr/password-form-row-element-selector rfr/field-valid-class false)
                           (tu/enter-text rfr/password-input-element-selector valid-password)
                           (tu/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-valid-class))
                  (testing "- from invalid password to invalid password causes field invalid class to remain"
                           (invalid-password-state!)
                           (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (tu/enter-text rfr/password-input-element-selector too-short-password)
                           (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class))))


(defn check-first-name-has-blank-validation-errors []
  (tu/test-field-has-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
  (rfrt/element-has-text rfr/first-name-validation-element-selector
                         (get-in rfr/translations [:index :register-first-name-blank-validation-message])))

(defn check-last-name-has-blank-validation-errors []
  (tu/test-field-has-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
  (rfrt/element-has-text rfr/last-name-validation-element-selector
                         (get-in rfr/translations [:index :register-last-name-blank-validation-message])))

(defn check-email-address-has-invalid-validation-errors []
  (tu/test-field-has-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
  (rfrt/element-has-text rfr/email-address-validation-element-selector
                         (get-in rfr/translations [:index :register-email-address-invalid-validation-message])))

(defn check-password-has-blank-validation-errors []
  (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
  (rfrt/element-has-text rfr/password-validation-element-selector
                         (get-in rfr/translations [:index :register-password-blank-validation-message])))


(deftest losing-focus
         (setup-index-page!)
         (utils/start)

         (testing "first-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (tu/lose-focus rfr/first-name-input-element-selector)
                           (check-first-name-has-blank-validation-errors)))

         (testing "- losing focus when too-long adds invalid field class and validation message"
                  (tu/set-value rfr/first-name-input-element-selector too-long-name)
                  (tu/lose-focus rfr/first-name-input-element-selector)
                  (tu/test-field-has-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                  (rfrt/element-has-text rfr/first-name-validation-element-selector
                                         (get-in rfr/translations [:index :register-first-name-too-long-validation-message])))

         (testing "- losing focus when valid removes invalid field class and validation message"
                  (tu/test-field-has-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                  (rfrt/element-has-text rfr/first-name-validation-element-selector
                                         (get-in rfr/translations [:index :register-first-name-too-long-validation-message]))

                  (tu/set-value rfr/first-name-input-element-selector valid-name)
                  (tu/lose-focus rfr/first-name-input-element-selector)

                  (tu/test-field-doesnt-have-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                  (rfrt/element-has-no-text rfr/first-name-validation-element-selector))

         (testing "last-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (tu/lose-focus rfr/last-name-input-element-selector)
                           (check-last-name-has-blank-validation-errors))

                  (testing "- losing focus when too-long adds invalid field class and validation message"
                           (tu/set-value rfr/last-name-input-element-selector too-long-name)
                           (tu/lose-focus rfr/last-name-input-element-selector)
                           (tu/test-field-has-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/last-name-validation-element-selector
                                                  (get-in rfr/translations [:index :register-last-name-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (tu/test-field-has-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/last-name-validation-element-selector
                                                  (get-in rfr/translations [:index :register-last-name-too-long-validation-message]))

                           (tu/set-value rfr/last-name-input-element-selector valid-name)
                           (tu/lose-focus rfr/last-name-input-element-selector)

                           (tu/test-field-doesnt-have-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-no-text rfr/last-name-validation-element-selector)))

         (testing "email-address"
                  (testing "- losing focus when invalid adds invalid field class and validation message"
                           (tu/set-value rfr/email-address-input-element-selector invalid-email)
                           (tu/lose-focus rfr/email-address-input-element-selector)
                           (check-email-address-has-invalid-validation-errors))

                  (testing "- losing focus when too-long adds invalid field class and validation message"
                           (tu/set-value rfr/email-address-input-element-selector too-long-email)
                           (tu/lose-focus rfr/email-address-input-element-selector)
                           (tu/test-field-has-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/email-address-validation-element-selector
                                                  (get-in rfr/translations [:index :register-email-address-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (tu/test-field-has-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/email-address-validation-element-selector
                                                  (get-in rfr/translations [:index :register-email-address-too-long-validation-message]))

                           (tu/set-value rfr/email-address-input-element-selector valid-email)
                           (tu/lose-focus rfr/email-address-input-element-selector)

                           (tu/test-field-doesnt-have-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-no-text rfr/email-address-validation-element-selector)))



         (testing "password"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (tu/lose-focus rfr/password-input-element-selector)
                           (check-password-has-blank-validation-errors))

                  (testing "- losing focus when too short adds invalid field class and validation message"
                           (tu/set-value rfr/password-input-element-selector too-short-password)
                           (tu/lose-focus rfr/password-input-element-selector)
                           (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/password-validation-element-selector
                                                  (get-in rfr/translations [:index :register-password-too-short-validation-message])))

                  (testing "- losing focus when too long adds invalid field class and validation message"
                           (tu/set-value rfr/password-input-element-selector too-long-password)
                           (tu/lose-focus rfr/password-input-element-selector)
                           (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/password-validation-element-selector
                                                  (get-in rfr/translations [:index :register-password-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (tu/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/password-validation-element-selector
                                                  (get-in rfr/translations [:index :register-password-too-long-validation-message])) ; gg|rs 18Sept - what about (element-has-any-text ...) ?

                           (tu/set-value rfr/password-input-element-selector valid-password)
                           (tu/lose-focus rfr/password-input-element-selector)

                           (tu/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-no-text rfr/password-validation-element-selector))))

(deftest submitting-invalid-forms
         (setup-index-page!)
         (utils/start)

         (testing "submitting empty form"
                  (tu/press-submit rfr/register-form-element-selector)
                  (check-first-name-has-blank-validation-errors)
                  (check-last-name-has-blank-validation-errors)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (tu/has-focus? rfr/first-name-input-element-selector))

         (testing "submitting form with only valid first-name"
                  (tu/enter-text rfr/first-name-input-element-selector valid-name)
                  (tu/press-submit rfr/register-form-element-selector)
                  (check-last-name-has-blank-validation-errors)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (tu/has-focus? rfr/last-name-input-element-selector))

         (testing "submitting form with only valid first-name and last-name"
                  (tu/enter-text rfr/last-name-input-element-selector valid-name)
                  (tu/press-submit rfr/register-form-element-selector)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (tu/has-focus? rfr/email-address-input-element-selector))

         (testing "submitting form with only valid first-name, last-name and email-address"
                  (tu/enter-text rfr/email-address-input-element-selector valid-email)
                  (tu/press-submit rfr/register-form-element-selector)
                  (check-password-has-blank-validation-errors)
                  (tu/has-focus? rfr/password-input-element-selector)))

(deftest prevent-default-submit
         (setup-index-page!)
         (utils/start)

         (testing "prevents default when page has errors"
                  (let [submit-event (tu/create-event :submit)]
                    (rf/block-invalid-submit submit-event)
                    (tu/default-prevented? submit-event true)))

         (testing "doesn't prevent default when inputs are valid"
                  (let [submit-event (tu/create-event :submit)]
                    (tu/enter-text rfr/first-name-input-element-selector valid-name)
                    (tu/enter-text rfr/last-name-input-element-selector valid-name)
                    (tu/enter-text rfr/email-address-input-element-selector valid-email)
                    (tu/enter-text rfr/password-input-element-selector valid-password)
                    (rf/block-invalid-submit submit-event)
                    (testing "all error classes are removed"
                             (tu/test-field-doesnt-have-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                             (tu/test-field-doesnt-have-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
                             (tu/test-field-doesnt-have-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
                             (tu/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-invalid-class))
                    (tu/default-prevented? submit-event false))))
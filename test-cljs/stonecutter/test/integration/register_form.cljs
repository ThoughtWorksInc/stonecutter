(ns stonecutter.test.integration.register-form
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [dommy.utils :as du]
            [clojure.string :as string]
            [stonecutter.register-form :as rf]
            [stonecutter.utils :as utils]
            [stonecutter.renderer.register-form :as rfr]
            [stonecutter.test.renderer.register-form :as rfrt]
            [stonecutter.test.unit.register-form :as rfut]
            [stonecutter.test.test-utils :as test-utils]
            [stonecutter.test.integration.change-password :as cp])
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
  (cp/enter-text rfr/password-input-element-selector blank-string)
  (cp/lose-focus rfr/password-input-element-selector))


(deftest on-input
         (setup-index-page!)
         (utils/start)
         (testing "inputing text in first name field will cause field invalid class to disappear"
                  (rfr/add-or-remove-class! rfr/first-name-form-row-element-selector rfr/field-invalid-class true)
                  (cp/enter-text rfr/first-name-input-element-selector valid-name)
                  (cp/test-field-doesnt-have-class rfr/first-name-form-row-element-selector rfr/field-invalid-class))
         (testing "inputing text in last name field will cause field invalid class to disappear"
                  (rfr/add-or-remove-class! rfr/last-name-form-row-element-selector rfr/field-invalid-class true)
                  (cp/enter-text rfr/last-name-input-element-selector valid-name)
                  (cp/test-field-doesnt-have-class rfr/last-name-form-row-element-selector rfr/field-invalid-class))
         (testing "inputing text in email address field will cause field invalid class to disappear"
                  (rfr/add-or-remove-class! rfr/email-address-form-row-element-selector rfr/field-invalid-class true)
                  (cp/enter-text rfr/email-address-input-element-selector valid-email)
                  (cp/test-field-doesnt-have-class rfr/email-address-form-row-element-selector rfr/field-invalid-class))

         (testing "password"
                  (testing "- inputing valid password causes field valid class to appear"
                           (cp/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-valid-class)
                           (cp/enter-text rfr/password-input-element-selector valid-password)
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-valid-class))
                  (testing "- from valid password to invalid password causes field valid class to disappear"
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-valid-class)
                           (cp/enter-text rfr/password-input-element-selector too-short-password)
                           (cp/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-valid-class))
                  (testing "- from invalid password to valid password causes field valid class to appear and field invalid class to disappear"
                           (rfr/add-or-remove-class! rfr/password-form-row-element-selector rfr/field-invalid-class true)
                           (rfr/add-or-remove-class! rfr/password-form-row-element-selector rfr/field-valid-class false)
                           (cp/enter-text rfr/password-input-element-selector valid-password)
                           (cp/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-valid-class))
                  (testing "- from invalid password to invalid password causes field invalid class to remain"
                           (invalid-password-state!)
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (cp/enter-text rfr/password-input-element-selector too-short-password)
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class))))


(defn check-first-name-has-blank-validation-errors []
  (cp/test-field-has-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
  (rfrt/element-has-text rfr/first-name-validation-element-selector
                         (get-in rfr/translations [:index :register-first-name-blank-validation-message])))

(defn check-last-name-has-blank-validation-errors []
  (cp/test-field-has-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
  (rfrt/element-has-text rfr/last-name-validation-element-selector
                         (get-in rfr/translations [:index :register-last-name-blank-validation-message])))

(defn check-email-address-has-invalid-validation-errors []
  (cp/test-field-has-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
  (rfrt/element-has-text rfr/email-address-validation-element-selector
                         (get-in rfr/translations [:index :register-email-address-invalid-validation-message])))

(defn check-password-has-blank-validation-errors []
  (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
  (rfrt/element-has-text rfr/password-validation-element-selector
                         (get-in rfr/translations [:index :register-password-blank-validation-message])))


(deftest losing-focus
         (setup-index-page!)
         (utils/start)

         (testing "first-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (cp/lose-focus rfr/first-name-input-element-selector)
                           (check-first-name-has-blank-validation-errors)))

         (testing "- losing focus when too-long adds invalid field class and validation message"
                  (cp/set-value rfr/first-name-input-element-selector too-long-name)
                  (cp/lose-focus rfr/first-name-input-element-selector)
                  (cp/test-field-has-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                  (rfrt/element-has-text rfr/first-name-validation-element-selector
                                         (get-in rfr/translations [:index :register-first-name-too-long-validation-message])))

         (testing "- losing focus when valid removes invalid field class and validation message"
                  (cp/test-field-has-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                  (rfrt/element-has-text rfr/first-name-validation-element-selector
                                         (get-in rfr/translations [:index :register-first-name-too-long-validation-message]))

                  (cp/set-value rfr/first-name-input-element-selector valid-name)
                  (cp/lose-focus rfr/first-name-input-element-selector)

                  (cp/test-field-doesnt-have-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                  (rfrt/element-has-no-text rfr/first-name-validation-element-selector))

         (testing "last-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (cp/lose-focus rfr/last-name-input-element-selector)
                           (check-last-name-has-blank-validation-errors))

                  (testing "- losing focus when too-long adds invalid field class and validation message"
                           (cp/set-value rfr/last-name-input-element-selector too-long-name)
                           (cp/lose-focus rfr/last-name-input-element-selector)
                           (cp/test-field-has-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/last-name-validation-element-selector
                                                  (get-in rfr/translations [:index :register-last-name-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (cp/test-field-has-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/last-name-validation-element-selector
                                                  (get-in rfr/translations [:index :register-last-name-too-long-validation-message]))

                           (cp/set-value rfr/last-name-input-element-selector valid-name)
                           (cp/lose-focus rfr/last-name-input-element-selector)

                           (cp/test-field-doesnt-have-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-no-text rfr/last-name-validation-element-selector)))

         (testing "email-address"
                  (testing "- losing focus when invalid adds invalid field class and validation message"
                           (cp/set-value rfr/email-address-input-element-selector invalid-email)
                           (cp/lose-focus rfr/email-address-input-element-selector)
                           (check-email-address-has-invalid-validation-errors))

                  (testing "- losing focus when too-long adds invalid field class and validation message"
                           (cp/set-value rfr/email-address-input-element-selector too-long-email)
                           (cp/lose-focus rfr/email-address-input-element-selector)
                           (cp/test-field-has-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/email-address-validation-element-selector
                                                  (get-in rfr/translations [:index :register-email-address-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (cp/test-field-has-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/email-address-validation-element-selector
                                                  (get-in rfr/translations [:index :register-email-address-too-long-validation-message]))

                           (cp/set-value rfr/email-address-input-element-selector valid-email)
                           (cp/lose-focus rfr/email-address-input-element-selector)

                           (cp/test-field-doesnt-have-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-no-text rfr/email-address-validation-element-selector)))



         (testing "password"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (cp/lose-focus rfr/password-input-element-selector)
                           (check-password-has-blank-validation-errors))

                  (testing "- losing focus when too short adds invalid field class and validation message"
                           (cp/set-value rfr/password-input-element-selector too-short-password)
                           (cp/lose-focus rfr/password-input-element-selector)
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/password-validation-element-selector
                                                  (get-in rfr/translations [:index :register-password-too-short-validation-message])))

                  (testing "- losing focus when too long adds invalid field class and validation message"
                           (cp/set-value rfr/password-input-element-selector too-long-password)
                           (cp/lose-focus rfr/password-input-element-selector)
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/password-validation-element-selector
                                                  (get-in rfr/translations [:index :register-password-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and validation message"
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/password-validation-element-selector
                                                  (get-in rfr/translations [:index :register-password-too-long-validation-message])) ; gg|rs 18Sept - what about (element-has-any-text ...) ?

                           (cp/set-value rfr/password-input-element-selector valid-password)
                           (cp/lose-focus rfr/password-input-element-selector)

                           (cp/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-no-text rfr/password-validation-element-selector))))

(deftest submitting-invalid-forms
         (setup-index-page!)
         (utils/start)

         (testing "submitting empty form"
                  (cp/press-submit rfr/register-form-element-selector)
                  (check-first-name-has-blank-validation-errors)
                  (check-last-name-has-blank-validation-errors)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (cp/has-focus? rfr/first-name-input-element-selector))

         (testing "submitting form with only valid first-name"
                  (cp/enter-text rfr/first-name-input-element-selector valid-name)
                  (cp/press-submit rfr/register-form-element-selector)
                  (check-last-name-has-blank-validation-errors)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (cp/has-focus? rfr/last-name-input-element-selector))

         (testing "submitting form with only valid first-name and last-name"
                  (cp/enter-text rfr/last-name-input-element-selector valid-name)
                  (cp/press-submit rfr/register-form-element-selector)
                  (check-email-address-has-invalid-validation-errors)
                  (check-password-has-blank-validation-errors)
                  (cp/has-focus? rfr/email-address-input-element-selector))

         (testing "submitting form with only valid first-name, last-name and email-address"
                  (cp/enter-text rfr/email-address-input-element-selector valid-email)
                  (cp/press-submit rfr/register-form-element-selector)
                  (check-password-has-blank-validation-errors)
                  (cp/has-focus? rfr/password-input-element-selector)))

(deftest prevent-default-submit
         (setup-index-page!)
         (utils/start)

         (testing "prevents default when page has errors"
                  (let [submit-event (test-utils/create-event :submit)]
                    (rf/block-invalid-submit submit-event)
                    (cp/default-prevented? submit-event true)))

         (testing "doesn't prevent default when inputs are valid"
                  (let [submit-event (test-utils/create-event :submit)]
                    (cp/enter-text rfr/first-name-input-element-selector valid-name)
                    (cp/enter-text rfr/last-name-input-element-selector valid-name)
                    (cp/enter-text rfr/email-address-input-element-selector valid-email)
                    (cp/enter-text rfr/password-input-element-selector valid-password)
                    (rf/block-invalid-submit submit-event)
                    (testing "all error classes are removed"
                             (cp/test-field-doesnt-have-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                             (cp/test-field-doesnt-have-class rfr/last-name-form-row-element-selector rfr/field-invalid-class)
                             (cp/test-field-doesnt-have-class rfr/email-address-form-row-element-selector rfr/field-invalid-class)
                             (cp/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-invalid-class))
                    (cp/default-prevented? submit-event false))))
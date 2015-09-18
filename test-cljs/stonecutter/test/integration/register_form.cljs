(ns stonecutter.test.integration.register-form
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [dommy.utils :as du]
            [clojure.string :as string]
            [stonecutter.register-form :as rf]
            [stonecutter.utils :as utils]
            [stonecutter.renderer.register-form :as rfr]
            [stonecutter.test.renderer.register-form :as rfrt]
            [stonecutter.test.test-utils :as test-utils]
            [stonecutter.test.integration.change-password :as cp])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]
                   [stonecutter.test.macros :refer [load-template]]))


(defn setup-index-page! []
  (dommy/set-html! (sel1 :html)
                   (load-template "public/index.html")))

(def valid-name "Frank")
(def valid-email "frank@franky.fr")
(def valid-password "avalidpassword")
(def invalid-password "short")
(def register-form :.clj--register__form)



(defn invalid-password-state []
  (cp/enter-text rfr/password-input-element-selector invalid-password)
  (cp/lose-focus rfr/password-input-element-selector)
  @rf/form-state)


(defn replace-state-with [new-state]
  (swap! rf/form-state new-state))

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
                           (cp/enter-text rfr/password-input-element-selector invalid-password)
                           (cp/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-valid-class))
                  (testing "- from invalid password to valid password causes field valid class to appear and field invalid class to disappear"
                           (rfr/add-or-remove-class! rfr/password-form-row-element-selector rfr/field-invalid-class true)
                           (rfr/add-or-remove-class! rfr/password-form-row-element-selector rfr/field-valid-class false)
                           (cp/enter-text rfr/password-input-element-selector valid-password)
                           (cp/test-field-doesnt-have-class rfr/password-form-row-element-selector rfr/field-invalid-class)
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-valid-class))
                  #_(testing "- from invalid password to invalid password causes field invalid class to remain"
                           (replace-state-with (invalid-password-state))
                           (cp/enter-text rfr/password-input-element-selector invalid-password)
                           (cp/test-field-has-class rfr/password-form-row-element-selector rfr/field-invalid-class))))


(deftest losing-focus
         (setup-index-page!)
         (utils/start)
         (testing "first-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (cp/lose-focus rfr/first-name-input-element-selector)
                           (cp/test-field-has-class rfr/first-name-form-row-element-selector rfr/field-invalid-class)
                           (rfrt/element-has-text rfr/first-name-validation-element-selector
                                                  (get-in rfr/translations [:index :register-first-name-blank-validation-message])))))
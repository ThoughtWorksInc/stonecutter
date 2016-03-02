(ns stonecutter.test.integration.change-profile-form
  (:require [cemerick.cljs.test]
            [stonecutter.test.test-utils :as tu]
            [stonecutter.js.dom.change-profile-form :as cpfd]
            [stonecutter.js.controller.change-profile-form :as cpfc]
            [stonecutter.js.app :as app]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.validation :as v])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [stonecutter.test.macros :refer [load-template]]))

(defonce change-profile-page-template (load-template "public/change-profile.html"))

(defn reset-change-profile-form-atom! []
  (reset! app/change-profile-details-form-state cpfc/default-state))

(defn clean-setup! []
  (tu/set-html! change-profile-page-template)
  (app/start)
  (reset-change-profile-form-atom!))

(deftest prevent-default-submit
         (testing "prevents default when page has errors"
                  (clean-setup!)
                  (let [submit-event (tu/create-event :submit)]
                    (cpfc/block-invalid-submit app/change-profile-details-form-state submit-event)
                    (tu/default-prevented? submit-event true)))

         (testing "doesn't prevent default when inputs are valid"
                  (clean-setup!)
                  (let [submit-event (tu/create-event :submit)]
                    (tu/enter-text (cpfd/input-selector :change-first-name) "valid-first-name")
                    (tu/enter-text (cpfd/input-selector :change-last-name) "valid-last-name")
                    (cpfc/block-invalid-submit app/change-profile-details-form-state submit-event)
                    (testing "all error classes are removed"
                             (tu/test-field-doesnt-have-class (cpfd/form-row-selector :change-first-name) cpfd/field-invalid-class)
                             (tu/test-field-doesnt-have-class (cpfd/form-row-selector :change-last-name) cpfd/field-invalid-class))
                    (tu/default-prevented? submit-event false))))

(defn check-upload-photo-has-too-large-validation-errors []
  (tu/test-field-has-class (cpfd/form-row-selector :change-profile-picture) cpfd/field-invalid-class)
  (tu/element-has-text (cpfd/validation-selector :change-profile-picture)
                       (get-in dom/translations [:upload-profile-picture :picture-too-large-validation-message])))

(deftest on-input
         (testing "inputing text in first name field will cause field invalid class to disappear"
                  (clean-setup!)
                  (dom/add-class! (cpfd/form-row-selector :change-first-name) cpfd/field-invalid-class)
                  (tu/enter-text (cpfd/input-selector :change-first-name) "valid-name")
                  (tu/test-field-doesnt-have-class (cpfd/form-row-selector :change-first-name) cpfd/field-invalid-class))
         (testing "inputing text in last name field will cause field invalid class to disappear"
                  (clean-setup!)
                  (dom/add-class! (cpfd/form-row-selector :change-last-name) cpfd/field-invalid-class)
                  (tu/enter-text (cpfd/input-selector :change-last-name) "valid-name")
                  (tu/test-field-doesnt-have-class (cpfd/form-row-selector :change-last-name) cpfd/field-invalid-class))
         (testing "adding too large image will cause invalid class to appear"
                  (with-redefs [cpfd/get-file (constantly "too-large")
                                v/js-image->size (constantly 5300000)]
                               (clean-setup!)
                               (tu/fire-change-event! (cpfd/input-selector :change-profile-picture))
                               (check-upload-photo-has-too-large-validation-errors)))
         (testing "adding acceptable image will cause invalid class to disappear"
                  (with-redefs [cpfd/get-file (constantly "valid")
                                v/js-image->size (constantly 5300)]
                               (clean-setup!)
                               (dom/add-class! (cpfd/form-row-selector :change-profile-picture) cpfd/field-invalid-class)
                               (tu/fire-change-event! (cpfd/input-selector :change-profile-picture))
                               (tu/test-field-doesnt-have-class (cpfd/form-row-selector :change-profile-picture) cpfd/field-invalid-class))))

(defn check-first-name-has-blank-validation-errors []
  (tu/test-field-has-class (cpfd/form-row-selector :change-first-name) cpfd/field-invalid-class)
  (tu/element-has-text (cpfd/validation-selector :change-first-name)
                       (get-in dom/translations [:index :register-first-name-blank-validation-message])))

(defn check-last-name-has-blank-validation-errors []
  (tu/test-field-has-class (cpfd/form-row-selector :change-last-name) cpfd/field-invalid-class)
  (tu/element-has-text (cpfd/validation-selector :change-last-name)
                       (get-in dom/translations [:index :register-last-name-blank-validation-message])))

(def valid-name "Frank")
(def too-long-name (tu/string-of-length 71))

(deftest losing-focus
         (testing "first-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (clean-setup!)
                           (tu/lose-focus (cpfd/input-selector :change-first-name))
                           (check-first-name-has-blank-validation-errors))

                  (testing "- losing focus when too-long adds invalid field class and validation message"
                           (clean-setup!)
                           (tu/set-value (cpfd/input-selector :change-first-name) too-long-name)
                           (tu/lose-focus (cpfd/input-selector :change-first-name))
                           (tu/test-field-has-class (cpfd/form-row-selector :change-first-name) cpfd/field-invalid-class)
                           (tu/element-has-text (cpfd/validation-selector :change-first-name)
                                                (get-in dom/translations [:index :register-first-name-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and there is novalidation message"
                           (clean-setup!)
                           (dom/add-class! (cpfd/form-row-selector :change-first-name) cpfd/field-invalid-class)
                           (tu/set-value (cpfd/input-selector :change-first-name) valid-name)
                           (tu/lose-focus (cpfd/input-selector :change-first-name))

                           (tu/test-field-doesnt-have-class (cpfd/form-row-selector :change-first-name) cpfd/field-invalid-class)
                           (tu/element-has-no-text (cpfd/validation-selector :change-first-name))))

         (testing "last-name"
                  (testing "- losing focus when blank adds invalid field class and validation message"
                           (clean-setup!)
                           (tu/lose-focus (cpfd/input-selector :change-last-name))
                           (check-last-name-has-blank-validation-errors))

                  (testing "- losing focus when too-long adds invalid field class and validation message"
                           (clean-setup!)
                           (tu/set-value (cpfd/input-selector :change-last-name) too-long-name)
                           (tu/lose-focus (cpfd/input-selector :change-last-name))
                           (tu/test-field-has-class (cpfd/form-row-selector :change-last-name) cpfd/field-invalid-class)
                           (tu/element-has-text (cpfd/validation-selector :change-last-name)
                                                (get-in dom/translations [:index :register-last-name-too-long-validation-message])))

                  (testing "- losing focus when valid removes invalid field class and there is no validation message"
                           (clean-setup!)
                           (dom/add-class! (cpfd/form-row-selector :change-last-name) cpfd/field-invalid-class)
                           (tu/set-value (cpfd/input-selector :change-last-name) valid-name)
                           (tu/lose-focus (cpfd/input-selector :change-last-name))

                           (tu/test-field-doesnt-have-class (cpfd/form-row-selector :change-last-name) cpfd/field-invalid-class)
                           (tu/element-has-no-text (cpfd/validation-selector :change-last-name)))))

(deftest submitting-invalid-forms
         (testing "submitting empty form"
                  (clean-setup!)
                  (tu/set-value (cpfd/input-selector :change-last-name) "   ")
                  (tu/set-value (cpfd/input-selector :change-first-name) "   ")
                  (tu/press-submit cpfd/change-profile-details-form-element-selector)
                  (check-first-name-has-blank-validation-errors)
                  (check-last-name-has-blank-validation-errors)
                  (tu/has-focus? (cpfd/input-selector :change-first-name)))

         (testing "submitting form with only valid first-name"
                  (clean-setup!)
                  (tu/set-value (cpfd/input-selector :change-first-name) valid-name)
                  (tu/set-value (cpfd/input-selector :change-last-name) "   ")
                  (tu/press-submit cpfd/change-profile-details-form-element-selector)
                  (check-last-name-has-blank-validation-errors)
                  (tu/has-focus? (cpfd/input-selector :change-last-name))))
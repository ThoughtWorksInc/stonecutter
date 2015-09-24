(ns stonecutter.test.integration.change-password
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [clojure.string :as string]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.app :as app]
            [stonecutter.js.dom.change-password :as cpd]
            [stonecutter.js.controller.change-password :as cpc]
            [stonecutter.test.test-utils :as tu])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing are]]
                   [dommy.core :refer [sel1 sel]]
                   [stonecutter.test.macros :refer [load-template]]))

(defonce change-password-template (load-template "public/change-password.html"))

(defn string-of-length [n]
  (apply str (repeat n "x")))

(def blank-string "")
(def invalid-password "blah")
(def valid-password "12345678")
(def valid-new-password "23456789")

(defn reset-change-password-form-atom! []
  (reset! app/change-password-form-state cpc/default-state))

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

(defn clean-setup! []
  (dommy/set-html! (sel1 :html) change-password-template)
  (app/start)
  (reset-change-password-form-atom!))

(defn invalid-new-password-state! []
  (clean-setup!)
  (tu/enter-text (cpd/input-selector :new-password) blank-string)
  (tu/lose-focus (cpd/input-selector :new-password)))

(deftest on-input
         (testing "inputing text in current-password field will cause field invalid class to disappear"
                  (clean-setup!)
                  (dom/add-class! (cpd/form-row-selector :current-password) cpd/field-invalid-class)
                  (tu/enter-text (cpd/input-selector :current-password) valid-password)
                  (tu/test-field-doesnt-have-class (cpd/form-row-selector :current-password) cpd/field-invalid-class))

         (testing "new-password"
                  (testing "- valid input will cause field invalid class to disappear"
                           (clean-setup!)
                           (dom/add-class! (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                           (tu/enter-text (cpd/input-selector :new-password) valid-new-password)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-invalid-class))
                  (testing "- valid input will cause field valid class to appear"
                           (clean-setup!)
                           (tu/enter-text (cpd/input-selector :new-password) valid-new-password)
                           (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-valid-class))
                  (testing "- from valid to invalid causes field valid class to disappear"
                           (clean-setup!)
                           (dom/add-class! (cpd/form-row-selector :new-password) cpd/field-valid-class)
                           (tu/enter-text (cpd/input-selector :new-password) invalid-password)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-valid-class))
                  (testing "- from invalid to invalid causes field invalid class to remain"
                           (invalid-new-password-state!)
                           (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                           (tu/enter-text (cpd/input-selector :new-password) invalid-password)
                           (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-invalid-class)))

         (testing "current-password & new-password interaction"
                  (testing "- if new-password matches current-password, valid class disappears from new-password"
                           (clean-setup!)
                           (tu/enter-text (cpd/input-selector :current-password) valid-password)
                           (dom/add-class! (cpd/form-row-selector :new-password) cpd/field-valid-class)
                           (tu/enter-text (cpd/input-selector :new-password) valid-password)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-valid-class))

                  (testing "- if new-password matches current-password, valid class disappears from new-password when current-password is entered later"
                           (clean-setup!)
                           (dom/add-class! (cpd/form-row-selector :new-password) cpd/field-valid-class)
                           (tu/enter-text (cpd/input-selector :new-password) valid-password)
                           (tu/enter-text (cpd/input-selector :current-password) valid-password)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-valid-class))))

(deftest losing-focus
         (testing "current password"
                  (let [current-password-error-message (get-in dom/translations [:change-password-form :current-password-invalid-validation-message])]
                    (are [?invalid-password]
                         (testing "- losing focus when invalid input adds invalid field class and validation message"
                                  (clean-setup!)
                                  (tu/set-value (cpd/input-selector :current-password) ?invalid-password)
                                  (tu/lose-focus (cpd/input-selector :current-password))
                                  (tu/test-field-has-class (cpd/form-row-selector :current-password) cpd/field-invalid-class)
                                  (element-has-text (cpd/validation-selector :current-password) current-password-error-message))
                         ;?invalid-password
                         blank-string
                         "2short"
                         (string-of-length 255)))

                  (testing "- losing focus when valid does not add invalid field class and there is no validation message"
                           (clean-setup!)
                           (tu/set-value (cpd/input-selector :current-password) valid-password)
                           (tu/lose-focus (cpd/input-selector :current-password))
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :current-password) cpd/field-invalid-class)
                           (element-has-no-text (cpd/validation-selector :current-password))))

         (testing "new password field"
                  (are [?invalid-password ?translation-key]
                       (let [validation-message (get-in dom/translations [:change-password-form ?translation-key])]
                         (testing "- losing focus when invalid input adds invalid field class, removes valid class and adds validation message"
                                  (clean-setup!)
                                  (tu/set-value (cpd/input-selector :new-password) ?invalid-password)
                                  (tu/lose-focus (cpd/input-selector :new-password))
                                  (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                                  (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-valid-class)
                                  (element-has-text (cpd/validation-selector :new-password) validation-message)))
                       ;?invalid-password       ;?translation-key
                       blank-string             :new-password-blank-validation-message
                       "2short"                 :new-password-too-short-validation-message
                       (string-of-length 255)   :new-password-too-long-validation-message)

                  (testing "- losing focus when valid adds valid class, does not add invalid field class and there is no validation message"
                           (clean-setup!)
                           (tu/set-value (cpd/input-selector :new-password) valid-password)
                           (tu/lose-focus (cpd/input-selector :new-password))
                           (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-valid-class)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                           (element-has-no-text (cpd/validation-selector :new-password)))

                  (testing "- losing focus when new-password matches current-password adds invalid field class, removes valid class and adds validation message"
                           (clean-setup!)
                           (tu/set-value (cpd/input-selector :current-password) valid-password)
                           (tu/lose-focus (cpd/input-selector :current-password))
                           (tu/set-value (cpd/input-selector :new-password) valid-password)
                           (tu/lose-focus (cpd/input-selector :new-password))
                           (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-valid-class)
                           (element-has-text (cpd/validation-selector :new-password) (get-in dom/translations [:change-password-form :new-password-unchanged-validation-message])))

                  (testing "- losing focus when passwords are different adds valid class, does not add invalid class and there is no validation message"
                           (clean-setup!)
                           (tu/set-value (cpd/input-selector :current-password) valid-password)
                           (tu/lose-focus (cpd/input-selector :current-password))
                           (tu/set-value (cpd/input-selector :new-password) valid-new-password)
                           (tu/lose-focus (cpd/input-selector :new-password))
                           (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-valid-class)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                           (element-has-no-text (cpd/validation-selector :new-password)))))

(deftest submitting-invalid-forms
         (testing "submitting empty form"
                  (clean-setup!)
                  (tu/press-submit cpd/change-password-form-element-selector)
                  (tu/test-field-has-class (cpd/form-row-selector :current-password) cpd/field-invalid-class)
                  (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                  (tu/has-focus? (cpd/input-selector :current-password)))

         (testing "submitting form with only valid current-password"
                  (clean-setup!)
                  (tu/enter-text (cpd/input-selector :current-password) valid-password)
                  (tu/press-submit cpd/change-password-form-element-selector)
                  (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                  (tu/has-focus? (cpd/input-selector :new-password))))

(deftest prevent-default-submit
         (testing "prevents default when page has errors"
                  (clean-setup!)
                  (let [submit-event (tu/create-event :submit)]
                    (cpc/block-invalid-submit app/change-password-form-state submit-event)
                    (tu/default-prevented? submit-event true)))

         (testing "doesn't prevent default when inputs are valid"
                  (clean-setup!)
                  (let [submit-event (tu/create-event :submit)]
                    (tu/enter-text (cpd/input-selector :current-password) valid-password)
                    (tu/enter-text (cpd/input-selector :new-password) valid-new-password)
                    (cpc/block-invalid-submit app/change-password-form-state submit-event)
                    (testing "all error classes are removed"
                             (tu/test-field-doesnt-have-class (cpd/form-row-selector :current-password) cpd/field-invalid-class)
                             (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-invalid-class))
                    (tu/default-prevented? submit-event false))))

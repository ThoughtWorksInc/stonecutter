(ns stonecutter.test.integration.change-password
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [stonecutter.js.change-password :as cp]
            [stonecutter.js.app :as app]
            [stonecutter.js.renderer.change-password :as cpd]
            [stonecutter.js.controller.change-password :as cpc]
            [stonecutter.test.test-utils :as tu])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]
                   [stonecutter.test.macros :refer [load-template]]))

(def blank-string "")
(def invalid-password "blah")
(def valid-password "12345678")
(def valid-new-password "23456789")

(def change-password-form :.clj--change-password__form)
(def validation-summary :.clj--validation-summary)
(def current-password-input :#current-password)
(def current-password-field :.clj--current-password)
(def new-password-field :.clj--new-password)
(def new-password-input :#new-password)

(def form-row-new-password-error-class :.cljs--new-password-form-row__inline-message)
(def form-row-current-password-error-class :.cljs--current-password-form-row__inline-message)

(def show-validation-form "validation-summary--show")

(def field-valid-class "form-row--valid")
(def field-invalid-class "form-row--invalid")

(def change-password-template (load-template "public/change-password.html"))

(defn setup-change-password-page! []
  (dommy/set-html! (sel1 :html) change-password-template))

(defn reset-change-password-form-atom! []
  (reset! app/change-password-form-state cpc/default-state))

(defn clean-setup! []
  (setup-change-password-page!)
  (app/start)
  (reset-change-password-form-atom!))


(defn invalid-new-password-state! []
  (clean-setup!)
  (tu/enter-text (cpd/input-selector :new-password) blank-string)
  (tu/lose-focus (cpd/input-selector :new-password)))

(deftest on-input
         (testing "inputing text in current-password field will cause field invalid class to disappear"
                  (clean-setup!)
                  (cpd/add-class! (cpd/form-row-selector :current-password) cpd/field-invalid-class)
                  (tu/enter-text (cpd/input-selector :current-password) valid-password)
                  (tu/test-field-doesnt-have-class (cpd/form-row-selector :current-password) cpd/field-invalid-class))

         (testing "new-password"
                  (testing "- valid input will cause field invalid class to disappear"
                           (clean-setup!)
                           (cpd/add-class! (cpd/form-row-selector :new-password) cpd/field-invalid-class)
                           (tu/enter-text (cpd/input-selector :new-password) valid-new-password)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-invalid-class))
                  (testing "- valid input will cause field valid class to appear"
                           (clean-setup!)
                           (tu/enter-text (cpd/input-selector :new-password) valid-new-password)
                           (tu/test-field-has-class (cpd/form-row-selector :new-password) cpd/field-valid-class))
                  (testing "- from valid to invalid causes field valid class to disappear"
                           (clean-setup!)
                           (cpd/add-class! (cpd/form-row-selector :new-password) cpd/field-valid-class)
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
                           (cpd/add-class! (cpd/form-row-selector :new-password) cpd/field-valid-class)
                           (tu/enter-text (cpd/input-selector :new-password) valid-password)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-valid-class))

                  (testing "- if new-password matches current-password, valid class disappears from new-password when current-password is entered later"
                           (clean-setup!)
                           (cpd/add-class! (cpd/form-row-selector :new-password) cpd/field-valid-class)
                           (tu/enter-text (cpd/input-selector :new-password) valid-password)
                           (tu/enter-text (cpd/input-selector :current-password) valid-password)
                           (tu/test-field-doesnt-have-class (cpd/form-row-selector :new-password) cpd/field-valid-class))))

(deftest losing-focus-on-input-fields
         (setup-change-password-page!)
         (app/start)

         (testing "current password field"
                  (testing "losing focus when blank adds invalid field class"
                           (tu/lose-focus current-password-input)
                           (tu/test-field-has-class current-password-field field-invalid-class)
                           (tu/has-message-on-selector form-row-current-password-error-class (get-in cpd/error-to-message [:current-password :blank])))

                  (testing "losing focus when correct format does not add invalid field class"
                           (tu/enter-text current-password-input valid-password)
                           (tu/lose-focus current-password-input)
                           (tu/test-field-doesnt-have-class current-password-field field-invalid-class))

                  (testing "invalid input does not add invalid field class before losing focus"
                           (tu/enter-text current-password-input invalid-password)
                           (tu/test-field-doesnt-have-class current-password-field field-invalid-class))

                  (testing "losing focus when incorrect format adds invalid field class"
                           (tu/lose-focus current-password-input)
                           (tu/test-field-has-class current-password-field field-invalid-class)))

         (reset-change-password-form-atom!)

         (testing "new password field"
                  (testing "losing focus when blank adds invalid field class"
                           (tu/lose-focus new-password-input)
                           (tu/test-field-has-class new-password-field field-invalid-class)
                           (tu/has-message-on-selector form-row-new-password-error-class (get-in cpd/error-to-message [:new-password :blank])))

                  (testing "valid input adds valid field class"
                           (tu/enter-text new-password-input valid-password)
                           (tu/test-field-has-class new-password-field field-valid-class)
                           (tu/has-no-message-on-selector form-row-new-password-error-class))

                  (testing "losing focus when correct format does not add invalid field class"
                           (tu/lose-focus new-password-input)
                           (tu/test-field-has-class new-password-field field-valid-class)
                           (tu/test-field-doesnt-have-class new-password-field field-invalid-class))

                  (testing "invalid input does not add invalid field class before losing focus"
                           (tu/enter-text new-password-input invalid-password)
                           (tu/test-field-doesnt-have-class new-password-field field-invalid-class))

                  (testing "losing focus when incorrect format adds invalid field class"
                           (tu/lose-focus new-password-input)
                           (tu/has-message-on-selector form-row-new-password-error-class (get-in cpd/error-to-message [:new-password :too-short]))
                           (tu/test-field-has-class new-password-field field-invalid-class))

                  (testing "losing focus when password is unchange adds invalid field class and message"
                           (tu/enter-text current-password-input valid-password)
                           (tu/enter-text new-password-input valid-password)
                           (tu/lose-focus new-password-input)
                           (tu/has-message-on-selector form-row-new-password-error-class (get-in cpd/error-to-message [:new-password :unchanged]))
                           (tu/test-field-has-class new-password-field field-invalid-class))))

(deftest submitting-invalid-forms
         (setup-change-password-page!)
         (app/start)

         (testing "submitting empty form"
                  (tu/press-submit change-password-form)
                  (tu/has-focus? current-password-input))

         (testing "submitting form with only valid current-password"
                  (tu/enter-text current-password-input valid-password)
                  (tu/press-submit change-password-form)
                  (tu/has-focus? new-password-input)))

(deftest prevent-default-submit
         (setup-change-password-page!)
         (testing "prevents default when page has errors"
                  (let [submit-event (tu/create-event :submit)]
                    (cp/block-invalid-submit submit-event)
                    (tu/default-prevented? submit-event true)))
         (testing "doesn't prevent default when inputs are valid"
                  (let [submit-event (tu/create-event :submit)]
                    (tu/enter-text current-password-input valid-password)
                    (tu/enter-text new-password-input valid-new-password)
                    (cp/block-invalid-submit submit-event)
                    (testing "all error classes are removed"
                             (tu/test-field-doesnt-have-class current-password-field field-invalid-class)
                             (tu/test-field-doesnt-have-class new-password-field field-invalid-class))
                    (tu/default-prevented? submit-event false))))

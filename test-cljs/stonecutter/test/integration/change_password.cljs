(ns stonecutter.test.integration.change-password
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [stonecutter.change-password :as cp]
            [stonecutter.utils :as utils]
            [stonecutter.renderer.change-password :as r]
            [stonecutter.test.test-utils :as tu])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]
                   [stonecutter.test.macros :refer [load-template]]))

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

(defn setup-page! [html]
  (dommy/set-html! (sel1 :html) html))

(defn reset-change-password-form-atom! []
  (reset! cp/change-password-form-state {}))

(def change-password-template (load-template "public/change-password.html"))

(defn test-field-validates-client-side [selector target-element]
  (tu/test-field-doesnt-have-class target-element field-valid-class)
  (tu/enter-text selector valid-password)
  (tu/test-field-has-class target-element field-valid-class)
  (tu/enter-text selector invalid-password)
  (tu/test-field-doesnt-have-class target-element field-valid-class))

(deftest current-password-validation-on-input
         (setup-page! change-password-template)
         (utils/start)

         (testing "correcting invalid password will cause field invalid class to disappear"
                  (tu/enter-text current-password-input invalid-password)
                  (tu/press-submit change-password-form)

                  (tu/enter-text current-password-input valid-password)
                  (tu/test-field-doesnt-have-class current-password-field field-invalid-class)))

(deftest new-password-validation-on-input
         (setup-page! change-password-template)
         (utils/start)
         (reset-change-password-form-atom!)

         (testing "typing in valid password causes valid class to appear"
                  (test-field-validates-client-side new-password-input new-password-field))

         (testing "valid class disappears if new password is the same as current password"
                  (tu/enter-text current-password-input valid-password)
                  (tu/enter-text new-password-input valid-password)
                  (tu/test-field-doesnt-have-class new-password-field field-valid-class))

         (setup-page! change-password-template)
         (utils/start)

         (testing "form rows are checked on current-password input event as well"
                  (tu/enter-text new-password-input valid-password)
                  (tu/enter-text current-password-input valid-password)
                  (tu/test-field-doesnt-have-class new-password-field field-valid-class))

         (testing "field error class gets removed if the new password is correct"
                  (tu/enter-text new-password-input invalid-password)
                  (tu/enter-text current-password-input invalid-password)
                  (tu/press-submit change-password-form)
                  (tu/enter-text current-password-input valid-password)
                  (tu/enter-text new-password-input valid-new-password)
                  (tu/test-field-doesnt-have-class new-password-field field-invalid-class)))

(deftest losing-focus-on-input-fields
         (setup-page! change-password-template)
         (utils/start)

         (testing "current password field"
                  (testing "losing focus when blank adds invalid field class"
                           (tu/lose-focus current-password-input)
                           (tu/test-field-has-class current-password-field field-invalid-class)
                           (tu/has-message-on-selector form-row-current-password-error-class (get-in r/error-to-message [:current-password :blank])))

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
                           (tu/has-message-on-selector form-row-new-password-error-class (get-in r/error-to-message [:new-password :blank])))

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
                           (tu/has-message-on-selector form-row-new-password-error-class (get-in r/error-to-message [:new-password :too-short]))
                           (tu/test-field-has-class new-password-field field-invalid-class))

                  (testing "losing focus when password is unchange adds invalid field class and message"
                           (tu/enter-text current-password-input valid-password)
                           (tu/enter-text new-password-input valid-password)
                           (tu/lose-focus new-password-input)
                           (tu/has-message-on-selector form-row-new-password-error-class (get-in r/error-to-message [:new-password :unchanged]))
                           (tu/test-field-has-class new-password-field field-invalid-class))))

(deftest submitting-invalid-forms
         (setup-page! change-password-template)
         (utils/start)

         (testing "submitting empty form"
                  (tu/press-submit change-password-form)
                  (tu/has-focus? current-password-input))

         (testing "submitting form with only valid current-password"
                  (tu/enter-text current-password-input valid-password)
                  (tu/press-submit change-password-form)
                  (tu/has-focus? new-password-input)))

(deftest prevent-default-submit
         (setup-page! change-password-template)
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

(defn run-all []  (run-tests))

(ns stonecutter.test.change-password
  (:require [cemerick.cljs.test :as t]
            [dommy.core :as dommy]
            [dommy.utils :as du]
            [clojure.string :as string]
            [stonecutter.change-password :as cp]
            [stonecutter.test.test-utils :as test-utils])
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

(def show-validation-form "validation-summary--show")

(def field-valid-class "form-row--valid")
(def field-invalid-class "form-row--invalid")

(defn setup-page! [html]
    (dommy/set-html! (sel1 :html) html))

(def change-password-template (load-template "public/change-password.html"))

(defn enter-text [sel text]
      (dommy/set-value! (sel1 sel) text)
      (test-utils/fire! (sel1 sel) :input))

(defn press-submit [form-sel]
  (test-utils/fire! (sel1 form-sel) :submit))

(defn test-field-class-existance [has-class? selector valid-class]
  (is (= has-class? (dommy/has-class? (sel1 selector) valid-class))
      (if has-class?
        (str "field: " selector " does not contain correct class: " valid-class)
        (str "field: " selector " contains class " valid-class " when it shouldn't"))))

(def test-field-has-class (partial test-field-class-existance true))
(def test-field-doesnt-have-class (partial test-field-class-existance false))

(defn has-focus? [sel]
  (is (= (sel1 sel) (.-activeElement js/document))
      (str "Element " sel " does not have focus")))

(defn default-prevented? [event required]
  (is (= (.-defaultPrevented event) required)
      (str "Element " event " should have default prevented of " required)))

(defn test-field-validates-client-side [selector target-element]
  (test-field-doesnt-have-class target-element field-valid-class)
  (enter-text selector valid-password)
  (test-field-has-class target-element field-valid-class)
  (enter-text selector invalid-password)
  (test-field-doesnt-have-class target-element field-valid-class))

(deftest current-password-validation-on-input
         (setup-page! change-password-template)
         (cp/start)

         (testing "correcting invalid password will cause field invalid class to disappear"
                  (enter-text current-password-input invalid-password)
                  (press-submit change-password-form)

                  (enter-text current-password-input valid-password)
                  (test-field-doesnt-have-class current-password-field field-invalid-class)))

(deftest new-password-validation-on-input
         (setup-page! change-password-template)
         (cp/start)

         (testing "typing in valid password causes valid class to appear"
                  (test-field-validates-client-side new-password-input new-password-field))

         (testing "valid class disappears if new password is the same as current password"
                  (enter-text current-password-input valid-password)
                  (enter-text new-password-input valid-password)
                  (test-field-doesnt-have-class new-password-field field-valid-class))

         (setup-page! change-password-template)
         (cp/start)

         (testing "form rows are checked on current-password input event as well"
                  (enter-text new-password-input valid-password)
                  (enter-text current-password-input valid-password)
                  (test-field-doesnt-have-class new-password-field field-valid-class))

         (testing "field error class get removed if the new password is correct"
                  (enter-text new-password-input invalid-password)
                  (enter-text current-password-input invalid-password)
                  (press-submit change-password-form)
                  (enter-text current-password-input valid-password)
                  (enter-text new-password-input valid-new-password)
                  (test-field-doesnt-have-class new-password-field field-invalid-class)))

(defn has-summary-message [message]
  (let [validation-classes (sel :.validation-summary__item)
        err-messages (mapv (partial dommy/text) validation-classes)]
    (is (not (string/blank? message)) "Error message key returns blank string")
    (is (some #{message} err-messages)
        (str "Missing error message " message " when error occurs"))))

(defn has-no-duplicating-messages []
  (let [validation-classes (sel :.validation-summary__item)
        err-messages (mapv (partial dommy/text) validation-classes)]
    (is (= (count err-messages) (count (set err-messages)))
        "The same message appeared multiple times in list")))

(deftest submitting-invalid-forms
         (setup-page! change-password-template)
         (cp/start)

         (testing "submitting empty form"
                  (press-submit change-password-form)
                  (test-field-has-class validation-summary show-validation-form)
                  (test-field-has-class current-password-field field-invalid-class)
                  (test-field-has-class new-password-field field-invalid-class)
                  (has-focus? current-password-input)
                  (has-summary-message (get-in cp/error-to-message [:current-password :blank]))
                  (press-submit change-password-form)
                  (has-no-duplicating-messages))

         (testing "submitting form with current-password which is too short"
                  (enter-text current-password-input invalid-password)
                  (press-submit change-password-form)
                  (test-field-has-class current-password-field field-invalid-class)
                  (has-summary-message (get-in cp/error-to-message [:current-password :too-short])))

         (testing "submitting form with only valid current-password"
                  (enter-text current-password-input valid-password)
                  (press-submit change-password-form)
                  (test-field-doesnt-have-class current-password-field field-invalid-class)
                  (test-field-doesnt-have-class current-password-field field-valid-class)
                  (test-field-has-class new-password-field field-invalid-class)
                  (has-focus? new-password-input)
                  (has-summary-message (get-in cp/error-to-message [:new-password :blank])))

         (testing "submitting form with unchanged new-password"
                  (enter-text current-password-input valid-password)
                  (enter-text new-password-input valid-password)
                  (press-submit change-password-form)
                  (has-summary-message (get-in cp/error-to-message [:new-password :unchanged])))

         (testing "submitting form with new-password which is too short"
                  (enter-text new-password-input invalid-password)
                  (press-submit change-password-form)
                  (test-field-has-class new-password-field field-invalid-class)
                  (has-summary-message (get-in cp/error-to-message [:new-password :too-short])))

         (testing "submitting form with all valid inputs"
                  (enter-text new-password-input valid-new-password)
                  (press-submit change-password-form)
                  (test-field-doesnt-have-class validation-summary show-validation-form)
                  (test-field-doesnt-have-class current-password-field field-invalid-class)
                  (test-field-doesnt-have-class new-password-field field-invalid-class)))

(deftest prevent-default-submit
         (setup-page! change-password-template)
         (testing "prevents default when page has errors"
                  (let [submit-event (test-utils/create-event :submit)]
                    (cp/check-change-password! submit-event)
                    (default-prevented? submit-event true)))
         (testing "doesn't prevent default when inputs are valid"
                  (let [submit-event (test-utils/create-event :submit)]
                    (enter-text current-password-input valid-password)
                    (enter-text new-password-input valid-new-password)
                    (cp/check-change-password! submit-event)
                    (testing "all error classes are removed"
                             (test-field-doesnt-have-class current-password-field field-invalid-class)
                             (test-field-doesnt-have-class new-password-field field-invalid-class))
                    (default-prevented? submit-event false))))

(defn run-all []  (run-tests))

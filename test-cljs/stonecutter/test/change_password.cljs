(ns stonecutter.test.change-password
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [dommy.utils :as du]
            [clojure.string :as string]
            [stonecutter.change-password :as cp]
            [stonecutter.utils :as utils]
            [stonecutter.renderer.change-password :as r]
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

(defn enter-text [sel text]
      (dommy/set-value! (sel1 sel) text)
      (test-utils/fire! (sel1 sel) :input))

(defn press-submit [form-sel]
  (test-utils/fire! (sel1 form-sel) :submit))

(defn lose-focus [sel]
  (test-utils/fire! (sel1 sel) :blur))

(defn test-field-class-existance [has-class? selector css-class]
  (is (= has-class? (dommy/has-class? (sel1 selector) css-class))
      (if has-class?
        (str "field: " selector " does not contain expected class: " css-class)
        (str "field: " selector " contains class " css-class " when it shouldn't"))))

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
         (utils/start)

         (testing "correcting invalid password will cause field invalid class to disappear"
                  (enter-text current-password-input invalid-password)
                  (press-submit change-password-form)

                  (enter-text current-password-input valid-password)
                  (test-field-doesnt-have-class current-password-field field-invalid-class)))

(deftest new-password-validation-on-input
         (setup-page! change-password-template)
         (utils/start)
         (reset-change-password-form-atom!)

         (testing "typing in valid password causes valid class to appear"
                  (test-field-validates-client-side new-password-input new-password-field))

         (testing "valid class disappears if new password is the same as current password"
                  (enter-text current-password-input valid-password)
                  (enter-text new-password-input valid-password)
                  (test-field-doesnt-have-class new-password-field field-valid-class))

         (setup-page! change-password-template)
         (utils/start)

         (testing "form rows are checked on current-password input event as well"
                  (enter-text new-password-input valid-password)
                  (enter-text current-password-input valid-password)
                  (test-field-doesnt-have-class new-password-field field-valid-class))

         (testing "field error class gets removed if the new password is correct"
                  (enter-text new-password-input invalid-password)
                  (enter-text current-password-input invalid-password)
                  (press-submit change-password-form)
                  (enter-text current-password-input valid-password)
                  (enter-text new-password-input valid-new-password)
                  (test-field-doesnt-have-class new-password-field field-invalid-class)))

(defn has-message-on-selector [selector message]
  (let [selected-elements (sel selector)
        err-messages (mapv (partial dommy/text) selected-elements)]
    (is (not (string/blank? message)) "Error message key returns blank string")
    (is (some #{message} err-messages)
        (str "Missing error message " message " when error occurs"))))

(defn has-no-message-on-selector [selector]
  (let [error-message (dommy/text (sel1 selector))]
    (is (= "" error-message)
        (str "Expecting no error message but received: " error-message))))

(deftest losing-focus-on-input-fields
         (setup-page! change-password-template)
         (utils/start)

         (testing "current password field"
                  (testing "losing focus when blank adds invalid field class"
                           (lose-focus current-password-input)
                           (test-field-has-class current-password-field field-invalid-class)
                           (has-message-on-selector form-row-current-password-error-class (get-in r/error-to-message [:current-password :blank])))

                  (testing "losing focus when correct format does not add invalid field class"
                           (enter-text current-password-input valid-password)
                           (lose-focus current-password-input)
                           (test-field-doesnt-have-class current-password-field field-invalid-class))

                  (testing "invalid input does not add invalid field class before losing focus"
                           (enter-text current-password-input invalid-password)
                           (test-field-doesnt-have-class current-password-field field-invalid-class))

                  (testing "losing focus when incorrect format adds invalid field class"
                           (lose-focus current-password-input)
                           (test-field-has-class current-password-field field-invalid-class)))

         (reset-change-password-form-atom!)

         (testing "new password field"
                  (testing "losing focus when blank adds invalid field class"
                           (lose-focus new-password-input)
                           (test-field-has-class new-password-field field-invalid-class)
                           (has-message-on-selector form-row-new-password-error-class (get-in r/error-to-message [:new-password :blank])))

                  (testing "valid input adds valid field class"
                           (enter-text new-password-input valid-password)
                           (test-field-has-class new-password-field field-valid-class)
                           (has-no-message-on-selector form-row-new-password-error-class))

                  (testing "losing focus when correct format does not add invalid field class"
                           (lose-focus new-password-input)
                           (test-field-has-class new-password-field field-valid-class)
                           (test-field-doesnt-have-class new-password-field field-invalid-class))

                  (testing "invalid input does not add invalid field class before losing focus"
                           (enter-text new-password-input invalid-password)
                           (test-field-doesnt-have-class new-password-field field-invalid-class))

                  (testing "losing focus when incorrect format adds invalid field class"
                           (lose-focus new-password-input)
                           (has-message-on-selector form-row-new-password-error-class (get-in r/error-to-message [:new-password :too-short]))
                           (test-field-has-class new-password-field field-invalid-class))

                  (testing "losing focus when password is unchange adds invalid field class and message"
                           (enter-text current-password-input valid-password)
                           (enter-text new-password-input valid-password)
                           (lose-focus new-password-input)
                           (has-message-on-selector form-row-new-password-error-class (get-in r/error-to-message [:new-password :unchanged]))
                           (test-field-has-class new-password-field field-invalid-class))))

(deftest submitting-invalid-forms
         (setup-page! change-password-template)
         (utils/start)

         (testing "submitting empty form"
                  (press-submit change-password-form)
                  (has-focus? current-password-input))

         (testing "submitting form with only valid current-password"
                  (enter-text current-password-input valid-password)
                  (press-submit change-password-form)
                  (has-focus? new-password-input)))

(deftest prevent-default-submit
         (setup-page! change-password-template)
         (testing "prevents default when page has errors"
                  (let [submit-event (test-utils/create-event :submit)]
                    (cp/block-invalid-submit submit-event)
                    (default-prevented? submit-event true)))
         (testing "doesn't prevent default when inputs are valid"
                  (let [submit-event (test-utils/create-event :submit)]
                    (enter-text current-password-input valid-password)
                    (enter-text new-password-input valid-new-password)
                    (cp/block-invalid-submit submit-event)
                    (testing "all error classes are removed"
                             (test-field-doesnt-have-class current-password-field field-invalid-class)
                             (test-field-doesnt-have-class new-password-field field-invalid-class))
                    (default-prevented? submit-event false))))

(defn run-all []  (run-tests))

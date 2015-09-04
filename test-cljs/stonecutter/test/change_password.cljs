(ns stonecutter.test.change-password
  (:require [cemerick.cljs.test :as t]
            [dommy.core :as dommy]
            [dommy.utils :as du]
            [stonecutter.change-password :as cp]
            [stonecutter.test.test-utils :as test-utils])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1]]
                   [stonecutter.test.macros :refer [load-template]]))

(def form-row-valid-class "form-row__help--valid")

(def invalid-password "blah")
(def valid-password "12345678")
(def valid-new-password "23456789")

(def current-password-input :#current-password)
(def current-password-field :.clj--current-password)
(def new-password-field :.clj--new-password)
(def new-password-input :#new-password)

(def field-error-class "form-row--validation-error")

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
  (test-field-doesnt-have-class target-element form-row-valid-class)
  (enter-text selector valid-password)
  (test-field-has-class target-element form-row-valid-class)
  (enter-text selector invalid-password)
  (test-field-doesnt-have-class target-element form-row-valid-class))

(deftest password-validation
         (setup-page! change-password-template)
         (cp/start)
         (test-field-validates-client-side new-password-input :.form-row__help))

(deftest submitting-invalid-forms
         (setup-page! change-password-template)
         (cp/start)

         (testing "submitting empty form"
                  (press-submit :.clj--change-password__form)
                  (test-field-has-class current-password-field field-error-class)
                  (test-field-has-class new-password-field field-error-class)
                  (has-focus? current-password-input))

         (testing "submitting form with only valid current-password"
                  (enter-text current-password-input valid-password)
                  (press-submit :.clj--change-password__form)
                  (test-field-doesnt-have-class current-password-field field-error-class)
                  (test-field-has-class new-password-field field-error-class)
                  (has-focus? new-password-input))

         (testing "submitting form with all valid inputs"
                  (enter-text new-password-input valid-new-password)
                  (press-submit :.clj--change-password__form)
                  (test-field-doesnt-have-class current-password-field field-error-class)
                  (test-field-doesnt-have-class new-password-field field-error-class)))

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
                             (test-field-doesnt-have-class current-password-field field-error-class)
                             (test-field-doesnt-have-class new-password-field field-error-class))
                    (default-prevented? submit-event false))))

(defn run-all []  (run-tests))

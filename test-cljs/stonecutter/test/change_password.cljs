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

(def current-password-input :#current-password)
(def current-password-field :.clj--current-password)
(def new-password-field :.clj--new-password)
(def new-password-input :#new-password)
(def verify-password-field :.clj--confirm-new-password)

(def field-error-class "form-row--validation-error")

(defn setup-page! [html]
    (dommy/set-html! (sel1 :html) html))

(def change-password-template (load-template "public/change-password.html"))

(defn enter-text [sel text]
      (dommy/set-value! (sel1 sel) text)
      (test-utils/fire! (sel1 sel) :input))

(defn test-field-class-existance [has-class? selector valid-class]
  (is (= has-class? (dommy/has-class? (sel1 selector) valid-class))
      (if has-class?
        (str "field: " selector " does not contain correct class: " valid-class)
        (str "field: " selector " contains class " valid-class " when it shouldn't"))))

(def test-field-has-class (partial test-field-class-existance true))
(def test-field-doesnt-have-class (partial test-field-class-existance false))

(defn has-focus? [sel]
  (is (= (sel1 sel) (.-activeElement js/document))
      (str "Element " sel "does not have focus")))

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
         (test-utils/fire! (sel1 :.clj--change-password__form) :submit)
         (test-field-has-class current-password-field field-error-class)
         (test-field-has-class new-password-field field-error-class)
         (test-field-has-class verify-password-field field-error-class)
         (has-focus? current-password-input))

(defn run-all []  (run-tests))

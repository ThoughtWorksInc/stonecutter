(ns stonecutter.test.integration.register-form
  (:require [cemerick.cljs.test :as t]
            [stonecutter.renderer.register-form :as r]
            [stonecutter.test.change-password :as cp-test]
            [dommy.core :as dommy])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]
                   [stonecutter.test.macros :refer [load-template]]))

(defn setup-page! [html]
  (dommy/set-html! (sel1 :html) html))

(def index-page-template (load-template "public/index.html"))

(def default-state {:first-name    {:value "" :error nil}
                    :last-name     {:value "" :error nil}
                    :email-address {:value "" :error nil}
                    :password      {:value "" :error nil :tick nil}})

(deftest render!
         (setup-page! index-page-template)

         (testing "first name field"
                  (testing "- no error, does not add invalid class"
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/first-name-form-row-element-selector r/field-invalid-class))

                  (testing "- any error, adds an invalid class"
                           (r/render! (assoc-in default-state [:first-name :error] :ANYTHING))
                           (cp-test/test-field-has-class r/first-name-form-row-element-selector r/field-invalid-class))

                  (testing "- no error, removes invalid class"
                           (r/render! (assoc-in default-state [:first-name :error] :ANYTHING))
                           (cp-test/test-field-has-class r/first-name-form-row-element-selector r/field-invalid-class)
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/first-name-form-row-element-selector r/field-invalid-class))

                  (testing "- valid class is never added"
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/first-name-form-row-element-selector r/field-valid-class)
                           (r/render! (assoc-in default-state [:first-name :error] :ANYTHING))
                           (cp-test/test-field-doesnt-have-class r/first-name-form-row-element-selector r/field-valid-class)
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/first-name-form-row-element-selector r/field-valid-class)))

         (testing "last name field"
                  (testing "- no error, does not add invalid class"
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/last-name-form-row-element-selector r/field-invalid-class))

                  (testing "- any error, adds an invalid class"
                           (r/render! (assoc-in default-state [:last-name :error] :ANYTHING))
                           (cp-test/test-field-has-class r/last-name-form-row-element-selector r/field-invalid-class))

                  (testing "- no error, removes invalid class"
                           (r/render! (assoc-in default-state [:last-name :error] :ANYTHING))
                           (cp-test/test-field-has-class r/last-name-form-row-element-selector r/field-invalid-class)
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/last-name-form-row-element-selector r/field-invalid-class))

                  (testing "- valid class is never added"
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/last-name-form-row-element-selector r/field-valid-class)
                           (r/render! (assoc-in default-state [:last-name :error] :ANYTHING))
                           (cp-test/test-field-doesnt-have-class r/last-name-form-row-element-selector r/field-valid-class)
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/last-name-form-row-element-selector r/field-valid-class))))

(ns stonecutter.test.integration.register-form
  (:require [cemerick.cljs.test :as t]
            [stonecutter.renderer.register-form :as r]
            [stonecutter.test.change-password :as cp-test]
            [dommy.core :as dommy])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests are]]
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

         (testing "adding and removing invalid class from first name, last name and email address fields"
                  (are [?field-key ?form-row-selector]
                       (testing "tabular"
                                (testing "- no error, does not add invalid class"
                                         (r/render! default-state)
                                         (cp-test/test-field-doesnt-have-class ?form-row-selector r/field-invalid-class))

                                (testing "- any error, adds invalid class"
                                         (r/render! (assoc-in default-state [?field-key :error] :ANYTHING))
                                         (cp-test/test-field-has-class ?form-row-selector r/field-invalid-class))

                                (testing "- no error, removes invalid class"
                                         (r/render! (assoc-in default-state [?field-key :error] :ANYTHING))
                                         (cp-test/test-field-has-class ?form-row-selector r/field-invalid-class)
                                         (r/render! (assoc-in default-state [?field-key :error] nil))
                                         (cp-test/test-field-doesnt-have-class ?form-row-selector r/field-invalid-class)))

                       ;?field-key    ?form-row-selector
                       :first-name    r/first-name-form-row-element-selector
                       :last-name     r/last-name-form-row-element-selector
                       :email-address r/email-address-form-row-element-selector
                       :password      r/password-form-row-element-selector))


         (testing "valid class on password field"
                  (testing "- tick false, does not add valid class"
                           (r/render! default-state)
                           (cp-test/test-field-doesnt-have-class r/password-form-row-element-selector r/field-valid-class))

                  (testing "- tick true, adds valid class"
                           (r/render! (assoc-in default-state [:password :tick] true))
                           (cp-test/test-field-has-class r/password-form-row-element-selector r/field-valid-class))

                  (testing "- tick false, removes valid class"
                           (r/render! (assoc-in default-state [:password :tick] true))
                           (cp-test/test-field-has-class r/password-form-row-element-selector r/field-valid-class)
                           (r/render! (assoc-in default-state [:password :tick] false))
                           (cp-test/test-field-doesnt-have-class r/password-form-row-element-selector r/field-valid-class))))

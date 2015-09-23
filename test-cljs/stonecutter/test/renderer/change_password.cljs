(ns stonecutter.test.renderer.change-password
  (:require [cemerick.cljs.test]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.renderer.change-password :as cp]
            [stonecutter.test.integration.change-password :as cp-test]
            [stonecutter.test.test-utils :as tu]
            [dommy.core :as dommy])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]
                   [stonecutter.test.macros :refer [load-template]]))

(defn setup-page! [html]
  (dommy/set-html! (sel1 :html) html))

(def change-password-template (load-template "public/change-password.html"))

(def default-state {:current-password {:value "" :error nil} :new-password {:value "" :error nil :tick nil}})


(deftest render-new!
         (with-redefs [dom/add-class! tu/mock-add-class!
                       dom/remove-class! tu/mock-remove-class!
                       dom/set-text! tu/mock-set-text!]

                      (testing "current-password error adds invalid class"
                               (tu/reset-mock-call-state!)
                               (cp/render! (assoc-in default-state [:current-password :error] :ANYTHING))
                               (tu/test-add-class-was-called-with (cp/form-row-selector :current-password) cp/field-invalid-class))

                      (testing "no current-password error removes invalid class"
                               (tu/reset-mock-call-state!)
                               (cp/render! (assoc-in default-state [:current-password :error] nil))
                               (tu/test-remove-class-was-called-with (cp/form-row-selector :current-password) cp/field-invalid-class))

                      (testing "new-password error adds invalid class"
                               (tu/reset-mock-call-state!)
                               (cp/render! (assoc-in default-state [:new-password :error] :ANYTHING))
                               (tu/test-add-class-was-called-with (cp/form-row-selector :new-password) cp/field-invalid-class))

                      (testing "no new-password error removes invalid class"
                               (tu/reset-mock-call-state!)
                               (cp/render! (assoc-in default-state [:new-password :error] nil))
                               (tu/test-remove-class-was-called-with (cp/form-row-selector :new-password) cp/field-invalid-class))

                      #_(testing "error messages"           ; 23/09/2015 RS&GG wip
                               (testing "- current password"
                                        (testing "- blank"
                                                 (tu/reset-mock-call-state!)
                                                 (cp/render! (assoc-in default-state [:current-password :error] :blank))
                                                 (tu/test-set-text-was-called-with (cp/validation-selector :current-password)
                                                                                   (get-in dom/translations [:change-password-form :current-password-blank-validation-message])))
                                        #_(testing "- too short"
                                                 (tu/reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] :too-short))
                                                 (tu/test-set-text-was-called-with (rfd/validation-selector :registration-password)
                                                                                   (get-in dom/translations [:index :register-password-too-short-validation-message])))
                                        #_(testing "- too long"
                                                 (tu/reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] :too-long))
                                                 (tu/test-set-text-was-called-with (rfd/validation-selector :registration-password)
                                                                                   (get-in dom/translations [:index :register-password-too-long-validation-message])))
                                        #_(testing "- no error"
                                                 (tu/reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] nil))
                                                 (tu/test-set-text-was-called-with (rfd/validation-selector :registration-password) nil))))))

(deftest render!
         (setup-page! change-password-template)

         (testing "current password rendering"
                  (testing "error blank, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:current-password :error] :blank)]
                             (cp/render! state))
                           (tu/test-field-has-class cp-test/current-password-field cp-test/field-invalid-class)
                           (tu/has-message-on-selector cp-test/form-row-current-password-error-class (get-in cp/error-to-message [:current-password :blank])))

                  (testing "error too-short, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:current-password :error] :too-short)]
                             (cp/render! state))
                           (tu/test-field-has-class cp-test/current-password-field cp-test/field-invalid-class)
                           (tu/has-message-on-selector cp-test/form-row-current-password-error-class (get-in cp/error-to-message [:current-password :too-short])))

                  (testing "no error, gives no validation classes and no error message"
                           (cp/render! default-state)
                           (tu/test-field-doesnt-have-class cp-test/current-password-field cp-test/field-invalid-class)
                           (tu/test-field-doesnt-have-class cp-test/current-password-field cp-test/field-valid-class)
                           (tu/has-no-message-on-selector cp-test/form-row-current-password-error-class)))

         (testing "new password rendering"
                  (testing "error blank, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:new-password :error] :blank)]
                             (cp/render! state))
                           (tu/test-field-has-class cp-test/new-password-field cp-test/field-invalid-class)
                           (tu/has-message-on-selector cp-test/form-row-new-password-error-class (get-in cp/error-to-message [:new-password :blank])))

                  (testing "error too-short, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:new-password :error] :too-short)]
                             (cp/render! state))
                           (tu/test-field-has-class cp-test/new-password-field cp-test/field-invalid-class)
                           (tu/has-message-on-selector cp-test/form-row-new-password-error-class (get-in cp/error-to-message [:new-password :too-short])))

                  (testing "error unchaged, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:new-password :error] :unchanged)]
                             (cp/render! state))
                           (tu/test-field-has-class cp-test/new-password-field cp-test/field-invalid-class)
                           (tu/has-message-on-selector cp-test/form-row-new-password-error-class (get-in cp/error-to-message [:new-password :unchanged])))

                  (testing "no error, gives no validation classes and no error message"
                           (let [state (assoc-in default-state [:new-password :tick] true)]
                             (cp/render! state)
                             (tu/test-field-doesnt-have-class cp-test/new-password-field cp-test/field-invalid-class)
                             (tu/has-no-message-on-selector cp-test/form-row-new-password-error-class)))

                  (testing "tick true gives it a valid class"
                           (let [state (assoc-in default-state [:new-password :tick] true)]
                             (cp/render! state))
                           (tu/test-field-has-class cp-test/new-password-field cp-test/field-valid-class)
                           (tu/test-field-doesnt-have-class cp-test/new-password-field cp-test/field-invalid-class))

                  (testing "tick false removes valid class and does not add invalid class"
                           (let [state (assoc-in default-state [:new-password :tick] false)]
                             (cp/render! state))
                           (tu/test-field-doesnt-have-class cp-test/new-password-field cp-test/field-valid-class)
                           (tu/test-field-doesnt-have-class cp-test/new-password-field cp-test/field-invalid-class))))
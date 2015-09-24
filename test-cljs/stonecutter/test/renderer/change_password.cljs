(ns stonecutter.test.renderer.change-password
  (:require [cemerick.cljs.test]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.renderer.change-password :as cp]
            [stonecutter.test.test-utils :as tu])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests are]]))

(def default-state {:current-password {:value "" :error nil}
                    :new-password     {:value "" :error nil :tick nil}})


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

                      (testing "error messages"
                               (are [?error]
                                    (testing "- current password gets the same error message for all errors"
                                             (let [error-message (get-in dom/translations [:change-password-form :current-password-invalid-validation-message])]
                                               (tu/reset-mock-call-state!)
                                               (cp/render! (assoc-in default-state [:current-password :error] ?error))
                                               (tu/test-set-text-was-called-with (cp/validation-selector :current-password) error-message)))
                                    ;?error
                                    :blank
                                    :too-short
                                    :too-long
                                    :invalid)

                               (testing "- current password gets no error message when there is no error"
                                        (tu/reset-mock-call-state!)
                                        (cp/render! (assoc-in default-state [:current-password :error] nil))
                                        (tu/test-set-text-was-called-with (cp/validation-selector :current-password) nil))

                               (are [?error ?translation-key]
                                    (testing "- new password gets the correct error message"
                                             (let [error-message (get-in dom/translations [:change-password-form ?translation-key])]
                                               (tu/reset-mock-call-state!)
                                               (cp/render! (assoc-in default-state [:new-password :error] ?error))
                                               (tu/test-set-text-was-called-with (cp/validation-selector :new-password) error-message)))
                                    ;?error     ?translation-key
                                    :blank      :new-password-blank-validation-message
                                    :too-short  :new-password-too-short-validation-message
                                    :too-long   :new-password-too-long-validation-message
                                    :unchanged  :new-password-unchanged-validation-message)

                               (testing "- new password gets no error message when there is no error"
                                        (tu/reset-mock-call-state!)
                                        (cp/render! (assoc-in default-state [:new-password :error] nil))
                                        (tu/test-set-text-was-called-with (cp/validation-selector :new-password) nil)))

                      (testing "new-password :tick set to true adds valid class"
                               (tu/reset-mock-call-state!)
                               (cp/render! (assoc-in default-state [:new-password :tick] true))
                               (tu/test-add-class-was-called-with (cp/form-row-selector :new-password) cp/field-valid-class))))

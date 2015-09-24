(ns stonecutter.test.unit.change-password
  (:require [cemerick.cljs.test]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.renderer.change-password :as cpr]
            [stonecutter.js.controller.change-password :as cpc]
            [stonecutter.test.test-utils :as tu])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests are]]))

(def default-state {:current-password {:value "" :error nil}
                    :new-password     {:value "" :error nil :tick nil}})

(defn string-of-length [n]
  (apply str (repeat n "x")))

(deftest update-model-on-blur
         (testing "current-password error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:current-password {:value input-value :error expected-output-error}}
                          (cpc/update-current-password-blur {:current-password {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "some-valid-password"         nil
                       nil                           :blank
                       ""                            :blank
                       "                "            :blank
                       "\t\t\t\t\t\t\t\t"            :blank
                       (string-of-length 8)          nil
                       (string-of-length 7)          :too-short
                       (string-of-length 254)        nil
                       (string-of-length 255)        :too-long))

         (testing "new-password error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:new-password {:value input-value :error expected-output-error}}
                          (cpc/update-new-password-blur {:new-password {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "some-valid-password"         nil
                       nil                           :blank
                       ""                            :blank
                       "                "            :blank
                       "\t\t\t\t\t\t\t\t"            :blank
                       (string-of-length 8)          nil
                       (string-of-length 7)          :too-short
                       (string-of-length 254)        nil
                       (string-of-length 255)        :too-long))

         (testing "new-password vs current-password"
                  (testing "when they are the same, new-password error is set to :unchanged"
                           (let [result (cpc/update-new-password-blur {:current-password {:value "12345678"}
                                                                      :new-password     {:value "12345678"}})]
                             (is (= :unchanged (get-in result [:new-password :error])))))))

(deftest update-model-on-input
         (testing "current-password"
                  (testing "when input is valid, error should be nil"
                           (let [result (cpc/update-current-password-input {:current-password {:value "12345678"}})]
                             (is (= nil (get-in result [:current-password :error] :not-found)))))

                  (testing "when input is invalid, error is not changed"
                           (let [result (cpc/update-current-password-input {:current-password {:value "2short" :error :previous-error}})]
                             (is (= :previous-error (get-in result [:current-password :error])))))

                  (testing "new-password vs current-password"
                           (testing "when they are the same, new-password tick should be false"
                                    (let [result (cpc/update-current-password-input {:current-password {:value "12345678"}
                                                                                    :new-password     {:value "12345678"}})]
                                      (is (= false (get-in result [:new-password :tick])))))))

         (testing "new-password"
                  (testing "when input is valid, error should be nil, tick should be true"
                           (let [result (cpc/update-new-password-input {:new-password {:value "a-valid-password" :error "not-nil"}})]
                             (is (= nil (get-in result [:new-password :error] :not-found)))
                             (is (= true (get-in result [:new-password :tick])))))

                  (testing "when input is invalid, error is not changed, tick should be false"
                           (let [result (cpc/update-new-password-input {:new-password {:value "2short" :error :previous-error :tick true}})]
                             (is (= :previous-error (get-in result [:new-password :error])))
                             (is (= false (get-in result [:new-password :tick])))))

                  (testing "new-password vs current-password"
                           (testing "when they are the same, new-password tick should be false and new-password error is not changed"
                                    (let [result (cpc/update-new-password-input {:current-password {:value "12345678"}
                                                                                :new-password     {:value "12345678"
                                                                                                   :error :previous-error-state}})]
                                      (is (= false (get-in result [:new-password :tick])))
                                      (is (= :previous-error-state (get-in result [:new-password :error]))))))))


(deftest render!
         (with-redefs [dom/add-class! tu/mock-add-class!
                       dom/remove-class! tu/mock-remove-class!
                       dom/set-text! tu/mock-set-text!]

                      (testing "current-password error adds invalid class"
                               (tu/reset-mock-call-state!)
                               (cpr/render! (assoc-in default-state [:current-password :error] :ANYTHING))
                               (tu/test-add-class-was-called-with (cpr/form-row-selector :current-password) cpr/field-invalid-class))

                      (testing "no current-password error removes invalid class"
                               (tu/reset-mock-call-state!)
                               (cpr/render! (assoc-in default-state [:current-password :error] nil))
                               (tu/test-remove-class-was-called-with (cpr/form-row-selector :current-password) cpr/field-invalid-class))

                      (testing "new-password error adds invalid class"
                               (tu/reset-mock-call-state!)
                               (cpr/render! (assoc-in default-state [:new-password :error] :ANYTHING))
                               (tu/test-add-class-was-called-with (cpr/form-row-selector :new-password) cpr/field-invalid-class))

                      (testing "no new-password error removes invalid class"
                               (tu/reset-mock-call-state!)
                               (cpr/render! (assoc-in default-state [:new-password :error] nil))
                               (tu/test-remove-class-was-called-with (cpr/form-row-selector :new-password) cpr/field-invalid-class))

                      (testing "error messages"
                               (are [?error]
                                    (testing "- current password gets the same error message for all errors"
                                             (let [error-message (get-in dom/translations [:change-password-form :current-password-invalid-validation-message])]
                                               (tu/reset-mock-call-state!)
                                               (cpr/render! (assoc-in default-state [:current-password :error] ?error))
                                               (tu/test-set-text-was-called-with (cpr/validation-selector :current-password) error-message)))
                                    ;?error
                                    :blank
                                    :too-short
                                    :too-long
                                    :invalid)

                               (testing "- current password gets no error message when there is no error"
                                        (tu/reset-mock-call-state!)
                                        (cpr/render! (assoc-in default-state [:current-password :error] nil))
                                        (tu/test-set-text-was-called-with (cpr/validation-selector :current-password) nil))

                               (are [?error ?translation-key]
                                    (testing "- new password gets the correct error message"
                                             (let [error-message (get-in dom/translations [:change-password-form ?translation-key])]
                                               (tu/reset-mock-call-state!)
                                               (cpr/render! (assoc-in default-state [:new-password :error] ?error))
                                               (tu/test-set-text-was-called-with (cpr/validation-selector :new-password) error-message)))
                                    ;?error     ?translation-key
                                    :blank      :new-password-blank-validation-message
                                    :too-short  :new-password-too-short-validation-message
                                    :too-long   :new-password-too-long-validation-message
                                    :unchanged  :new-password-unchanged-validation-message)

                               (testing "- new password gets no error message when there is no error"
                                        (tu/reset-mock-call-state!)
                                        (cpr/render! (assoc-in default-state [:new-password :error] nil))
                                        (tu/test-set-text-was-called-with (cpr/validation-selector :new-password) nil)))

                      (testing "new-password :tick set to true adds valid class"
                               (tu/reset-mock-call-state!)
                               (cpr/render! (assoc-in default-state [:new-password :tick] true))
                               (tu/test-add-class-was-called-with (cpr/form-row-selector :new-password) cpr/field-valid-class))))

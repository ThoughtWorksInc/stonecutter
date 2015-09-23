(ns stonecutter.test.unit.register-form
  (:require [cemerick.cljs.test]
            [stonecutter.js.dom.register-form :as rfd]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.controller.register-form :as rfc])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing are]]))

(def default-state {:registration-first-name {:value "" :error nil}
                    :registration-last-name  {:value "" :error nil}
                    :registration-email      {:value "" :error nil}
                    :registration-password   {:value "" :error nil :tick nil}})

(defn string-of-length [n]
  (apply str (repeat n "x")))

(def email-of-length-254
  (str (string-of-length 250) "@x.y"))

(def email-of-length-255
  (str (string-of-length 251) "@x.y"))

(deftest update-model-on-blur
         (testing "first name error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:registration-first-name {:value input-value :error expected-output-error}}
                          (rfc/update-first-name-blur {:registration-first-name {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "something"            nil
                       ""                     :blank
                       "     "                :blank
                       "\t\t\t"               :blank
                       (string-of-length 70)  nil
                       (string-of-length 71)  :too-long))

         (testing "last name error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:registration-last-name {:value input-value :error expected-output-error}}
                          (rfc/update-last-name-blur {:registration-last-name {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "something"            nil
                       ""                     :blank
                       "     "                :blank
                       "\t\t\t"               :blank
                       (string-of-length 70)  nil
                       (string-of-length 71)  :too-long))

         (testing "email-address error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:registration-email {:value input-value :error expected-output-error}}
                          (rfc/update-email-address-blur {:registration-email {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "valid@email.com"             nil
                       "invalid-email-format"        :invalid
                       email-of-length-254           nil
                       email-of-length-255           :too-long))

         (testing "password error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:registration-password {:value input-value :error expected-output-error}}
                          (rfc/update-password-blur {:registration-password {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "some-valid-password"         nil
                       nil                           :blank
                       ""                            :blank
                       "                "            :blank
                       "\t\t\t\t\t\t\t\t"            :blank
                       (string-of-length 8)          nil
                       (string-of-length 7)          :too-short
                       (string-of-length 254)        nil
                       (string-of-length 255)        :too-long)))



(deftest update-model-on-input
         (testing "first name error is set to nil for any value"
                  (is (= {:registration-first-name {:value "Barry" :error nil}}
                         (rfc/update-first-name-input {:registration-first-name {:value "Barry" :error :anything}}))))
         (testing "last name error is set to nil for any value"
                  (is (= {:registration-last-name {:value "Barry" :error nil}}
                         (rfc/update-last-name-input {:registration-last-name {:value "Barry" :error :anything}}))))
         (testing "email-address error is set to nil for any value"
                  (is (= {:registration-email {:value "hello@world.com" :error nil}}
                         (rfc/update-email-address-input {:registration-email {:value "hello@world.com" :error :anything}}))))
         (testing "when password value is valid, error is set to nil and tick is set to true"
                  (is (= {:registration-password {:value "a-valid-password" :error nil :tick true}}
                         (rfc/update-password-input {:registration-password {:value "a-valid-password" :error :anything :tick :anything}}))))
         (testing "when password value is invalid, error is unchanged and tick is set to false"
                  (is (= {:registration-password {:value "short" :error :anything :tick false}}
                         (rfc/update-password-input {:registration-password {:value "short" :error :anything :tick true}})))))

(def mock-call-state (atom {}))

(defn reset-mock-call-state! []
  (reset! mock-call-state {}))

(defn mock-add-class! [selector css-class]
  (swap! mock-call-state update-in [:add-class-calls selector] conj css-class))

(defn test-add-class-was-called-with [selector css-class]
  (is (= css-class (some #{css-class} (get-in @mock-call-state [:add-class-calls selector])))
      (str "add-class! was not called with selector: " selector " and css class: " css-class)))

(defn mock-remove-class! [selector css-class]
  (swap! mock-call-state update-in [:remove-class-calls selector] conj css-class))

(defn test-remove-class-was-called-with [selector css-class]
  (is (= css-class (some #{css-class} (get-in @mock-call-state [:remove-class-calls selector])))
      (str "remove-class! was not called with selector: " selector " and css class: " css-class)))

(defn mock-set-text! [selector message]
  (swap! mock-call-state assoc-in [:set-text-calls selector] message))

(defn test-set-text-was-called-with [selector message]
  (is (= message (get-in @mock-call-state [:set-text-calls selector]))
      (str "the last call to set-text! with selector: '" selector "' did not have the message: \"" message "\"")))

(deftest render!
         (with-redefs [dom/add-class! mock-add-class!
                       dom/remove-class! mock-remove-class!
                       dom/set-text! mock-set-text!]
                      (testing "adding and removing invalid class from first name, last name and email address fields"
                               (are [?field-key ?form-row-selector]
                                    (testing "- tabular"
                                             (testing "- any error, adds invalid class"
                                                      (reset-mock-call-state!)
                                                      (rfc/render! (assoc-in default-state [?field-key :error] :ANYTHING))
                                                      (test-add-class-was-called-with ?form-row-selector rfd/field-invalid-class))

                                             (testing "- no error, removes invalid class"
                                                      (reset-mock-call-state!)
                                                      (rfc/render! (assoc-in default-state [?field-key :error] nil))
                                                      (test-remove-class-was-called-with ?form-row-selector rfd/field-invalid-class)))

                                    ;?field-key    ?form-row-selector
                                    :registration-first-name (rfd/form-row-selector :registration-first-name)
                                    :registration-last-name (rfd/form-row-selector :registration-last-name)
                                    :registration-email (rfd/form-row-selector :registration-email)
                                    :registration-password (rfd/form-row-selector :registration-password)))

                      (testing "error messages"
                               (testing "- first name"
                                        (testing "- blank"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-first-name :error] :blank))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-first-name)
                                                                                (get-in dom/translations [:index :register-first-name-blank-validation-message])))
                                        (testing "- too long"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-first-name :error] :too-long))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-first-name)
                                                                                (get-in dom/translations [:index :register-first-name-too-long-validation-message])))
                                        (testing "- no error"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-first-name :error] nil))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-first-name) nil)))

                               (testing "- last name"
                                        (testing "- blank"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-last-name :error] :blank))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-last-name)
                                                                                (get-in dom/translations [:index :register-last-name-blank-validation-message])))
                                        (testing "- too long"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-last-name :error] :too-long))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-last-name)
                                                                                (get-in dom/translations [:index :register-last-name-too-long-validation-message])))
                                        (testing "- no error"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-last-name :error] nil))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-last-name) nil)))

                               (testing "- email address"
                                        (testing "- invalid"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-email :error] :invalid))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-email)
                                                                                (get-in dom/translations [:index :register-email-address-invalid-validation-message])))
                                        (testing "- too long"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-email :error] :too-long))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-email)
                                                                                (get-in dom/translations [:index :register-email-address-too-long-validation-message])))
                                        (testing "- no error"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-last-name :error] nil))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-email) nil)))

                               (testing "- password"
                                        (testing "- blank"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] :blank))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-password)
                                                                                (get-in dom/translations [:index :register-password-blank-validation-message])))
                                        (testing "- too short"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] :too-short))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-password)
                                                                                (get-in dom/translations [:index :register-password-too-short-validation-message])))
                                        (testing "- too long"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] :too-long))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-password)
                                                                                (get-in dom/translations [:index :register-password-too-long-validation-message])))
                                        (testing "- no error"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] nil))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-password) nil))))

                      (testing "valid class on password field"
                               (testing "- tick true, adds valid class"
                                        (reset-mock-call-state!)
                                        (rfc/render! (assoc-in default-state [:registration-password :tick] true))
                                        (test-add-class-was-called-with (rfd/form-row-selector :registration-password) rfd/field-valid-class))
                               (testing "- tick false, removes valid class"
                                        (reset-mock-call-state!)
                                        (rfc/render! (assoc-in default-state [:registration-password :tick] false))
                                        (test-remove-class-was-called-with (rfd/form-row-selector :registration-password) rfd/field-valid-class)))))

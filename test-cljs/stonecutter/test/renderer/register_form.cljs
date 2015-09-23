(ns stonecutter.test.renderer.register-form
  (:require [cemerick.cljs.test]
            [stonecutter.js.dom.register-form :as rfd]
            [stonecutter.js.controller.register-form :as rfc])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing are]]))

(def default-state {:registration-first-name {:value "" :error nil}
                    :registration-last-name  {:value "" :error nil}
                    :registration-email      {:value "" :error nil}
                    :registration-password   {:value "" :error nil :tick nil}})

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

(deftest render!-unit-tests
         (with-redefs [rfd/add-class! mock-add-class!
                       rfd/remove-class! mock-remove-class!
                       rfd/set-text! mock-set-text!]
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
                                                                                (get-in rfd/translations [:index :register-first-name-blank-validation-message])))
                                        (testing "- too long"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-first-name :error] :too-long))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-first-name)
                                                                                (get-in rfd/translations [:index :register-first-name-too-long-validation-message])))
                                        (testing "- no error"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-first-name :error] nil))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-first-name) nil)))

                               (testing "- last name"
                                        (testing "- blank"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-last-name :error] :blank))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-last-name)
                                                                                (get-in rfd/translations [:index :register-last-name-blank-validation-message])))
                                        (testing "- too long"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-last-name :error] :too-long))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-last-name)
                                                                                (get-in rfd/translations [:index :register-last-name-too-long-validation-message])))
                                        (testing "- no error"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-last-name :error] nil))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-last-name) nil)))

                               (testing "- email address"
                                        (testing "- invalid"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-email :error] :invalid))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-email)
                                                                                (get-in rfd/translations [:index :register-email-address-invalid-validation-message])))
                                        (testing "- too long"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-email :error] :too-long))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-email)
                                                                                (get-in rfd/translations [:index :register-email-address-too-long-validation-message])))
                                        (testing "- no error"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-last-name :error] nil))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-email) nil)))

                               (testing "- password"
                                        (testing "- blank"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] :blank))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-password)
                                                                                (get-in rfd/translations [:index :register-password-blank-validation-message])))
                                        (testing "- too short"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] :too-short))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-password)
                                                                                (get-in rfd/translations [:index :register-password-too-short-validation-message])))
                                        (testing "- too long"
                                                 (reset-mock-call-state!)
                                                 (rfc/render! (assoc-in default-state [:registration-password :error] :too-long))
                                                 (test-set-text-was-called-with (rfd/validation-selector :registration-password)
                                                                                (get-in rfd/translations [:index :register-password-too-long-validation-message])))
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


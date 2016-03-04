(ns stonecutter.test.unit.change-profile-form
  (:require [cemerick.cljs.test]
            [stonecutter.js.controller.change-profile-form :as cpfc]
            [stonecutter.test.test-utils :as tu]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.dom.change-profile-form :as cpfd]
            [stonecutter.validation :as v])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests are]]))

(deftest update-model-on-blur
         (testing "first name error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:change-first-name {:value input-value :error expected-output-error}}
                          (cpfc/update-first-name-blur {:change-first-name {:value input-value :error nil}}))

                       ;input-value              expected-output-error
                       "something"               nil
                       ""                        :blank
                       "     "                   :blank
                       "\t\t\t"                  :blank
                       (tu/string-of-length 70)  nil
                       (tu/string-of-length 71)  :too-long))

         (testing "last name error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:change-last-name {:value input-value :error expected-output-error}}
                          (cpfc/update-last-name-blur {:change-last-name {:value input-value :error nil}}))

                       ;input-value              expected-output-error
                       "something"               nil
                       ""                        :blank
                       "     "                   :blank
                       "\t\t\t"                  :blank
                       (tu/string-of-length 70)  nil
                       (tu/string-of-length 71)  :too-long)))

(deftest update-model-on-change
         (testing "profile picture error is set correctly based on the value"
                  (are [image-size image-type error]
                       (with-redefs [v/js-image->size (constantly image-size)
                                     v/js-image->type (constantly image-type)]
                                    (is (= {:change-profile-picture {:value "" :error error}}
                                           (cpfc/update-profile-picture-change {:change-profile-picture {:value "" :error nil}}))))

                       ;image-size  image-type    error
                       10000        "image/jpeg"  nil
                       5300000      "image/jpeg"  :too-large
                       10000        "text/html"   :not-image)))

(deftest update-model-on-input
         (testing "first name error is set to nil for any value"
                  (is (= {:change-first-name {:value "Barry" :error nil}}
                         (cpfc/update-first-name-input {:change-first-name {:value "Barry" :error :anything}}))))
         (testing "last name error is set to nil for any value"
                  (is (= {:change-last-name {:value "Barry" :error nil}}
                         (cpfc/update-last-name-input {:change-last-name {:value "Barry" :error :anything}})))))

(deftest render!
         (with-redefs [dom/add-class! tu/mock-add-class!
                       dom/remove-class! tu/mock-remove-class!
                       dom/set-text! tu/mock-set-text!]
                      (testing "adding and removing invalid class from first name, last name and profile picture fields"
                               (are [?field-key ?form-row-selector]
                                    (testing "- tabular"
                                             (testing "- any error, adds invalid class"
                                                      (tu/reset-mock-call-state!)
                                                      (cpfc/render! (assoc-in cpfc/default-state [?field-key :error] :ANYTHING))
                                                      (tu/test-add-class-was-called-with ?form-row-selector cpfd/field-invalid-class))

                                             (testing "- no error, removes invalid class"
                                                      (tu/reset-mock-call-state!)
                                                      (cpfc/render! (assoc-in cpfc/default-state [?field-key :error] nil))
                                                      (tu/test-remove-class-was-called-with ?form-row-selector cpfd/field-invalid-class)))

                                    ;?field-key              ?form-row-selector
                                    :change-first-name       :.clj--first-name
                                    :change-last-name        :.clj--last-name
                                    :change-profile-picture  :.clj--upload-picture))

                      (testing "error messages"
                               (testing "- first name"
                                        (testing "- blank"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-first-name :error] :blank))
                                                 (tu/test-set-text-was-called-with :.clj--change-first-name__validation
                                                                                   (get-in dom/translations [:index :register-first-name-blank-validation-message])))
                                        (testing "- too long"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-first-name :error] :too-long))
                                                 (tu/test-set-text-was-called-with :.clj--change-first-name__validation
                                                                                   (get-in dom/translations [:index :register-first-name-too-long-validation-message])))
                                        (testing "- no error"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-first-name :error] nil))
                                                 (tu/test-set-text-was-called-with :.clj--change-first-name__validation nil)))

                               (testing "- last name"
                                        (testing "- blank"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-last-name :error] :blank))
                                                 (tu/test-set-text-was-called-with :.clj--change-last-name__validation
                                                                                   (get-in dom/translations [:index :register-last-name-blank-validation-message])))
                                        (testing "- too long"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-last-name :error] :too-long))
                                                 (tu/test-set-text-was-called-with :.clj--change-last-name__validation
                                                                                   (get-in dom/translations [:index :register-last-name-too-long-validation-message])))
                                        (testing "- no error"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-last-name :error] nil))
                                                 (tu/test-set-text-was-called-with :.clj--change-last-name__validation nil)))

                               (testing "- profile picture"
                                        (testing "- too large"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-profile-picture :error] :too-large))
                                                 (tu/test-set-text-was-called-with :.clj--upload-picture__validation
                                                                                   (get-in dom/translations [:upload-profile-picture :picture-too-large-validation-message])))
                                        (testing "- not an image"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-profile-picture :error] :not-image))
                                                 (tu/test-set-text-was-called-with :.clj--upload-picture__validation
                                                                                   (get-in dom/translations [:upload-profile-picture :picture-not-image-validation-message])))
                                        (testing "- no error"
                                                 (tu/reset-mock-call-state!)
                                                 (cpfc/render! (assoc-in cpfc/default-state [:change-profile-picture :error] nil))
                                                 (tu/test-set-text-was-called-with :.clj--upload-picture__validation nil))))))
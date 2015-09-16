(ns stonecutter.test.unit.register-form
  (:require [cemerick.cljs.test]
            [stonecutter.controller.register-form :as rf])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests are]]))

(defn string-of-length [n]
  (apply str (repeat n "x")))

(def email-of-length-254
  (str (string-of-length 250) "@x.y"))

(def email-of-length-255
  (str (string-of-length 251) "@x.y"))

(deftest update-model-on-blur
         (testing "first name error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:first-name {:value input-value :error expected-output-error}}
                          (rf/update-first-name-blur {:first-name {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "something"            nil
                       ""                     :blank
                       "     "                :blank
                       "\t\t\t"               :blank
                       (string-of-length 70)  nil
                       (string-of-length 71)  :too-long))

         (testing "last name error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:last-name {:value input-value :error expected-output-error}}
                          (rf/update-last-name-blur {:last-name {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "something"            nil
                       ""                     :blank
                       "     "                :blank
                       "\t\t\t"               :blank
                       (string-of-length 70)  nil
                       (string-of-length 71)  :too-long))

         (testing "email-address error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:email-address {:value input-value :error expected-output-error}}
                          (rf/update-email-address-blur {:email-address {:value input-value :error nil}}))

                       ;input-value           expected-output-error
                       "valid@email.com"             nil
                       "invalid-email-format"        :invalid
                       email-of-length-254           nil
                       email-of-length-255           :too-long))

         (testing "password error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:password {:value input-value :error expected-output-error}}
                          (rf/update-password-blur {:password {:value input-value :error nil}}))

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
                  (is (= {:first-name {:value "Barry" :error nil}}
                         (rf/update-first-name-input {:first-name {:value "Barry" :error :anything}}))))
         (testing "last name error is set to nil for any value"
                  (is (= {:last-name {:value "Barry" :error nil}}
                         (rf/update-last-name-input {:last-name {:value "Barry" :error :anything}}))))
         (testing "email-address error is set to nil for any value"
                  (is (= {:email-address {:value "hello@world.com" :error nil}}
                         (rf/update-email-address-input {:email-address {:value "hello@world.com" :error :anything}}))))
         (testing "when password value is valid, error is set to nil and tick is set to true"
                  (is (= {:password {:value "a-valid-password" :error nil :tick true}}
                         (rf/update-password-input {:password {:value "a-valid-password" :error :anything :tick :anything}}))))
         (testing "when password value is invalid, error is unchanged and tick is set to false"
                  (is (= {:password {:value "short" :error :anything :tick false}}
                         (rf/update-password-input {:password {:value "short" :error :anything :tick true}})))))




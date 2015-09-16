(ns stonecutter.test.unit.change-password
  (:require [cemerick.cljs.test]
            [stonecutter.controller.change-password :as cp])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]))

(deftest update-model-on-blur
         (testing "current-password"
                  (testing "when blank, field is invalid and error is blank"
                           (let [current-state {:current-password {:value "" :error nil} :new-password {}}
                                 expected-state {:current-password {:value "" :error :blank} :new-password {}}]
                             (is (= expected-state (cp/update-current-password-blur current-state))
                                 "blank current password did not produce error on blur")))

                  (testing "when password is less than 8 characters, field is invalid and error is too-short"
                           (let [current-state {:current-password {:value "123" :error nil} :new-password {}}
                                 expected-state {:current-password {:value "123" :error :too-short} :new-password {}}]
                             (is (= expected-state (cp/update-current-password-blur current-state))
                                 "short current password did not produce error on blur"))))

         (testing "new-password"
                  (testing "when blank, field is invalid and error is blank"
                           (let [current-state {:current-password {} :new-password {:value "" :error nil}}
                                 expected-state {:current-password {} :new-password {:value "" :error :blank}}]
                             (is (= expected-state (cp/update-new-password-blur current-state))
                                 "blank new password did not produce error on blur")))

                  (testing "when password is less than 8 characters, field is invalid and error is too-short"
                           (let [current-state {:current-password {} :new-password {:value "123" :error nil}}
                                 expected-state {:current-password {} :new-password {:value "123" :error :too-short}}]
                             (is (= expected-state (cp/update-new-password-blur current-state))
                                 "short new password did not produce error on blur")))

                  (testing "when password is same as current, field is invalid and error is unchanged"
                           (let [current-state {:current-password {:value "12345678" :error nil} :new-password {:value "12345678" :error nil}}
                                 expected-state {:current-password {:value "12345678" :error nil} :new-password {:value "12345678" :error :unchanged}}]
                             (is (= expected-state (cp/update-new-password-blur current-state))
                                 "unchanged new password did not produce error on blur")))))

(deftest update-model-on-input
         (testing "current-password"
                  (testing "when input is valid, error should be nil"
                           (let [current-state {:current-password {:value "12345678" :error :too-short} :new-password {}}
                                 expected-state {:current-password {:value "12345678" :error nil} :new-password {}}]
                             (is (= expected-state (cp/update-current-password-input current-state))
                                 "correcting an error did not clear error in state")))

                  (testing "when password is less than 8 characters, error is not changed"
                           (let [current-state {:current-password {:value "123" :error :anything} :new-password {}}
                                 expected-state {:current-password {:value "123" :error :anything} :new-password {}}]
                             (is (= expected-state (cp/update-current-password-input current-state))
                                 "short current password changed the error"))))

         (testing "new-password"
                  (testing "when input is valid, error should be nil, tick should be true"
                           (let [current-state {:current-password {} :new-password {:value "12345678" :error :something}}
                                 expected-state {:current-password {} :new-password {:value "12345678" :error nil :tick true}}]
                             (is (= expected-state (cp/update-new-password-input current-state))
                                 "correcting an error did not clear error in state for new password")))

                  (testing "when input is invalid, error should be the same, tick should be false"
                           (let [current-state {:current-password {} :new-password {:value "1234567" :error :anything :tick true}}
                                 expected-state {:current-password {} :new-password {:value "1234567" :error :anything :tick false}}]
                             (is (= expected-state (cp/update-new-password-input current-state))
                                 "short new password did not turn tick to false and/or changed the error")))

                  (testing "when input is the same as current-password, tick should be false and error should stay nil"
                           (let [current-state {:current-password {:value "12345678" :error :anything} :new-password {:value "12345678" :error nil :tick true}}
                                 expected-state {:current-password {:value "12345678" :error :anything} :new-password {:value "12345678" :error nil :tick false}}]
                             (is (= expected-state (cp/update-new-password-input current-state))
                                 "new password that is the same as current password did not turn tick to false and/or changed the error")))))

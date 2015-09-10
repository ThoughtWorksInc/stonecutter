(ns stonecutter.test.unit.change-password
  (:require [cemerick.cljs.test :as t]
            [stonecutter.controller.change-password :as cp])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]))

(deftest update-model-on-blur
         (testing "current-password"
                  (testing "when blank, field is invalid and error is blank"
                           (let [current-state {:current-password "" :error nil}
                                 expected-state {:current-password "" :error :blank}]
                             (is (= expected-state (cp/update-current-password-blur current-state)))))

                  (testing "when password is less than 8 characters, field is invalid and error is too-short"
                           (let [current-state {:current-password "123" :error nil}
                                 expected-state {:current-password "123" :error :too-short}]
                             (is (= expected-state (cp/update-current-password-blur current-state)))))))

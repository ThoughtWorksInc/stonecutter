(ns stonecutter.test.unit.change-password
  (:require [cemerick.cljs.test]
            [stonecutter.js.controller.change-password :as cp])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests are]]))

(defn string-of-length [n]
  (apply str (repeat n "x")))

(deftest update-model-on-blur
         (testing "current-password error is set correctly based on the value"
                  (are [input-value expected-output-error]
                       (= {:current-password {:value input-value :error expected-output-error}}
                          (cp/update-current-password-blur {:current-password {:value input-value :error nil}}))

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
                          (cp/update-new-password-blur {:new-password {:value input-value :error nil}}))

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
                           (let [result (cp/update-new-password-blur {:current-password {:value "12345678"}
                                                                      :new-password     {:value "12345678"}})]
                             (is (= :unchanged (get-in result [:new-password :error])))))))

(deftest update-model-on-input
         (testing "current-password"
                  (testing "when input is valid, error should be nil"
                           (let [result (cp/update-current-password-input {:current-password {:value "12345678"}})]
                             (is (= nil (get-in result [:current-password :error] :not-found)))))

                  (testing "when input is invalid, error is not changed"
                           (let [result (cp/update-current-password-input {:current-password {:value "2short" :error :previous-error}})]
                             (is (= :previous-error (get-in result [:current-password :error])))))

                  (testing "new-password vs current-password"
                           (testing "when they are the same, new-password tick should be false"
                                    (let [result (cp/update-current-password-input {:current-password {:value "12345678"}
                                                                                    :new-password     {:value "12345678"}})]
                                      (is (= false (get-in result [:new-password :tick])))))))

         (testing "new-password"
                  (testing "when input is valid, error should be nil, tick should be true"
                           (let [result (cp/update-new-password-input {:new-password {:value "a-valid-password" :error "not-nil"}})]
                             (is (= nil (get-in result [:new-password :error] :not-found)))
                             (is (= true (get-in result [:new-password :tick])))))

                  (testing "when input is invalid, error is not changed, tick should be false"
                           (let [result (cp/update-new-password-input {:new-password {:value "2short" :error :previous-error :tick true}})]
                             (is (= :previous-error (get-in result [:new-password :error])))
                             (is (= false (get-in result [:new-password :tick])))))

                  (testing "new-password vs current-password"
                           (testing "when they are the same, new-password tick should be false and new-password error is not changed"
                                    (let [result (cp/update-new-password-input {:current-password {:value "12345678"}
                                                                                :new-password     {:value "12345678"
                                                                                                   :error :previous-error-state}})]
                                      (is (= false (get-in result [:new-password :tick])))
                                      (is (= :previous-error-state (get-in result [:new-password :error]))))))))

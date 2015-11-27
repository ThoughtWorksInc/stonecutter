(ns stonecutter.test.unit.translations
  (:require [cemerick.cljs.test]
            [stonecutter.js.dom.register-form :as rfd]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.controller.client_translations :as ct])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests are]]))


(deftest about-handling-client-side-translations
         (testing "if given an unsupported language then falls back to english"
                  (with-redefs [dom/get-lang (constantly :xx)]
                               (is (= (ct/t (dom/get-lang) :index/register-first-name-blank-validation-message) "First name cannot be blank"))))
         (testing "if lang is updated then returns a translated message"
                  (with-redefs [dom/get-lang (constantly :en)]
                               (is (not (contains? (ct/t (dom/get-lang) :index/register-email-address-blank-validation-message) "Finnish"))))
                  (with-redefs [dom/get-lang (constantly :fi)]
                               (is (= (ct/t (dom/get-lang) :index/register-email-address-blank-validation-message) "Email address cannot be blank in Finnish")))))
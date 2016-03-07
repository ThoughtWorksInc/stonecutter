(ns stonecutter.test.integration.upload-photo
  (:require [cemerick.cljs.test]
            [stonecutter.js.app :as app]
            [stonecutter.test.test-utils :as tu]
            [stonecutter.js.dom.upload-photo :as ulp]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.validation :as v])
  (:require-macros [cemerick.cljs.test :refer [deftest is are testing]]
                   [stonecutter.test.macros :refer [load-template]]))

(defonce profile-page-template (load-template "public/profile.html"))

(defn clean-setup! []
  (tu/set-html! profile-page-template)
  (app/start))

(deftest on-mouse
         (testing "update photo button is hidden on page load"
                  (clean-setup!)
                  (tu/test-field-has-attr ulp/profile-card-photo__form :hidden))
         (testing "update photo button is shown on mouse enter and hidden on mouse leave"
                  (clean-setup!)
                  (tu/move-mouse-over-elem ulp/profile-card-photo__selector)
                  (tu/test-field-doesnt-have-attr ulp/profile-card-photo__form :hidden)
                  (tu/move-mouse-out-of-elem ulp/profile-card-photo__selector)
                  (tu/test-field-has-attr ulp/profile-card-photo__form :hidden)))

(deftest on-change
         (testing "error is not shown on page load"
                  (clean-setup!)
                  (tu/test-field-has-attr ulp/profile-card-photo__error-container :hidden))
         (testing "selecting an invalid image will show an error"
                  (are [?file ?file-size ?file-type ?translation-key]
                       (with-redefs [dom/get-file (constantly ?file)
                                     v/js-image->size (constantly ?file-size)
                                     v/js-image->type (constantly ?file-type)]
                                    (clean-setup!)
                                    (tu/fire-change-event! ulp/profile-card-photo__input)
                                    (tu/test-field-doesnt-have-attr ulp/profile-card-photo__error-container :hidden)
                                    (tu/element-has-text ulp/profile-card-photo__error-text
                                                         (get-in dom/translations [:upload-profile-picture ?translation-key])))
                       ;?file       ?file-size  ?file-type    ?translation-key
                       "too-large"  5300000     "image/jpeg"  :picture-too-large-validation-message
                       "not-image"  100         "text/html"   :picture-not-image-validation-message))
         (testing "selecting a valid image will submit the form and not show any errors"
                  (with-redefs [ulp/submit-form! tu/mock-submit-form!
                                dom/get-file (constantly "valid-file")
                                v/js-image->size (constantly 100)
                                v/js-image->type (constantly "image/jpeg")]
                               (clean-setup!)
                               (tu/fire-change-event! ulp/profile-card-photo__input)
                               (tu/test-field-has-attr ulp/profile-card-photo__error-container :hidden)
                               (tu/test-submit-form-was-called ulp/profile-card-photo__form))))
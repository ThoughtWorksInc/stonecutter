(ns stonecutter.test.view.confirmation-sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.view.confirmation-sign-in :refer [confirmation-sign-in-form]]
            [stonecutter.helper :as helper]))

(facts "about confirmation sign in form"
       (let [page (-> (th/create-request) confirmation-sign-in-form)]
         (fact "confirmation sign-in-form should return some html"
               page => (th/element-exists? [:form]))

         (fact "there are no missing translations"
               (th/test-translations "confirmation sign in form" confirmation-sign-in-form))

         (fact "work in progress should be removed from page"
               page => th/work-in-progress-removed)

         (fact "form should post to correct endpoint"
               page => (th/has-form-action? (r/path :confirmation-sign-in)))

         (fact "confirmation id should be set in form"
               (let [confirmation-id "confirmation-123"
                     page (-> (th/create-request {} nil {:confirmation-id confirmation-id}) confirmation-sign-in-form)]
                 page => (th/has-attr? [:.clj--confirmation-id__input] :value confirmation-id)))

         (fact "forgotten-password button should link to correct page"
               page => (th/has-attr? [:.clj--forgot-password]
                                     :href (r/path :show-forgotten-password-form)))))

(facts "about displaying errors"
       (facts "when password is invalid"
              (let [errors {:credentials :confirmation-invalid}
                    page (-> (th/create-request {} errors {}) confirmation-sign-in-form)]

                (fact "credentials validation element is present"
                      page => (th/element-exists? [:.validation-summary--show]))

                (fact "correct error message is displayed"
                      page => (th/has-attr? [:.clj--validation-summary__item]
                                            :data-l8n "content:confirmation-sign-in-form/invalid-credentials-validation-message"))

                (fact "invalid value is not preserved in input field"
                      (-> page (html/select [:.func--password__input]) first :attrs :value) => empty?))))

(ns stonecutter.test.view.reset-password
  (:require [midje.sweet :refer :all]
            [stonecutter.routes :as r]
            [stonecutter.view.reset-password :as rp]
            [stonecutter.test.view.test-helpers :as th]
            [net.cgrand.enlive-html :as html]))

(fact
 (th/test-translations "Reset password" rp/reset-password-form))

(fact "form posts to correct endpoint"
      (let [page (-> (th/create-request nil nil {:forgotten-password-id "some-uuid"}) rp/reset-password-form)]
        page => (th/has-form-action? (r/path :reset-password :forgotten-password-id "some-uuid"))
        page => (th/has-form-method? "post")))

(facts "about displaying errors"
       (tabular
         (let [page (-> (th/create-request {} ?errors {:forgotten-password-id "some-uuid"}) rp/reset-password-form)]
           (fact "validation-summary--show class is added to the validation summary element"
                 (-> (html/select page [[:.clj--validation-summary :.validation-summary--show]])) =not=> empty?)
           (fact "validation message is present as a validation summary item"
                 (html/select page [:.clj--validation-summary__item]) =not=> empty?)
           (fact "correct error messages are displayed"
                 (->> (html/select page [:.clj--validation-summary__item])
                      (map #(get-in % [:attrs :data-l8n]))) => ?validation-translations)
           (fact "correct elements are highlighted"
                 (->> (html/select page [:.form-row--validation-error])
                      (map #(get-in % [:attrs :class]))) => (contains ?highlighted-elements)))

         ?errors                          ?validation-translations                                                          ?highlighted-elements
         {:new-password :blank}           ["content:change-password-form/new-password-blank-validation-message"]            [#"clj--new-password"]
         {:new-password :too-short}       ["content:change-password-form/new-password-too-short-validation-message"]        [#"clj--new-password"]
         {:new-password :too-long}        ["content:change-password-form/new-password-too-long-validation-message"]         [#"clj--new-password"]
         {:confirm-new-password :invalid} ["content:change-password-form/confirm-new-password-invalid-validation-message"]  [#"clj--confirm-new-password"]
         {:new-password :too-short
          :confirm-new-password :invalid} ["content:change-password-form/new-password-too-short-validation-message"
                                           "content:change-password-form/confirm-new-password-invalid-validation-message"]  [#"clj--new-password"
                                                                                                                             #"clj--confirm-new-password"]))

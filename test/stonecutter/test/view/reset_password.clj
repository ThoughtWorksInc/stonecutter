(ns stonecutter.test.view.reset-password
  (:require [midje.sweet :refer :all]
            [stonecutter.routes :as r]
            [stonecutter.view.reset-password :as rp]
            [stonecutter.test.view.test-helpers :as th]))

(fact
 (th/test-translations "Reset password" rp/reset-password))

(fact "form posts to correct endpoint"
      (let [page (-> (th/create-request nil nil {:forgotten-password-id "some-uuid"}) rp/reset-password)]
        page => (th/has-form-action? (r/path :reset-password :forgotten-password-id "some-uuid"))
        page => (th/has-form-method? "post")))

(facts "about validation messages"
       (tabular
        (let [page (-> (th/create-request {} ?errors) rp/reset-password)]
          (fact "validation-summary--show class is added to the validation summary element")
          (fact "validation message is present as a validation summary item")
          (fact "correct error messages are displayed"))
        ?errors   ?validation-translations           ?highlighted-elements
        {}))

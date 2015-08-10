(ns stonecutter.test.view.forgotten-password-confirmation
  (:require [midje.sweet :refer :all]
            [stonecutter.view.forgotten-password-confirmation :as fpc]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]))

(fact
 (th/test-translations "Forgotten password email sent confirmation" fpc/forgotten-password-confirmation))

(fact "Forgotten password confirmation page has content"
      (-> (fpc/forgotten-password-confirmation {})
          (html/select [:.func--forgotten-password-confirmation-page])) =not=> empty?)

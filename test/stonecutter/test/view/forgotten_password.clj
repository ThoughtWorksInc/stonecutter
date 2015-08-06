(ns stonecutter.test.view.forgotten-password
  (:require [midje.sweet :refer :all]
            [stonecutter.view.forgotten-password :as fp]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]))

(future-fact
  (th/test-translations "Forgotten password form" fp/forgotten-password-form))

(fact "fp has correct action and method set"
      (let [form (-> (fp/forgotten-password-form {})
                     (html/select [:form])
                     first)]
        (-> form :attrs :action) => (r/path :send-forgotten-password-email)
        (-> form :attrs :method) => "post"))

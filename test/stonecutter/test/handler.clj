(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.handler :as h]))

(fact "can be split requests between html site and api"
      (let [site-handler (fn [r] :site)
            api-handler (fn [r] :api)
            handler (h/splitter site-handler api-handler)]

        (-> (mock/request :get "/blah") handler) => :site
        (-> (mock/request :get "/api/blah") handler) => :api))

(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.handler :as h]
            [stonecutter.db.storage :as storage]))

(def stores-m (storage/create-in-memory-stores))

(def app (h/create-app {:secure "false"} stores-m :dev-mode? false))

(fact "registration url returns a 200 response"
      (-> (mock/request :get "/register") app  :status) => 200)

(fact "sign-in url returns a 200 response"
      (-> (mock/request :get "/sign-in") app :status) => 200)

(fact "sign-out url returns a 302 response"
      (-> (mock/request :get "/sign-out") app :status) => 302)

(fact "unknown url returns a 404 response"
      (-> (mock/request :get "/unknown-url") app :status) => 404)

(fact "can be split requests between html site and api"
      (let [site-handler (fn [r] :site)
            api-handler (fn [r] :api)
            handler (h/splitter site-handler api-handler)]

        (-> (mock/request :get "/blah") handler) => :site
        (-> (mock/request :get "/api/blah") handler) => :api))

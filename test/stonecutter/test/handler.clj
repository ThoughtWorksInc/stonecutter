(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.handler :refer [app]]))

(fact "registration url returns a 200 response"
      (-> (mock/request :get "/register") app :status) => 200)

(defn p [v] (prn v) v)

(fact "sign-in url returns a 200 response"
      (-> (mock/request :get "/sign-in") app :status) => 200)

(fact "sign-out url returns a 302 response"
      (-> (mock/request :get "/sign-out") app :status) => 302)

(fact "unknown url returns a 404 response"
      (-> (mock/request :get "/unknown-url") app :status) => 404)

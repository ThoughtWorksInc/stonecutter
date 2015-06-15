(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.handler :refer [app]]))

(defn p [v] (prn v) v)

(fact "registration url returns a 200 response"
      (-> (mock/request :get "/register") app p :status) => 200 
      )

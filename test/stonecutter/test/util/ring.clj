(ns stonecutter.test.util.ring
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock-request]
            [stonecutter.util.ring :as util-ring]))

(tabular
(fact "complete-uri rebuilds the requested uri from a ring request map, not including the host name"
      (util-ring/complete-uri-of (mock-request/request :get ?uri)) => ?uri)
  ?uri
  "/no-query"
  "/query?a=1"
  "/multiple-query?a=1&b=2"
  "relative")

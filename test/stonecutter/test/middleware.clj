(ns stonecutter.test.middleware
  (:require [midje.sweet :refer :all]
            [stonecutter.middleware :as middleware]))


(defn example-handler [request] "return value")
(defn wrap-function [handler] (fn [request] "wrap function return value"))

(def handlers {:handler-1 example-handler
               :handler-2 example-handler
               :handler-3 example-handler})

(facts "about wrap-handlers"
      (fact "wrap handlers wraps all handlers in a wrap-function"
            (let [wrapped-handlers (middleware/wrap-handlers handlers wrap-function nil)]
              ((:handler-1 wrapped-handlers) "request") => "wrap function return value"
              ((:handler-2 wrapped-handlers) "request") => "wrap function return value"
              ((:handler-3 wrapped-handlers) "request") => "wrap function return value"))

      (fact "wrap handlers takes a set of exclusions which are not wrapped"
            (let [wrapped-handlers (middleware/wrap-handlers handlers wrap-function #{:handler-1 :handler-3})]
              ((:handler-1 wrapped-handlers) "request") => "return value"
              ((:handler-2 wrapped-handlers) "request") => "wrap function return value"
              ((:handler-3 wrapped-handlers) "request") => "return value")))

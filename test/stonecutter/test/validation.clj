(ns stonecutter.test.validation
  (:require [midje.sweet :refer :all]
            [stonecutter.validation :as v]))

(tabular
  (fact "Testing email validation"
        (v/is-email-valid? ?email) => ?is-valid?)

  ?email                        ?is-valid? 
  ""                            falsey
  "invalid"                     falsey
  "invalid@email"               falsey 
  "valid@email.com"             truthy 
  "vAlId@eMaIl.cOm"             truthy 
  "VALID@EMAIL.COM"             truthy 
  "very.valid@email.co.uk"      truthy 
  "very123.v0alid@email.co.uk"  truthy 
  "valid@email.averylongdsn"    truthy

  ) 


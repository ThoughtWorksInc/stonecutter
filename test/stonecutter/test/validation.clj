(ns stonecutter.test.validation
  (:require [midje.sweet :refer :all]
            [stonecutter.validation :as v]))

(tabular
  (fact "testing email validation"
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

(facts "about registration validation"
       (fact "invalid email returns error message"
             (v/validate-registration {:email "invalid"}) => "Email address is invalid"
             )
       (fact "invalid email returns nil"
             (v/validate-registration {:email "valid@email.com"}) => nil))


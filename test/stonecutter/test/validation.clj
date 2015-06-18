(ns stonecutter.test.validation
  (:require [midje.sweet :refer :all]
            [stonecutter.validation :as v]))

(tabular
(fact "Testing email validation"
      (v/is-email-valid? ?email) => ?is-valid?)
 
?email    ?is-valid? 
""        false
  ) 


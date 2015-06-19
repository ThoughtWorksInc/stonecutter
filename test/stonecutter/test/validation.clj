(ns stonecutter.test.validation
  (:require [midje.sweet :refer :all]
            [stonecutter.validation :as v]))

(tabular
  (fact "testing email validation"
        (v/is-email-valid? {:email ?email}) => ?is-valid?)

  ?email                        ?is-valid? 
  nil                           falsey
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

(tabular
  (fact "testing password validation"
        (v/is-password-valid? {:password ?password}) => ?is-valid?)

  ?password                     ?is-valid? 
  nil                           falsey
  ""                            falsey
  "valid-password"              truthy)

(facts "about registration validation"
       (fact "invalid email returns email error key"
             (v/validate-registration {:email "invalid" 
                                       :password "valid-password" 
                                       :confirm-password "valid-password"}) => {:email :invalid})
       (fact "there are no errors"
             (v/validate-registration {:email "valid@email.com" 
                                       :password "valid-password" 
                                       :confirm-password "valid-password"}) => {})
       (fact "invalid password returns error message"
             (v/validate-registration {:email "valid@email.com" 
                                       :password "" 
                                       :confirm-password ""}) => {:password :invalid})
       (fact "blank password and non-blank confirm password returns error message"
             (v/validate-registration {:email "valid@email.com" 
                                       :password "" 
                                       :confirm-password "password"}) => {:password :invalid 
                                                                          :confirm-password :invalid})
       (fact "invalid password confirmation returns an error"
             (v/validate-registration {:email "valid@email.com" 
                                       :password "password" 
                                       :confirm-password "invalid-password"}) => {:confirm-password :invalid}))

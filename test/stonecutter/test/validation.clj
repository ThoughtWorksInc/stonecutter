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
  "valid@email.averylongdsn"    truthy)

(tabular
  (fact "testing password validation"
        (v/is-password-valid? {:password ?password}) => ?is-valid?)

  ?password                     ?is-valid?
  nil                           falsey
  ""                            falsey
  "valid-password"              truthy)

(defn create-params [email password confirm-password]
  {:email email
   :password password
   :confirm-password confirm-password })

(def default-duplicate-user-fn (fn [email] false))

(facts "about registration validation"
       (fact "invalid email returns email error key"
             (v/validate-registration
               (create-params "invalid-email" "valid-password" "valid-password")
               default-duplicate-user-fn) => {:email :invalid})
       (fact "there are no errors for valid email and passwords"
             (v/validate-registration
               (create-params "valid@email.com" "valid-password" "valid-password")
               default-duplicate-user-fn) => {})
       (fact "invalid password returns error message"
             (v/validate-registration
               (create-params "valid@email.com" "" "")
               default-duplicate-user-fn) => {:password :invalid})
       (fact "blank password and non-blank confirm password returns error message"
             (v/validate-registration
               (create-params "valid@email.com" "" "password")
               default-duplicate-user-fn) => {:password         :invalid
                                              :confirm-password :invalid})
       (fact "return error when passwords don't match"
             (v/validate-registration
               (create-params "valid@email.com" "password" "invalid-password")
               default-duplicate-user-fn) => {:confirm-password :invalid})
       (fact "if a duplicate user is found then an error is returned"
             (let [duplicate-user-fn (fn [email] true)]
               (v/validate-registration
                 (create-params "valid@email.com" "password" "password")
                 duplicate-user-fn) => {:email :duplicate}))
       (fact "email address longer than 254 characters returns email error key"
             (let [long-email-address (apply str (repeat 255 "x"))]
               (v/validate-registration
                 (create-params long-email-address "valid-password" "valid-password")
                 default-duplicate-user-fn) => {:email :too-long})))

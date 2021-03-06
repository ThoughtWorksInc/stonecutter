(ns stonecutter.test.validation
  (:require [midje.sweet :refer :all]
            [stonecutter.validation :as v]))

(defn string-of-length [n]
  (apply str (repeat n "x")))

(def not-duplicate-user (fn [email] false))

(def is-duplicate-user (fn [email] true))

(def email-of-length-254
  (str (string-of-length 250) "@x.y"))

(def email-of-length-255
  (str (string-of-length 251) "@x.y"))

(tabular
  (fact "testing email format"
        (v/is-email-valid? ?email) => ?is-valid?)

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
  (fact "testing name validation"
        (v/validate-registration-name ?name) => ?error)
  ?name                   ?error
  "Barry"                 nil
  nil                     :blank
  ""                      :blank
  (string-of-length 70)   nil
  (string-of-length 71)   :too-long)

(tabular
  (fact "testing email validation"
        (v/validate-registration-email ?email not-duplicate-user)=> ?error)

  ?email                        ?error
  "valid@email.com"             nil
  "invalid-email-format"        :invalid
  email-of-length-254           nil
  email-of-length-255           :too-long)

(tabular
  (fact "testing password validation"
        (v/validate-password-format ?password) => ?error)

  ?password                     ?error
  "some-valid-password"         nil
  nil                           :blank
  ""                            :blank
  "                "            :blank
  "\t\t\t\t\t\t\t\t"            :blank
  (string-of-length 8)          nil
  (string-of-length 7)          :too-short
  (string-of-length 254)        nil
  (string-of-length 255)        :too-long)

(tabular
  (fact "validating registration"
        (v/validate-registration
          {:registration-first-name ?first-name
           :registration-last-name  ?last-name
           :registration-email      ?email
           :registration-password   ?password}
          ?duplicate-user-fn) => ?validations)

  ?first-name  ?last-name  ?email             ?password         ?duplicate-user-fn  ?validations
  "Frank"      "Lasty"     "valid@email.com"  "valid-password"  not-duplicate-user  {}
  ""           "Lasty"     "valid@email.com"  "valid-password"  not-duplicate-user  {:registration-first-name :blank}
  "Frank"      ""          "valid@email.com"  "valid-password"  not-duplicate-user  {:registration-last-name :blank}
  "Frank"      "Lasty"     "invalid-email"    "valid-password"  not-duplicate-user  {:registration-email :invalid}
  "Frank"      "Lasty"     "valid@email.com"  "password"        is-duplicate-user   {:registration-email :duplicate}
  "Frank"      "Lasty"     "valid@email.com"  ""                not-duplicate-user  {:registration-password :blank}
  ""           ""          "invalid-email"    ""                not-duplicate-user  {:registration-first-name :blank
                                                                                     :registration-last-name :blank
                                                                                     :registration-email :invalid
                                                                                     :registration-password :blank})

(tabular
  (fact "validating sign-in"
        (v/validate-sign-in
          {:sign-in-email ?email
           :sign-in-password ?password}) => ?validations)

  ?email                   ?password            ?validations
  "valid@email.com"        "valid-password"     {}
  "invalid-email"          "valid-password"     {:sign-in-email :invalid}
  "valid@email.com"        ""                   {:sign-in-password :blank}
  "invalid-email"          ""                   {:sign-in-email :invalid
                                                 :sign-in-password :blank})

(tabular
 (fact "validating change-password"
       (v/validate-change-password {:current-password ?current-password
                                    :new-password ?new-password} (constantly ?correct-password)) => ?validations)

  ?current-password         ?new-password          ?correct-password   ?validations
  "currentPassword"         "newPassword"          true                {}
  ""                        "newPassword"          true                {:current-password :blank}
  "currentPassword"         ""                     true                {:new-password :blank}
  "currentPassword"         "currentPassword"      true                {:new-password :unchanged}
  "currentPassword"         "newPassword"          true                {}
  ""                        ""                     true                {:current-password :blank
                                                                        :new-password :blank}
 "invalidPassword"          "newPassword"          false               {:current-password :invalid}
 ""                         "newPassword"          false               {:current-password :blank}
 )

(tabular
 (fact "validating forgotten-password"
       (v/validate-forgotten-password {:email ?email} (constantly ?user-exists)) => ?validations)
 ?email              ?user-exists       ?validations
 "valid@email.com"   true               {}
 "invalid"           true               {:email :invalid}
 "invalid"           false              {:email :invalid}
 "valid@email.com"   false              {:email :non-existent}
 )

(tabular
  (fact "validating reset-password"
        (v/validate-reset-password {:new-password ?new-password}) => ?validations)
  ?new-password            ?validations
  "newPassword"            {}
  ""                       {:new-password :blank}
  "blah"                   {:new-password :too-short}
  (string-of-length 255)   {:new-password :too-long})

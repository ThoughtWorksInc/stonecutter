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
  (fact "testing email validation"
        (v/validate-registration-email ?email not-duplicate-user)=> ?error)

  ?email                        ?error
  "valid@email.com"             nil
  "invalid-email-format"        :invalid
  email-of-length-254           nil
  email-of-length-255           :too-long)

(tabular
  (fact "testing password validation"
        (v/validate-password ?password) => ?error)

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
          {:registration-email ?email
           :registration-password ?password
           :registration-confirm-password ?confirm-password}
          ?duplicate-user-fn) => ?validations)

  ?email                   ?password           ?confirm-password        ?duplicate-user-fn        ?validations
  "valid@email.com"        "valid-password"    "valid-password"         not-duplicate-user        {}
  "invalid-email"          "valid-password"    "valid-password"         not-duplicate-user        {:registration-email :invalid}
  "valid@email.com"        "password"          "password"               is-duplicate-user         {:registration-email :duplicate}
  "valid@email.com"        ""                  ""                       not-duplicate-user        {:registration-password :blank}
  "valid@email.com"        "password"          "non-matching-password"  not-duplicate-user        {:registration-confirm-password :invalid}
  "invalid-email"          ""                  "password"               not-duplicate-user        {:registration-email :invalid
                                                                                                   :registration-password :blank
                                                                                                   :registration-confirm-password :invalid})

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
                                    :new-password ?new-password
                                    :confirm-new-password ?confirm-new-password} (constantly ?correct-password)) => ?validations)

  ?current-password         ?new-password       ?confirm-new-password       ?correct-password   ?validations
  "currentPassword"         "newPassword"       "newPassword"               true                {}
  ""                        "newPassword"       "newPassword"               true                {:current-password :blank}
  "currentPassword"         ""                  ""                          true                {:new-password :blank}
  "currentPassword"         "currentPassword"   "currentPassword"           true                {:new-password :unchanged}
  "currentPassword"         "newPassword"       "nonMatchingNewPassword"    true                {:confirm-new-password :invalid}
  ""                        ""                  "newPassword"               true                {:current-password :blank
                                                                                                 :new-password :blank
                                                                                                 :confirm-new-password :invalid}
 "invalidPassword"          "newPassword"       "newPassword"               false               {:current-password :invalid}
 ""                         "newPassword"       "newPassword"               false               {:current-password :blank}
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
        (v/validate-reset-password {:new-password ?new-password
                                    :confirm-new-password ?confirm-new-password}) => ?validations)
  ?new-password           ?confirm-new-password   ?validations
  "newPassword"           "newPassword"           {}
  ""                      "newPassword"           {:new-password :blank :confirm-new-password :invalid}
  "blah"                  "blah"                  {:new-password :too-short}
  (string-of-length 255)  (string-of-length 255)  {:new-password :too-long}
  "newPassword"           "nonMatching"           {:confirm-new-password :invalid}
  "newPassword"           ""                      {:confirm-new-password :invalid}
  "newPassword"           "newpassword"           {:confirm-new-password :invalid})

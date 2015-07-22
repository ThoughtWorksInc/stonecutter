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
          {:email ?email
           :password ?password
           :confirm-password ?confirm-password}
          ?duplicate-user-fn) => ?validations)

  ?email                   ?password           ?confirm-password        ?duplicate-user-fn        ?validations
  "valid@email.com"        "valid-password"    "valid-password"         not-duplicate-user        {}
  "invalid-email"          "valid-password"    "valid-password"         not-duplicate-user        {:email :invalid}
  "valid@email.com"        "password"          "password"               is-duplicate-user         {:email :duplicate}
  "valid@email.com"        ""                  ""                       not-duplicate-user        {:password :blank}
  "valid@email.com"        "password"          "non-matching-password"  not-duplicate-user        {:confirm-password :invalid}
  "invalid-email"          ""                  "password"               not-duplicate-user        {:email :invalid
                                                                                                   :password :blank
                                                                                                   :confirm-password :invalid})

(tabular
  (fact "validating sign-in"
        (v/validate-sign-in
          {:email ?email
           :password ?password}) => ?validations)

  ?email                   ?password            ?validations
  "valid@email.com"        "valid-password"     {}
  "invalid-email"          "valid-password"     {:email :invalid}
  "valid@email.com"        ""                   {:password :blank}
  "invalid-email"          ""                   {:email :invalid
                                                 :password :blank})

(tabular
 (fact "validating change-password"
       (v/validate-change-password {
                                    :current-password ?current-password
                                    :new-password ?new-password
                                    :confirm-new-password ?confirm-new-password}) => ?validations)

  ?current-password         ?new-password       ?confirm-new-password       ?validations
  "currentPassword"         "newPassword"       "newPassword"               {}
  ""                        "newPassword"       "newPassword"               {:current-password :blank}
  "currentPassword"         ""                  ""                          {:new-password :blank}
  "currentPassword"         "currentPassword"   "currentPassword"           {:new-password :unchanged}
  "currentPassword"         "newPassword"       "nonMatchingNewPassword"    {:confirm-new-password :invalid}
  ""                        ""                  "newPassword"               {:current-password :blank
                                                                             :new-password :blank
                                                                             :confirm-new-password :invalid})

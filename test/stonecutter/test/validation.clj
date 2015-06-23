(ns stonecutter.test.validation
  (:require [midje.sweet :refer :all]
            [stonecutter.validation :as v]))

(defn string-of-length [n]
  (apply str (repeat n "x")))

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
        (v/validate-password {:password ?password}) => ?error)

  ?password                     ?error
  nil                           :invalid
  ""                            :invalid
  "                "            :invalid
  "\t\t\t\t\t\t\t\t"            :invalid
  (string-of-length 7)          :invalid
  (string-of-length 51)         :invalid
  (string-of-length 8)          nil
  (string-of-length 50)         nil
  "some-valid-password"         nil)

(defn create-params [email password confirm-password]
  {:email email
   :password password
   :confirm-password confirm-password })

(def not-duplicate-user (fn [email] false))

(def is-duplicate-user (fn [email] true))

(tabular
  (fact "validating registration"
        (v/validate-registration
          (create-params ?email ?password ?confirm-password)
          ?duplicate-user-fn) => ?validations)

  ?email                   ?password           ?confirm-password        ?duplicate-user-fn        ?validations
  "valid@email.com"        "valid-password"    "valid-password"         not-duplicate-user        {}
  "invalid-email"          "valid-password"    "valid-password"         not-duplicate-user        {:email :invalid}
  (string-of-length 255)   "valid-password"    "valid-password"         not-duplicate-user        {:email :too-long}
  "valid@email.com"        ""                  ""                       not-duplicate-user        {:password :invalid}
  "valid@email.com"        ""                  "password"               not-duplicate-user        {:password :invalid
                                                                                                   :confirm-password :invalid}
  "valid@email.com"        "password"          "non-matching-password"  not-duplicate-user        {:confirm-password :invalid}
  "valid@email.com"        "password"          "password"               is-duplicate-user         {:email :duplicate})

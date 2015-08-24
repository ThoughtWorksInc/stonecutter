(ns stonecutter.test.config
  (:require [midje.sweet :refer :all]
            [stonecutter.config :refer [get-env secure?]]))


(fact "get-env throws an exception when the requested key isn't in the env-vars set"
      (get-env {:env-key "env-var"} :some-key-that-isnt-in-env-vars) => (throws Exception))

(tabular
  (fact "secure? is true by default"
        (secure? {:secure ?secure-env-value}) => ?return-value)
  ?secure-env-value     ?return-value
  "true"                true
  "asdf"                true
  ""                    true
  nil                   true
  "false"               false)

(ns stonecutter.test.config
  (:require [midje.sweet :refer :all]
            [stonecutter.config :as c]
            [clojure.java.io :as io]))


(fact "get-env throws an exception when the requested key isn't in the env-vars set"
      (c/get-env {:env-key "env-var"} :some-key-that-isnt-in-env-vars) => (throws Exception))

(tabular
  (fact "secure? is true by default"
        (c/secure? {:secure ?secure-env-value}) => ?return-value)
  ?secure-env-value     ?return-value
  "true"                true
  "asdf"                true
  ""                    true
  nil                   true
  "false"               false)

(fact "about to-env"
      (c/to-env :some-config) => "SOME_CONFIG")

(fact "can generate config line in file"
      (c/gen-config-line {:some-config 1} [:some-config "This is a piece of config"])
        => "# This is a piece of config\nSOME_CONFIG=1"
      (c/gen-config-line {:some-config 1} [:some-other-config "Some other description"])
       => "# Some other description\n# SOME_OTHER_CONFIG=")

(fact "can generate config file"
      (c/gen-config! {:var-a "blah" :var-b "dave"} [:var-a "Var a description" :var-b "Var b description"] "test-resources/config.env")
      (slurp "test-resources/config.env") => "# Var a description\nVAR_A=blah\n\n# Var b description\nVAR_B=dave"
      (io/delete-file "test-resources/config.env"))



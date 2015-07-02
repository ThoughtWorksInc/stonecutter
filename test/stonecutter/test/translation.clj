(ns stonecutter.test.translation
  (:require [midje.sweet :refer :all]
            [stonecutter.translation :refer [load-translations-from-string load-translations-from-file]]))

(facts "can load translations from a string"
       (fact "basic example"
             (load-translations-from-string "a-key: Hello") => {:a-key "Hello"})
       (fact "with nested keys"
                    (load-translations-from-string
                      "a:\n
                          hello: Hello\n
                          goodbye: Goodbye\n") => {:a {:hello "Hello" :goodbye "Goodbye"}}))

(facts "can load translations from a file"
       (load-translations-from-file "test-translations.yml") => {:a {:hello "Hello" :goodbye "Goodbye"}})
